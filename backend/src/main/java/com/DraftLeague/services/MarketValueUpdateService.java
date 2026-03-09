package com.DraftLeague.services;

import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerMarketValueHistory;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.PlayerMarketValueHistoryRepository;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import com.DraftLeague.repositories.PlayerTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for recalculating player market values based on:
 * <ul>
 *   <li>Recent performance (fantasy points over the last {@value #FORM_WINDOW} matches)</li>
 *   <li>Discipline (yellow / red cards in the last {@value #DISCIPLINE_WINDOW} matches)</li>
 *   <li>Demand (number of fantasy teams that currently own the player)</li>
 *   <li>Activity status (inactive players are penalised)</li>
 * </ul>
 *
 * <p>Price changes per update are capped at ±{@value #MAX_CHANGE_PCT}% to avoid
 * wild single-update swings.  Absolute limits are {@value #MIN_VALUE} (floor) and
 * {@value #MAX_VALUE} (ceiling), and every price is rounded to the nearest
 * {@value #ROUND_TO} to keep numbers tidy.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketValueUpdateService {

    // ── Tunable constants ──────────────────────────────────────────────────────

    /** Number of recent matches used to assess a player's current form. */
    private static final int FORM_WINDOW = 5;

    /** Number of recent matches inspected for discipline (cards). */
    private static final int DISCIPLINE_WINDOW = 5;

    /** Maximum price movement (as a fraction) allowed in a single update cycle. */
    private static final double MAX_CHANGE_FRACTION = 0.20; // ±20 %

    /** Absolute minimum market value (3 M). */
    private static final int MIN_VALUE = 3_000_000;

    /** Absolute maximum market value (50 M). */
    private static final int MAX_VALUE = 50_000_000;

    /** Prices are rounded to the nearest multiple of this value. */
    private static final int ROUND_TO = 50_000;

    // ── Expected fantasy-point benchmarks per position ─────────────────────────

    private static final double GK_EXPECTED_PTS  = 5.0;
    private static final double DEF_EXPECTED_PTS = 6.0;
    private static final double MID_EXPECTED_PTS = 7.0;
    private static final double FWD_EXPECTED_PTS = 8.0;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final PlayerRepository playerRepository;
    private final PlayerStatisticRepository playerStatisticRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final PlayerMarketValueHistoryRepository marketValueHistoryRepository;

    @Autowired @Lazy private MarketValueUpdateService self;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Recalculates and persists market values for every player in the database.
     *
     * @return A summary map containing {@code updatedCount}, {@code skippedCount},
     *         and {@code errorCount}.
     */
    public Map<String, Integer> recalculateAllMarketValues() {
        return recalculateAllMarketValuesForGameweek(null);
    }

    /**
     * Recalculates and persists market values for every player and records a
     * price-history snapshot for the given gameweek.
     *
     * @param gameweek the gameweek number to associate with the history record,
     *                 or {@code null} to skip history recording.
     * @return A summary map containing {@code updatedCount}, {@code skippedCount},
     *         and {@code errorCount}.
     */
    public Map<String, Integer> recalculateAllMarketValuesForGameweek(Integer gameweek) {
        log.info("[MarketValueUpdate] Starting full market value recalculation (gameweek={})…", gameweek);

        List<Player> players = playerRepository.findAll();
        int updated = 0, skipped = 0, errors = 0;

        for (Player player : players) {
            try {
                boolean changed = self.recalculateForPlayer(player, gameweek);
                if (changed) updated++; else skipped++;
            } catch (Exception e) {
                errors++;
                log.error("[MarketValueUpdate] Error processing player {} ({}): {}",
                        player.getId(), player.getFullName(), e.getMessage(), e);
            }
        }

        log.info("[MarketValueUpdate] Completed. updated={}, skipped={}, errors={}",
                updated, skipped, errors);
        return Map.of("updatedCount", updated, "skippedCount", skipped, "errorCount", errors);
    }

    /**
     * Recalculates and persists the market value for a single player.
     * Delegates to {@link #recalculateForPlayer(Player, Integer)} with no gameweek.
     *
     * @param player the player to update
     * @return {@code true} if the price was actually changed.
     */
    public boolean recalculateForPlayer(Player player) {
        return self.recalculateForPlayer(player, null);
    }

    /**
     * Recalculates and persists the market value for a single player.
     * Also updates the {@code sellPrice} in every {@code PlayerTeam} row that
     * references this player, proportionally tracking the price change.
     * If {@code gameweek} is non-null, a {@link PlayerMarketValueHistory} record
     * is upserted so the evolution can be charted per jornada.
     *
     * @param player   the player to update
     * @param gameweek the active gameweek number, or {@code null} to skip history
     * @return {@code true} if the price was actually changed, {@code false} when
     *         there is not enough data to justify a change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recalculateForPlayer(Player player, Integer gameweek) {
        int baseValue = (player.getMarketValue() != null && player.getMarketValue() > MIN_VALUE)
            ? player.getMarketValue()
            : defaultBaseValue(player.getPosition());

        // ── 1. Form factor ──────────────────────────────────────────────────────
        List<PlayerStatistic> recentStats = playerStatisticRepository
                .findRecentStatsByPlayerId(player.getId(), PageRequest.of(0, FORM_WINDOW));

        double expectedPoints = getExpectedPoints(player.getPosition());

        if (recentStats.isEmpty()) {
            if (player.getMarketValue() == null) {
                player.setMarketValue(baseValue);
                playerRepository.save(player);
                playerTeamRepository.updateSellPriceByPlayerId(player.getId(), baseValue);
                return true;
            }
            // Fallback: use season-wide averages so benched/injured players still drift
            Map<String, Object> season = playerStatisticRepository.getPlayerStatisticsSummaryData(player.getId());
            double sf = computeSeasonFactor(season, expectedPoints);
            // Cap season-only drift to ±10 % so the effect is gentler than the full model
            double cappedSf = clamp(sf, 0.90, 1.10);
            int rawSeasonValue = (int) Math.round(baseValue * cappedSf);
            int seasonValue = roundToNearest(clampInt(rawSeasonValue, MIN_VALUE, MAX_VALUE), ROUND_TO);
            if (seasonValue == baseValue) {
                log.debug("[MarketValueUpdate] No recent stats, no season change for player {} – skipping.", player.getId());
                return false;
            }
            player.setMarketValue(seasonValue);
            playerRepository.save(player);
            playerTeamRepository.updateSellPriceByPlayerId(player.getId(), seasonValue);
            if (gameweek != null) {
                int changeAmount = seasonValue - baseValue;
                double changePercentage = baseValue > 0 ? ((double) changeAmount / baseValue) * 100.0 : 0.0;
                PlayerMarketValueHistory history = marketValueHistoryRepository
                        .findByPlayerIdAndGameweek(player.getId(), gameweek)
                        .orElse(new PlayerMarketValueHistory());
                history.setPlayerId(player.getId());
                history.setGameweek(gameweek);
                history.setPreviousValue(baseValue);
                history.setNewValue(seasonValue);
                history.setChangeAmount(changeAmount);
                history.setChangePercentage(Math.round(changePercentage * 100.0) / 100.0);
                history.setRecordedAt(new Date());
                marketValueHistoryRepository.save(history);
            }
            log.debug("[MarketValueUpdate] Player {} ({}) [season fallback]: {} -> {}",
                    player.getId(), player.getFullName(), baseValue, seasonValue);
            return true;
        }

        double avgFantasyPoints = recentStats.stream()
            .mapToInt(ps -> safeFantasyPoints(ps))
            .average()
            .orElse(0.0);

        double formRatio = (expectedPoints > 0) ? (avgFantasyPoints / expectedPoints) : 1.0;
        double formFactor = clamp(formRatio, 0.85, 1.25);

        double minutesFactor = clamp(recentStats.stream()
            .mapToInt(ps -> safeInt(ps.getMinutesPlayed()))
            .average()
            .orElse(0.0) / 90.0, 0.70, 1.05);

        double avgRating = recentStats.stream()
            .mapToDouble(ps -> ps.getRating() != null ? ps.getRating() : 6.5)
            .average()
            .orElse(6.5);
        double ratingFactor = clamp(1.0 + ((avgRating - 6.5) * 0.04), 0.90, 1.10);

        double consistencyFactor = computeConsistencyFactor(recentStats);
        double momentumFactor = computeMomentumFactor(recentStats, expectedPoints);
        double impactFactor = computeImpactFactor(recentStats, player.getPosition());

        Map<String, Object> season = playerStatisticRepository.getPlayerStatisticsSummaryData(player.getId());
        double seasonFactor = computeSeasonFactor(season, expectedPoints);

        double blendedForm = (formFactor * 0.65) + (seasonFactor * 0.35);

        // ── 2. Discipline factor ────────────────────────────────────────────────
        double disciplineFactor = computeDisciplineFactor(player.getId());

        // ── 3. Demand factor ────────────────────────────────────────────────────
        long ownershipCount = playerTeamRepository.countByPlayerId(player.getId());
        // Each owning team adds +1.5 %, capped at +20 %
        double demandBonus = Math.min(ownershipCount * 0.015, 0.20);
        double demandFactor = 1.0 + demandBonus;

        // ── 4. Activity factor ──────────────────────────────────────────────────
        double activityFactor = Boolean.TRUE.equals(player.getActive()) ? 1.0 : 0.90;

        // ── 5. Combined factor & change cap ────────────────────────────────────
        double combinedFactor = blendedForm * minutesFactor * ratingFactor *
            consistencyFactor * momentumFactor * impactFactor *
            disciplineFactor * demandFactor * activityFactor;

        // Clamp the total movement to ±MAX_CHANGE_FRACTION per update run
        double cappedFactor = clamp(combinedFactor,
                1.0 - MAX_CHANGE_FRACTION,
                1.0 + MAX_CHANGE_FRACTION);

        int rawNewValue = (int) Math.round(baseValue * cappedFactor);

        // ── 6. Absolute price clamp & rounding ─────────────────────────────────
        int newValue = roundToNearest(clampInt(rawNewValue, MIN_VALUE, MAX_VALUE), ROUND_TO);

        if (newValue == baseValue) {
            return false; // nothing changed – no write needed
        }

        log.debug("[MarketValueUpdate] Player {} ({}): {} -> {} | form={} season={} minutes={} rating={} momentum={} consistency={} impact={} disc={} demand={} active={}",
                player.getId(), player.getFullName(),
                baseValue, newValue,
            String.format("%.3f", formFactor),
            String.format("%.3f", seasonFactor),
            String.format("%.3f", minutesFactor),
            String.format("%.3f", ratingFactor),
            String.format("%.3f", momentumFactor),
            String.format("%.3f", consistencyFactor),
            String.format("%.3f", impactFactor),
                String.format("%.3f", disciplineFactor),
                String.format("%.3f", demandFactor),
                activityFactor);

        // ── 7. Persist ─────────────────────────────────────────────────────────
        player.setMarketValue(newValue);
        playerRepository.save(player);

        // Update sell prices in all PlayerTeam rows that hold this player so that
        // the new market signal is immediately visible in the fantasy market.
        playerTeamRepository.updateSellPriceByPlayerId(player.getId(), newValue);

        // ── 8. Record history snapshot (if gameweek is known) ──────────────────
        if (gameweek != null) {
            int changeAmount = newValue - baseValue;
            double changePercentage = baseValue > 0
                    ? ((double) changeAmount / baseValue) * 100.0
                    : 0.0;

            PlayerMarketValueHistory history = marketValueHistoryRepository
                    .findByPlayerIdAndGameweek(player.getId(), gameweek)
                    .orElse(new PlayerMarketValueHistory());

            history.setPlayerId(player.getId());
            history.setGameweek(gameweek);
            history.setPreviousValue(baseValue);
            history.setNewValue(newValue);
            history.setChangeAmount(changeAmount);
            history.setChangePercentage(Math.round(changePercentage * 100.0) / 100.0);
            history.setRecordedAt(new Date());
            marketValueHistoryRepository.save(history);
        }

        return true;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Computes a discipline multiplier (≤ 1.0) based on cards received in the
     * last {@value #DISCIPLINE_WINDOW} matches.
     * <ul>
     *   <li>Each yellow card → −2 % (i.e. × 0.98)</li>
     *   <li>Each red card   → −8 % (i.e. × 0.92)</li>
     * </ul>
     * The floor is 0.70 so extreme card-heavy players don't reach zero.
     */
    private double computeDisciplineFactor(String playerId) {
        try {
            Map<String, Object> disc = playerStatisticRepository
                    .findRecentDisciplineByPlayerId(playerId, DISCIPLINE_WINDOW);
            if (disc == null) return 1.0;

            long yellows = toLong(disc.get("total_yellow_cards"));
            long reds    = toLong(disc.get("total_red_cards"));

            double factor = 1.0
                    - (yellows * 0.02)
                    - (reds    * 0.08);

            return Math.max(factor, 0.70);
        } catch (Exception e) {
            log.warn("[MarketValueUpdate] Could not compute discipline for {}: {}", playerId, e.getMessage());
            return 1.0;
        }
    }

    /** Maps a {@link Position} to the expected average fantasy points per match. */
    private double getExpectedPoints(Position position) {
        if (position == null) return MID_EXPECTED_PTS;
        return switch (position) {
            case POR -> GK_EXPECTED_PTS;
            case DEF -> DEF_EXPECTED_PTS;
            case MID -> MID_EXPECTED_PTS;
            case DEL -> FWD_EXPECTED_PTS;
            default  -> MID_EXPECTED_PTS;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int defaultBaseValue(Position position) {
        if (position == null) return MIN_VALUE;
        return switch (position) {
            case POR -> 5_000_000;
            case DEF -> 6_000_000;
            case MID -> 7_000_000;
            case DEL -> 8_000_000;
            default -> MIN_VALUE;
        };
    }

    private int safeFantasyPoints(PlayerStatistic ps) {
        if (ps.getTotalFantasyPoints() != null) return ps.getTotalFantasyPoints();
        return ps.calculateFantasyPoints();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private double computeConsistencyFactor(List<PlayerStatistic> recentStats) {
        if (recentStats.isEmpty()) return 1.0;
        double avg = recentStats.stream().mapToInt(this::safeFantasyPoints).average().orElse(0.0);
        double variance = recentStats.stream()
                .mapToDouble(ps -> Math.pow(safeFantasyPoints(ps) - avg, 2))
                .average()
                .orElse(0.0);
        double std = Math.sqrt(variance);
        return clamp(1.02 - (std * 0.015), 0.92, 1.05);
    }

    private double computeMomentumFactor(List<PlayerStatistic> recentStats, double expectedPoints) {
        if (recentStats.size() < 3 || expectedPoints <= 0) return 1.0;
        double last3 = recentStats.stream().limit(3)
                .mapToInt(this::safeFantasyPoints).average().orElse(0.0);
        double prev = recentStats.stream().skip(3)
                .mapToInt(this::safeFantasyPoints).average().orElse(last3);
        double delta = (last3 - prev) / expectedPoints;
        return clamp(1.0 + (delta * 0.08), 0.95, 1.05);
    }

    private double computeImpactFactor(List<PlayerStatistic> recentStats, Position position) {
        if (recentStats.isEmpty()) return 1.0;

        double avgGoals = recentStats.stream().mapToInt(ps -> safeInt(ps.getGoals())).average().orElse(0.0);
        double avgAssists = recentStats.stream().mapToInt(ps -> safeInt(ps.getAssists())).average().orElse(0.0);
        double avgShotsOnTarget = recentStats.stream().mapToInt(ps -> safeInt(ps.getShotsOnTarget())).average().orElse(0.0);
        double avgChances = recentStats.stream().mapToInt(ps -> safeInt(ps.getChancesCreated())).average().orElse(0.0);
        double avgTackles = recentStats.stream().mapToInt(ps -> safeInt(ps.getTackles())).average().orElse(0.0);
        double avgInterceptions = recentStats.stream().mapToInt(ps -> safeInt(ps.getInterceptions())).average().orElse(0.0);
        double avgBlocks = recentStats.stream().mapToInt(ps -> safeInt(ps.getBlocks())).average().orElse(0.0);
        double avgSaves = recentStats.stream().mapToInt(ps -> safeInt(ps.getSaves())).average().orElse(0.0);
        double avgConceded = recentStats.stream().mapToInt(ps -> safeInt(ps.getGoalsConceded())).average().orElse(0.0);
        double cleanSheetRate = recentStats.stream().filter(ps -> Boolean.TRUE.equals(ps.getCleanSheet())).count()
                / (double) recentStats.size();

        double impactScore = switch (position) {
            case POR -> (avgSaves / 5.0) * 0.04 + (cleanSheetRate * 0.05) - (avgConceded / 2.0) * 0.03;
            case DEF -> (avgTackles + avgInterceptions + avgBlocks) / 10.0 * 0.05 + (cleanSheetRate * 0.04)
                    + (avgGoals * 0.03) + (avgAssists * 0.02);
            case MID -> (avgChances / 3.0) * 0.04 + (avgAssists * 0.05) + (avgGoals * 0.04);
            case DEL -> (avgShotsOnTarget / 3.0) * 0.04 + (avgGoals * 0.07) + (avgAssists * 0.03);
            default -> 0.0;
        };

        return clamp(1.0 + impactScore, 0.88, 1.12);
    }

    private double computeSeasonFactor(Map<String, Object> season, double expectedPoints) {
        if (season == null || expectedPoints <= 0) return 1.0;
        long matches = toLong(season.get("matches_played"));
        if (matches <= 0) return 1.0;
        double totalFantasy = toLong(season.get("total_fantasy_points"));
        double avgFantasy = totalFantasy / matches;
        double seasonRatio = avgFantasy / expectedPoints;
        double rating = toDouble(season.get("avg_rating"), 6.5);
        double ratingBoost = clamp(1.0 + ((rating - 6.5) * 0.03), 0.92, 1.08);
        return clamp(seasonRatio, 0.90, 1.15) * ratingBoost;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int roundToNearest(int value, int multiple) {
        return Math.round((float) value / multiple) * multiple;
    }

    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException ignored) { return 0L; }
    }

    private double toDouble(Object obj, double fallback) {
        if (obj == null) return fallback;
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (NumberFormatException ignored) { return fallback; }
    }
}
