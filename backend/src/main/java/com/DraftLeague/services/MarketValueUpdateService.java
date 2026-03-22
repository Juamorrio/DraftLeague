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

import java.util.*;

/**
 * Service responsible for recalculating player market values based on:
 * <ul>
 *   <li>Recent performance (fantasy points over the last {@value #FORM_WINDOW} matches)</li>
 *   <li>Discipline (yellow / red cards in the last {@value #DISCIPLINE_WINDOW} matches)</li>
 *   <li>Demand (number of fantasy teams that currently own the player)</li>
 *   <li>Activity status (inactive players are penalised)</li>
 * </ul>
 *
 * <p>Price changes per update are capped at ±{@value #MAX_CHANGE_FRACTION}% to avoid
 * wild single-update swings.  Absolute limits are {@value #MIN_VALUE} (floor) and
 * {@value #MAX_VALUE} (ceiling), and every price is rounded to the nearest
 * {@value #ROUND_TO} to keep numbers tidy.</p>
 *
 * <h3>Performance</h3>
 * <p>The full-recalculation path ({@link #recalculateAllMarketValuesForGameweek}) pre-loads
 * all required data in 5 bulk queries before entering the player loop, reducing DB round-trips
 * from O(N) to O(1) relative to player count.</p>
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

    // ── Lightweight projection used by the bulk recalculation path ────────────

    /**
     * Holds only the fields needed by the computation helpers when data is loaded
     * via the bulk native queries (no full {@link PlayerStatistic} entity required).
     */
    private record PlayerStatLite(
            int    totalFantasyPoints,
            int    minutesPlayed,
            Double rating,
            int    goals,
            int    assists,
            int    shotsOnTarget,
            int    chancesCreated,
            int    tackles,
            int    interceptions,
            int    blocks,
            int    saves,
            int    goalsConceded,
            boolean cleanSheet
    ) {
        static PlayerStatLite from(Map<String, Object> row) {
            return new PlayerStatLite(
                    toInt(row.get("total_fantasy_points")),
                    toInt(row.get("minutes_played")),
                    toDoubleNullable(row.get("rating")),
                    toInt(row.get("goals")),
                    toInt(row.get("assists")),
                    toInt(row.get("shots_on_target")),
                    toInt(row.get("chances_created")),
                    toInt(row.get("tackles")),
                    toInt(row.get("interceptions")),
                    toInt(row.get("blocks")),
                    toInt(row.get("saves")),
                    toInt(row.get("goals_conceded")),
                    toLong(row.get("clean_sheet")) == 1L
            );
        }

        private static int toInt(Object o) {
            if (o == null) return 0;
            if (o instanceof Number n) return n.intValue();
            try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return 0; }
        }

        private static long toLong(Object o) {
            if (o == null) return 0L;
            if (o instanceof Number n) return n.longValue();
            try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return 0L; }
        }

        private static Double toDoubleNullable(Object o) {
            if (o == null) return null;
            if (o instanceof Number n) return n.doubleValue();
            try { return Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return null; }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public Map<String, Integer> recalculateAllMarketValues() {
        return recalculateAllMarketValuesForGameweek(null);
    }

 
    public Map<String, Integer> recalculateAllMarketValuesForGameweek(Integer gameweek) {
        log.info("[MarketValueUpdate] Starting full recalculation (gameweek={})…", gameweek);

        log.debug("[MarketValueUpdate] Pre-loading bulk data…");

        Map<String, Map<String, Object>> seasonMap =
                buildSeasonDataMap(playerStatisticRepository.getAllPlayerStatisticsSummaryData());

        Map<String, List<PlayerStatLite>> recentStatsMap =
                buildRecentStatsMap(playerStatisticRepository.findRecentStatsForAllPlayers(FORM_WINDOW));

        Map<String, Map<String, Object>> disciplineMap =
                buildDisciplineMap(playerStatisticRepository.findRecentDisciplineForAllPlayers(DISCIPLINE_WINDOW));

        Map<String, Long> ownershipMap =
                buildOwnershipMap(playerTeamRepository.countOwnershipForAllPlayers());

        Map<String, PlayerMarketValueHistory> existingHistoryMap = (gameweek != null)
                ? buildHistoryMap(marketValueHistoryRepository.findByGameweek(gameweek))
                : Collections.emptyMap();

        log.info("[MarketValueUpdate] Bulk data loaded — seasonRecords={}, playersWithRecentStats={}, disciplineRecords={}, ownedPlayers={}, existingHistoryForGW={}",
                seasonMap.size(), recentStatsMap.size(), disciplineMap.size(), ownershipMap.size(), existingHistoryMap.size());
        if (recentStatsMap.isEmpty()) {
            log.warn("[MarketValueUpdate] No hay estadísticas recientes en BD (gameweek={}). " +
                     "Los precios solo cambiarán si el factor de media de temporada se desvía de 1.0. " +
                     "Realiza primero una sincronización de stats exitosa para activar la recalculación completa.", gameweek);
        }

        // ── 2. Per-player computation (writes only, no reads) ─────────────────
        List<Player> players = playerRepository.findAll();
        int updated = 0, skipped = 0, errors = 0;

        for (Player player : players) {
            try {
                boolean changed = self.recalculateForPlayerBulk(
                        player,
                        gameweek,
                        seasonMap.get(player.getId()),
                        recentStatsMap.getOrDefault(player.getId(), Collections.emptyList()),
                        disciplineMap.get(player.getId()),
                        ownershipMap.getOrDefault(player.getId(), 0L),
                        Optional.ofNullable(existingHistoryMap.get(player.getId()))
                );
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
     * <p>This path issues individual DB queries per player and is intended for
     * on-demand single-player updates. For bulk recalculation use
     * {@link #recalculateAllMarketValuesForGameweek(Integer)} instead.</p>
     *
     * @param player   the player to update
     * @param gameweek the active gameweek number, or {@code null} to skip history
     * @return {@code true} if the price was actually changed, {@code false} when
     *         there is not enough data to justify a change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recalculateForPlayer(Player player, Integer gameweek) {
        // Fetch season stats once – reused for computeBaseValue anchor and seasonFactor below
        Map<String, Object> season = playerStatisticRepository.getPlayerStatisticsSummaryData(player.getId());

        int baseValue = (player.getMarketValue() != null && player.getMarketValue() > MIN_VALUE)
            ? player.getMarketValue()
            : computeBaseValue(player, season);

        // ── 1. Form factor ──────────────────────────────────────────────────────
        List<PlayerStatistic> recentStats = playerStatisticRepository
                .findRecentStatsByPlayerId(player.getId(), PageRequest.of(0, FORM_WINDOW));

        double expectedPoints = getExpectedPoints(player.getPosition());

        if (recentStats.isEmpty()) {
            return applySeasonFallback(player, baseValue, season, expectedPoints, gameweek);
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
        double momentumFactor    = computeMomentumFactor(recentStats, expectedPoints);
        double impactFactor      = computeImpactFactor(recentStats, player.getPosition());
        double seasonFactor      = computeSeasonFactor(season, expectedPoints);
        double blendedForm       = (formFactor * 0.65) + (seasonFactor * 0.35);

        double disciplineFactor = computeDisciplineFactor(player.getId());

        long ownershipCount = playerTeamRepository.countByPlayerId(player.getId());
        double demandBonus  = Math.min(Math.log1p(ownershipCount) / Math.log1p(10.0) * 0.25, 0.25);
        double demandFactor = 1.0 + demandBonus;

        double activityFactor = Boolean.TRUE.equals(player.getActive()) ? 1.0 : 0.90;

        double combinedFactor = blendedForm * minutesFactor * ratingFactor *
            consistencyFactor * momentumFactor * impactFactor *
            disciplineFactor * demandFactor * activityFactor;

        double cappedFactor = clamp(combinedFactor,
                1.0 - MAX_CHANGE_FRACTION, 1.0 + MAX_CHANGE_FRACTION);

        int rawNewValue = (int) Math.round(baseValue * cappedFactor);
        int newValue    = roundToNearest(clampInt(rawNewValue, MIN_VALUE, MAX_VALUE), ROUND_TO);

        if (newValue == baseValue) {
            if (gameweek != null) {
                persistHistoryOnly(player.getId(), baseValue, gameweek,
                        marketValueHistoryRepository.findByPlayerIdAndGameweek(player.getId(), gameweek));
            }
            return false;
        }

        logFactors(player, baseValue, newValue, formFactor, seasonFactor, minutesFactor,
                ratingFactor, momentumFactor, consistencyFactor, impactFactor,
                disciplineFactor, demandFactor, activityFactor);

        persistPlayerValue(player, newValue, baseValue, gameweek,
                marketValueHistoryRepository.findByPlayerIdAndGameweek(player.getId(), gameweek));
        return true;
    }

    /**
     * Bulk variant of {@link #recalculateForPlayer(Player, Integer)}.
     * Accepts pre-loaded data so no DB reads are performed inside this method —
     * only writes (player save, sell-price update, history upsert).
     * Each invocation runs in its own transaction so a single player failure does
     * not roll back the rest of the batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recalculateForPlayerBulk(
            Player player,
            Integer gameweek,
            Map<String, Object> season,
            List<PlayerStatLite> recentStats,
            Map<String, Object> discipline,
            long ownershipCount,
            Optional<PlayerMarketValueHistory> existingHistory) {

        int baseValue = (player.getMarketValue() != null && player.getMarketValue() > MIN_VALUE)
                ? player.getMarketValue()
                : computeBaseValue(player, season);

        double expectedPoints = getExpectedPoints(player.getPosition());

        // ── Fallback: no recent stats ──────────────────────────────────────────
        if (recentStats.isEmpty()) {
            return applySeasonFallbackBulk(player, baseValue, season, expectedPoints, gameweek, existingHistory);
        }

        // ── Form factors (in-memory, no DB) ───────────────────────────────────
        double avgFantasyPoints = recentStats.stream()
                .mapToInt(PlayerStatLite::totalFantasyPoints)
                .average().orElse(0.0);

        double formFactor = clamp(
                expectedPoints > 0 ? avgFantasyPoints / expectedPoints : 1.0,
                0.85, 1.25);

        double minutesFactor = clamp(
                recentStats.stream().mapToInt(PlayerStatLite::minutesPlayed).average().orElse(0.0) / 90.0,
                0.70, 1.05);

        double avgRating = recentStats.stream()
                .mapToDouble(ps -> ps.rating() != null ? ps.rating() : 6.5)
                .average().orElse(6.5);
        double ratingFactor = clamp(1.0 + ((avgRating - 6.5) * 0.04), 0.90, 1.10);

        double consistencyFactor = computeConsistencyFactorLite(recentStats);
        double momentumFactor    = computeMomentumFactorLite(recentStats, expectedPoints);
        double impactFactor      = computeImpactFactorLite(recentStats, player.getPosition());
        double seasonFactor      = computeSeasonFactor(season, expectedPoints);
        double blendedForm       = (formFactor * 0.65) + (seasonFactor * 0.35);

        double disciplineFactor = computeDisciplineFactorFromMap(discipline);

        double demandBonus  = Math.min(Math.log1p(ownershipCount) / Math.log1p(10.0) * 0.25, 0.25);
        double demandFactor = 1.0 + demandBonus;

        double activityFactor = Boolean.TRUE.equals(player.getActive()) ? 1.0 : 0.90;

        double combinedFactor = blendedForm * minutesFactor * ratingFactor *
                consistencyFactor * momentumFactor * impactFactor *
                disciplineFactor * demandFactor * activityFactor;

        double cappedFactor = clamp(combinedFactor,
                1.0 - MAX_CHANGE_FRACTION, 1.0 + MAX_CHANGE_FRACTION);

        int rawNewValue = (int) Math.round(baseValue * cappedFactor);
        int newValue    = roundToNearest(clampInt(rawNewValue, MIN_VALUE, MAX_VALUE), ROUND_TO);

        if (newValue == baseValue) {
            if (gameweek != null) {
                persistHistoryOnly(player.getId(), baseValue, gameweek, existingHistory);
            }
            return false;
        }

        logFactors(player, baseValue, newValue, formFactor, seasonFactor, minutesFactor,
                ratingFactor, momentumFactor, consistencyFactor, impactFactor,
                disciplineFactor, demandFactor, activityFactor);

        persistPlayerValue(player, newValue, baseValue, gameweek, existingHistory);
        return true;
    }

    // ── Pre-load helpers (build Maps from bulk query results) ─────────────────

    private Map<String, Map<String, Object>> buildSeasonDataMap(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> result = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            String pid = (String) row.get("player_id");
            if (pid != null) result.put(pid, row);
        }
        return result;
    }

    private Map<String, List<PlayerStatLite>> buildRecentStatsMap(List<Map<String, Object>> rows) {
        Map<String, List<PlayerStatLite>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String pid = (String) row.get("player_id");
            if (pid != null) {
                result.computeIfAbsent(pid, k -> new ArrayList<>()).add(PlayerStatLite.from(row));
            }
        }
        return result;
    }

    private Map<String, Map<String, Object>> buildDisciplineMap(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> result = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            String pid = (String) row.get("player_id");
            if (pid != null) result.put(pid, row);
        }
        return result;
    }

    private Map<String, Long> buildOwnershipMap(List<Object[]> rows) {
        Map<String, Long> result = new HashMap<>(rows.size() * 2);
        for (Object[] row : rows) {
            if (row[0] != null) result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private Map<String, PlayerMarketValueHistory> buildHistoryMap(List<PlayerMarketValueHistory> rows) {
        Map<String, PlayerMarketValueHistory> result = new HashMap<>(rows.size() * 2);
        for (PlayerMarketValueHistory h : rows) {
            if (h.getPlayerId() != null) result.put(h.getPlayerId(), h);
        }
        return result;
    }

    // ── Shared persist helper ─────────────────────────────────────────────────

    /**
     * Persists the new player price, updates all sell prices in PlayerTeam rows,
     * and upserts the history record for the given gameweek (if non-null).
     * The {@code existingHistory} optional allows callers to pass a pre-loaded record
     * so no additional DB read is needed.
     */
    private void persistPlayerValue(Player player, int newValue, int baseValue,
                                    Integer gameweek, Optional<PlayerMarketValueHistory> existingHistory) {
        player.setMarketValue(newValue);
        playerRepository.save(player);
        playerTeamRepository.updateSellPriceByPlayerId(player.getId(), newValue);

        if (gameweek != null) {
            PlayerMarketValueHistory history = existingHistory.orElse(new PlayerMarketValueHistory());
            boolean isNew = history.getId() == null;
            int effectivePrev = isNew ? baseValue : history.getPreviousValue();
            int changeAmount  = newValue - effectivePrev;
            double changePct  = effectivePrev > 0 ? ((double) changeAmount / effectivePrev) * 100.0 : 0.0;

            history.setPlayerId(player.getId());
            history.setGameweek(gameweek);
            if (isNew) history.setPreviousValue(baseValue);
            history.setNewValue(newValue);
            history.setChangeAmount(changeAmount);
            history.setChangePercentage(Math.round(changePct * 100.0) / 100.0);
            history.setRecordedAt(new Date());
            marketValueHistoryRepository.save(history);
        }
    }

    /**
     * Persists a history record with zero change for the given gameweek.
     * Called when the computed market value equals the current value so that the
     * evolution chart always has a data point for every gameweek, even when the
     * price is stable.  No player or sell-price updates are performed.
     */
    private void persistHistoryOnly(String playerId, int currentValue, int gameweek,
                                    Optional<PlayerMarketValueHistory> existingHistory) {
        PlayerMarketValueHistory history = existingHistory.orElse(new PlayerMarketValueHistory());
        history.setPlayerId(playerId);
        history.setGameweek(gameweek);
        if (history.getId() == null) history.setPreviousValue(currentValue);
        history.setNewValue(currentValue);
        history.setChangeAmount(0);
        history.setChangePercentage(0.0);
        history.setRecordedAt(new Date());
        marketValueHistoryRepository.save(history);
    }

    // ── Season-fallback helpers ───────────────────────────────────────────────

    /** Single-player path: season-only drift when no recent matches exist. */
    private boolean applySeasonFallback(Player player, int baseValue,
                                        Map<String, Object> season, double expectedPoints,
                                        Integer gameweek) {
        if (player.getMarketValue() == null) {
            player.setMarketValue(baseValue);
            playerRepository.save(player);
            playerTeamRepository.updateSellPriceByPlayerId(player.getId(), baseValue);
            return true;
        }
        double cappedSf = clamp(computeSeasonFactor(season, expectedPoints), 0.90, 1.10);
        int seasonValue = roundToNearest(clampInt((int) Math.round(baseValue * cappedSf), MIN_VALUE, MAX_VALUE), ROUND_TO);
        if (seasonValue == baseValue) {
            log.debug("[MarketValueUpdate] No recent stats, no change for {} – skip.", player.getId());
            if (gameweek != null) {
                persistHistoryOnly(player.getId(), baseValue, gameweek,
                        marketValueHistoryRepository.findByPlayerIdAndGameweek(player.getId(), gameweek));
            }
            return false;
        }
        persistPlayerValue(player, seasonValue, baseValue, gameweek,
                gameweek != null
                        ? marketValueHistoryRepository.findByPlayerIdAndGameweek(player.getId(), gameweek)
                        : Optional.empty());
        log.debug("[MarketValueUpdate] {} (season fallback): {} -> {}", player.getId(), baseValue, seasonValue);
        return true;
    }

    /** Bulk path: season-only drift when no recent matches exist (uses pre-loaded history). */
    private boolean applySeasonFallbackBulk(Player player, int baseValue,
                                             Map<String, Object> season, double expectedPoints,
                                             Integer gameweek, Optional<PlayerMarketValueHistory> existingHistory) {
        if (player.getMarketValue() == null) {
            player.setMarketValue(baseValue);
            playerRepository.save(player);
            playerTeamRepository.updateSellPriceByPlayerId(player.getId(), baseValue);
            return true;
        }
        double cappedSf = clamp(computeSeasonFactor(season, expectedPoints), 0.90, 1.10);
        int seasonValue = roundToNearest(clampInt((int) Math.round(baseValue * cappedSf), MIN_VALUE, MAX_VALUE), ROUND_TO);
        if (seasonValue == baseValue) {
            log.debug("[MarketValueUpdate] No recent stats, no change for {} – skip.", player.getId());
            if (gameweek != null) {
                persistHistoryOnly(player.getId(), baseValue, gameweek, existingHistory);
            }
            return false;
        }
        persistPlayerValue(player, seasonValue, baseValue, gameweek, existingHistory);
        log.debug("[MarketValueUpdate] {} (season fallback): {} -> {}", player.getId(), baseValue, seasonValue);
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
            return computeDisciplineFactorFromMap(disc);
        } catch (Exception e) {
            log.warn("[MarketValueUpdate] Could not compute discipline for {}: {}", playerId, e.getMessage());
            return 1.0;
        }
    }

    /** Computes discipline factor from a pre-loaded map (bulk path). */
    private double computeDisciplineFactorFromMap(Map<String, Object> disc) {
        if (disc == null) return 1.0;
        long yellows = toLong(disc.get("total_yellow_cards"));
        long reds    = toLong(disc.get("total_red_cards"));
        return Math.max(1.0 - (yellows * 0.02) - (reds * 0.08), 0.70);
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

    /**
     * Computes the starting anchor price for a player whose stored market value
     * is at or below the floor.
     */
    private int computeBaseValue(Player player, Map<String, Object> season) {
        int positionDefault = defaultBaseValue(player.getPosition());
        double clubMultiplier = clubPrestigeMultiplier(player.getClubId());

        long matches = season != null ? toLong(season.get("matches_played")) : 0;
        if (matches >= 5) {
            double expectedPts  = getExpectedPoints(player.getPosition());
            double totalFantasy = season != null ? toLong(season.get("total_fantasy_points")) : 0;
            double avgPts       = totalFantasy / matches;
            double perfRatio    = expectedPts > 0 ? avgPts / expectedPts : 1.0;
            double perfMultiplier = clamp(Math.sqrt(perfRatio), 0.65, 1.50);
            int raw = (int) Math.round(positionDefault * perfMultiplier * clubMultiplier);
            return roundToNearest(clampInt(raw, MIN_VALUE, 15_000_000), ROUND_TO);
        }

        int raw = (int) Math.round(positionDefault * clubMultiplier);
        return roundToNearest(clampInt(raw, MIN_VALUE, 15_000_000), ROUND_TO);
    }

    /**
     * Returns a prestige multiplier for a La Liga club based on its typical
     * squad quality and budget tier.
     */
    private double clubPrestigeMultiplier(Integer clubId) {
        if (clubId == null) return 1.0;
        return switch (clubId) {
            case 541, 529, 530 -> 1.20;           // Real Madrid, Barcelona, Atlético Madrid
            case 536, 533, 531, 548, 543 -> 1.10; // Sevilla, Villarreal, Athletic, R.Sociedad, Betis
            case 547, 538, 532, 728, 546, 727 -> 1.00; // Girona, Celta, Valencia, Rayo, Getafe, Osasuna
            case 798, 540, 542, 718, 539, 797 -> 0.90; // Mallorca, Espanyol, Alavés, Oviedo, Levante, Elche
            default -> 1.00;
        };
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

    // ── Computation helpers — PlayerStatistic (single-player path) ────────────

    private int safeFantasyPoints(PlayerStatistic ps) {
        if (ps.getTotalFantasyPoints() != null) return ps.getTotalFantasyPoints();
        return ps.calculateFantasyPoints();
    }

    private int safeInt(Integer value) { return value != null ? value : 0; }

    private double computeConsistencyFactor(List<PlayerStatistic> recentStats) {
        if (recentStats.isEmpty()) return 1.0;
        double avg = recentStats.stream().mapToInt(this::safeFantasyPoints).average().orElse(0.0);
        double variance = recentStats.stream()
                .mapToDouble(ps -> Math.pow(safeFantasyPoints(ps) - avg, 2))
                .average().orElse(0.0);
        return clamp(1.02 - (Math.sqrt(variance) * 0.015), 0.92, 1.05);
    }

    private double computeMomentumFactor(List<PlayerStatistic> recentStats, double expectedPoints) {
        if (recentStats.size() < 3 || expectedPoints <= 0) return 1.0;
        double last3 = recentStats.stream().limit(3).mapToInt(this::safeFantasyPoints).average().orElse(0.0);
        double prev  = recentStats.stream().skip(3).mapToInt(this::safeFantasyPoints).average().orElse(last3);
        return clamp(1.0 + ((last3 - prev) / expectedPoints * 0.08), 0.95, 1.05);
    }

    private double computeImpactFactor(List<PlayerStatistic> recentStats, Position position) {
        if (recentStats.isEmpty()) return 1.0;

        double avgGoals    = recentStats.stream().mapToInt(ps -> safeInt(ps.getGoals())).average().orElse(0.0);
        double avgAssists  = recentStats.stream().mapToInt(ps -> safeInt(ps.getAssists())).average().orElse(0.0);
        double avgShots    = recentStats.stream().mapToInt(ps -> safeInt(ps.getShotsOnTarget())).average().orElse(0.0);
        double avgChances  = recentStats.stream().mapToInt(ps -> safeInt(ps.getChancesCreated())).average().orElse(0.0);
        double avgTackles  = recentStats.stream().mapToInt(ps -> safeInt(ps.getTackles())).average().orElse(0.0);
        double avgInter    = recentStats.stream().mapToInt(ps -> safeInt(ps.getInterceptions())).average().orElse(0.0);
        double avgBlocks   = recentStats.stream().mapToInt(ps -> safeInt(ps.getBlocks())).average().orElse(0.0);
        double avgSaves    = recentStats.stream().mapToInt(ps -> safeInt(ps.getSaves())).average().orElse(0.0);
        double avgConceded = recentStats.stream().mapToInt(ps -> safeInt(ps.getGoalsConceded())).average().orElse(0.0);
        double cleanRate   = recentStats.stream().filter(ps -> Boolean.TRUE.equals(ps.getCleanSheet())).count()
                / (double) recentStats.size();

        return clamp(1.0 + impactScore(position, avgGoals, avgAssists, avgShots, avgChances,
                avgTackles, avgInter, avgBlocks, avgSaves, avgConceded, cleanRate), 0.88, 1.12);
    }

    // ── Computation helpers — PlayerStatLite (bulk path) ──────────────────────

    private double computeConsistencyFactorLite(List<PlayerStatLite> stats) {
        if (stats.isEmpty()) return 1.0;
        double avg = stats.stream().mapToInt(PlayerStatLite::totalFantasyPoints).average().orElse(0.0);
        double variance = stats.stream()
                .mapToDouble(ps -> Math.pow(ps.totalFantasyPoints() - avg, 2))
                .average().orElse(0.0);
        return clamp(1.02 - (Math.sqrt(variance) * 0.015), 0.92, 1.05);
    }

    private double computeMomentumFactorLite(List<PlayerStatLite> stats, double expectedPoints) {
        if (stats.size() < 3 || expectedPoints <= 0) return 1.0;
        double last3 = stats.stream().limit(3).mapToInt(PlayerStatLite::totalFantasyPoints).average().orElse(0.0);
        double prev  = stats.stream().skip(3).mapToInt(PlayerStatLite::totalFantasyPoints).average().orElse(last3);
        return clamp(1.0 + ((last3 - prev) / expectedPoints * 0.08), 0.95, 1.05);
    }

    private double computeImpactFactorLite(List<PlayerStatLite> stats, Position position) {
        if (stats.isEmpty()) return 1.0;

        double avgGoals    = stats.stream().mapToInt(PlayerStatLite::goals).average().orElse(0.0);
        double avgAssists  = stats.stream().mapToInt(PlayerStatLite::assists).average().orElse(0.0);
        double avgShots    = stats.stream().mapToInt(PlayerStatLite::shotsOnTarget).average().orElse(0.0);
        double avgChances  = stats.stream().mapToInt(PlayerStatLite::chancesCreated).average().orElse(0.0);
        double avgTackles  = stats.stream().mapToInt(PlayerStatLite::tackles).average().orElse(0.0);
        double avgInter    = stats.stream().mapToInt(PlayerStatLite::interceptions).average().orElse(0.0);
        double avgBlocks   = stats.stream().mapToInt(PlayerStatLite::blocks).average().orElse(0.0);
        double avgSaves    = stats.stream().mapToInt(PlayerStatLite::saves).average().orElse(0.0);
        double avgConceded = stats.stream().mapToInt(PlayerStatLite::goalsConceded).average().orElse(0.0);
        double cleanRate   = stats.stream().filter(PlayerStatLite::cleanSheet).count()
                / (double) stats.size();

        return clamp(1.0 + impactScore(position, avgGoals, avgAssists, avgShots, avgChances,
                avgTackles, avgInter, avgBlocks, avgSaves, avgConceded, cleanRate), 0.88, 1.12);
    }

    /** Shared position-specific impact score formula (used by both single and bulk paths). */
    private double impactScore(Position position,
                               double avgGoals, double avgAssists, double avgShots, double avgChances,
                               double avgTackles, double avgInter, double avgBlocks,
                               double avgSaves, double avgConceded, double cleanRate) {
        if (position == null) return 0.0;
        return switch (position) {
            case POR -> (avgSaves / 4.0) * 0.07 + (cleanRate * 0.07) - (avgConceded / 2.0) * 0.03;
            case DEF -> (avgTackles + avgInter + avgBlocks) / 10.0 * 0.05 + (cleanRate * 0.04)
                    + (avgGoals * 0.03) + (avgAssists * 0.02);
            case MID -> (avgChances / 3.0) * 0.04 + (avgAssists * 0.05) + (avgGoals * 0.04);
            case DEL -> (avgShots / 3.0) * 0.04 + (avgGoals * 0.07) + (avgAssists * 0.03);
            default  -> 0.0;
        };
    }

    private double computeSeasonFactor(Map<String, Object> season, double expectedPoints) {
        if (season == null || expectedPoints <= 0) return 1.0;
        long matches = toLong(season.get("matches_played"));
        if (matches <= 0) return 1.0;
        double avgFantasy = (double) toLong(season.get("total_fantasy_points")) / matches;
        double ratingBoost = clamp(1.0 + ((toDouble(season.get("avg_rating"), 6.5) - 6.5) * 0.03), 0.92, 1.08);
        return clamp(avgFantasy / expectedPoints, 0.90, 1.15) * ratingBoost;
    }

    // ── Logging helper ────────────────────────────────────────────────────────

    private void logFactors(Player player, int baseValue, int newValue,
                            double form, double season, double minutes, double rating,
                            double momentum, double consistency, double impact,
                            double discipline, double demand, double activity) {
        log.debug("[MarketValueUpdate] {} ({}): {} -> {} | form={} season={} min={} rating={} mom={} con={} imp={} disc={} dem={} act={}",
                player.getId(), player.getFullName(), baseValue, newValue,
                String.format("%.3f", form), String.format("%.3f", season),
                String.format("%.3f", minutes), String.format("%.3f", rating),
                String.format("%.3f", momentum), String.format("%.3f", consistency),
                String.format("%.3f", impact), String.format("%.3f", discipline),
                String.format("%.3f", demand), activity);
    }

    // ── Numeric conversion utilities ──────────────────────────────────────────

    private int clampInt(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

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
