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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Recalculates and persists market values for every player in the database.
     *
     * @return A summary map containing {@code updatedCount}, {@code skippedCount},
     *         and {@code errorCount}.
     */
    @Transactional
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
    @Transactional
    public Map<String, Integer> recalculateAllMarketValuesForGameweek(Integer gameweek) {
        log.info("[MarketValueUpdate] Starting full market value recalculation (gameweek={})…", gameweek);

        List<Player> players = playerRepository.findAll();
        int updated = 0, skipped = 0, errors = 0;

        for (Player player : players) {
            try {
                boolean changed = recalculateForPlayer(player, gameweek);
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
    @Transactional
    public boolean recalculateForPlayer(Player player) {
        return recalculateForPlayer(player, null);
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
    @Transactional
    public boolean recalculateForPlayer(Player player, Integer gameweek) {
        int baseValue = player.getMarketValue();

        // ── 1. Form factor ──────────────────────────────────────────────────────
        List<PlayerStatistic> recentStats = playerStatisticRepository
                .findRecentStatsByPlayerId(player.getId(), PageRequest.of(0, FORM_WINDOW));

        if (recentStats.isEmpty()) {
            // Edge case: no match data available – leave price unchanged
            log.debug("[MarketValueUpdate] No recent stats for player {} – skipping.", player.getId());
            return false;
        }

        double avgFantasyPoints = recentStats.stream()
                .mapToInt(ps -> ps.getTotalFantasyPoints() != null ? ps.getTotalFantasyPoints() : 0)
                .average()
                .orElse(0.0);

        double expectedPoints = getExpectedPoints(player.getPosition());
        // Form multiplier: centred at 1.0 (meeting expectation), clamped so a
        // single hot streak cannot more than double the price.
        double formRatio = (expectedPoints > 0) ? (avgFantasyPoints / expectedPoints) : 1.0;
        double formFactor = clamp(formRatio, 0.80, 1.20);

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
        double combinedFactor = formFactor * disciplineFactor * demandFactor * activityFactor;

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

        log.debug("[MarketValueUpdate] Player {} ({}): {} -> {} | form={} disc={} demand={} active={}",
                player.getId(), player.getFullName(),
                baseValue, newValue,
                String.format("%.3f", formFactor),
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
}
