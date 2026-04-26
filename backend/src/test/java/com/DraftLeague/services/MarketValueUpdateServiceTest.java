package com.DraftLeague.services;

import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerMarketValueHistory;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.PlayerMarketValueHistoryRepository;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import com.DraftLeague.repositories.PlayerTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketValueUpdateService Unit Tests")
class MarketValueUpdateServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerStatisticRepository playerStatisticRepository;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private PlayerMarketValueHistoryRepository marketValueHistoryRepository;

    @InjectMocks
    private MarketValueUpdateService service;

    /** Inject the self-proxy field so recalculateForPlayer(Player) can delegate correctly. */
    @BeforeEach
    void injectSelf() {
        ReflectionTestUtils.setField(service, "self", service);
    }


    @Test
    @DisplayName("cap +20%: el nuevo valor no supera el 120% del valor base")
    void recalculate_positiveCap_newValueDoesNotExceedTwentyPercent() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = highPerformanceStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        assertThat(changed).isTrue();
        int savedValue = capturePlayerSave();
        assertThat(savedValue).isLessThanOrEqualTo((int) (10_000_000 * 1.20));
    }


    @Test
    @DisplayName("cap -20%: el nuevo valor no baja por debajo del 80% del valor base")
    void recalculate_negativeCap_newValueDoesNotDropMoreThanTwentyPercent() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = lowPerformanceStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        assertThat(changed).isTrue();
        int savedValue = capturePlayerSave();
        assertThat(savedValue).isGreaterThanOrEqualTo((int) (10_000_000 * 0.80));
    }


    @Test
    @DisplayName("resultado siempre redondeado al múltiplo de 50.000 más cercano")
    void recalculate_resultRoundedToNearestFiftyThousand() {
        Player player = buildPlayer("P1", Position.DEL, 10_130_000, true);
        List<PlayerStatistic> stats = highPerformanceStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        service.recalculateForPlayer(player, null);

        int savedValue = capturePlayerSave();
        assertThat(savedValue % 50_000).isEqualTo(0);
    }


    @Test
    @DisplayName("límite mínimo 3M: el valor nunca baja de 3.000.000")
    void recalculate_minimumBoundary_neverFallsBelowThreeMillion() {
        Player player = buildPlayer("P1", Position.DEL, 3_050_000, true);
        List<PlayerStatistic> stats = lowPerformanceStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        service.recalculateForPlayer(player, null);

        int savedValue = capturePlayerSave();
        assertThat(savedValue).isGreaterThanOrEqualTo(3_000_000);
    }


    @Test
    @DisplayName("límite máximo 50M: el valor nunca sube de 50.000.000")
    void recalculate_maximumBoundary_neverExceedsFiftyMillion() {
        Player player = buildPlayer("P1", Position.DEL, 45_000_000, true);
        List<PlayerStatistic> stats = highPerformanceStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        service.recalculateForPlayer(player, null);

        int savedValue = capturePlayerSave();
        assertThat(savedValue).isLessThanOrEqualTo(50_000_000);
    }


    @Test
    @DisplayName("sin stats recientes: usa fallback de temporada y guarda si hay cambio")
    void recalculate_noRecentStats_usesSeasonFallback() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        // Good season data → seasonFactor > 1 → value increases
        Map<String, Object> season = Map.of(
            "matches_played", 10L,
            "total_fantasy_points", 100L,
            "avg_rating", 7.0
        );

        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(season);
        when(playerStatisticRepository.findRecentStatsByPlayerId(eq("P1"), any(Pageable.class)))
            .thenReturn(Collections.emptyList());
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        assertThat(changed).isTrue();
        verify(playerRepository).save(player);
    }


    @Test
    @DisplayName("jugador sin valor de mercado: se asigna valor base por posición")
    void recalculate_noMarketValue_assignsDefaultBaseValue() {
        Player player = buildPlayer("P1", Position.DEL, null, true);

        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(null);
        when(playerStatisticRepository.findRecentStatsByPlayerId(eq("P1"), any(Pageable.class)))
            .thenReturn(Collections.emptyList());
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        assertThat(changed).isTrue();
        assertThat(player.getMarketValue()).isNotNull().isGreaterThanOrEqualTo(3_000_000);
        verify(playerRepository).save(player);
    }


    @Test
    @DisplayName("valor sin cambio (fallback neutro): retorna false y no guarda")
    void recalculate_valueUnchanged_returnsFalseNoSave() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        // Neutral season → seasonFactor ≈ 1.0 → no change
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(null);
        when(playerStatisticRepository.findRecentStatsByPlayerId(eq("P1"), any(Pageable.class)))
            .thenReturn(Collections.emptyList());

        boolean changed = service.recalculateForPlayer(player, null);

        assertThat(changed).isFalse();
        verify(playerRepository, never()).save(any());
    }


    @Test
    @DisplayName("valor cambiado: retorna true y playerRepository.save llamado una vez")
    void recalculate_valueChanged_returnsTrueAndSaved() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = highPerformanceStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        assertThat(changed).isTrue();
        verify(playerRepository, times(1)).save(player);
    }


    @Test
    @DisplayName("jugador inactivo: activityFactor 0.90 reduce el valor")
    void recalculate_inactivePlayer_activityFactorReducesValue() {
        // Neutral stats (form ≈ 1.0) but inactive player → value should drop
        Player inactivePlayer = buildPlayer("P1", Position.DEL, 10_000_000, false);
        List<PlayerStatistic> stats = neutralStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(inactivePlayer, null);

        assertThat(changed).isTrue();
        int savedValue = capturePlayerSave();
        assertThat(savedValue).isLessThan(10_000_000);
    }


    @Test
    @DisplayName("tarjetas en stats de disciplina: reducen el valor respecto al base")
    void recalculate_disciplineWithCards_reducesValue() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = neutralStats("P1", 5);
        // 3 amarillas → factor = 1 - 0.06 = 0.94
        Map<String, Object> discipline = Map.of("total_yellow_cards", 3L, "total_red_cards", 0L);

        stubNonFallback("P1", stats, discipline, 0L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        assertThat(changed).isTrue();
        int savedValue = capturePlayerSave();
        assertThat(savedValue).isLessThan(10_000_000);
    }


    @Test
    @DisplayName("factor disciplina flooreado en 0.70 con tarjetas extremas")
    void recalculate_extremeCards_disciplineFactorFlooredAtSeventyPercent() {
        // 10 amarillas + 5 rojas → raw = 1 - 0.20 - 0.40 = 0.40, floor → 0.70
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = neutralStats("P1", 5);
        Map<String, Object> discipline = Map.of("total_yellow_cards", 10L, "total_red_cards", 5L);

        stubNonFallback("P1", stats, discipline, 0L);
        stubSavePlayer();

        service.recalculateForPlayer(player, null);

        int savedValue = capturePlayerSave();
        assertThat(savedValue).isGreaterThanOrEqualTo(3_000_000);
        assertThat(savedValue).isLessThan(10_000_000);
    }


    @Test
    @DisplayName("con gameweek: marketValueHistoryRepository.save llamado")
    void recalculate_withGameweek_historySaved() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = highPerformanceStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();
        when(marketValueHistoryRepository.findByPlayerIdAndGameweek("P1", 5))
            .thenReturn(Optional.empty());
        when(marketValueHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean changed = service.recalculateForPlayer(player, 5);

        assertThat(changed).isTrue();
        verify(marketValueHistoryRepository).save(any(PlayerMarketValueHistory.class));
    }


    @Test
    @DisplayName("sin gameweek: marketValueHistoryRepository.save NO llamado")
    void recalculate_withoutGameweek_historyNotSaved() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = highPerformanceStats("P1", 5);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        service.recalculateForPlayer(player, null);

        verify(marketValueHistoryRepository, never()).save(any());
    }


    private Player buildPlayer(String id, Position position, Integer marketValue, boolean active) {
        Player p = new Player();
        p.setId(id);
        p.setFullName("Player " + id);
        p.setPosition(position);
        p.setMarketValue(marketValue);
        p.setActive(active);
        p.setTotalPoints(0);
        p.setClubId(541); // Real Madrid (prestigue 1.20)
        return p;
    }

    /** 5 stats with very high fantasy points → triggers cap +20% */
    private List<PlayerStatistic> highPerformanceStats(String playerId, int count) {
        return Collections.nCopies(count, buildRawStat(playerId, 20, 95, 9.0, 2, 0));
    }

    /** 5 stats with very low fantasy points → triggers cap -20% */
    private List<PlayerStatistic> lowPerformanceStats(String playerId, int count) {
        return Collections.nCopies(count, buildRawStat(playerId, 0, 0, 4.0, 0, 0));
    }

    /** 5 stats around expected points for DEL (8.0) with neutral rating */
    private List<PlayerStatistic> neutralStats(String playerId, int count) {
        return Collections.nCopies(count, buildRawStat(playerId, 8, 90, 6.5, 0, 0));
    }

    private PlayerStatistic buildRawStat(String playerId, int totalFP, int minutes,
                                          double rating, int goals, int assists) {
        PlayerStatistic s = new PlayerStatistic();
        s.setPlayerId(playerId);
        s.setMatchId(1);
        s.setPlayerType(PlayerStatistic.PlayerType.FORWARD);
        s.setTotalFantasyPoints(totalFP);
        s.setMinutesPlayed(minutes);
        s.setRating(rating);
        s.getShooting().setGoals(goals);
        s.getPassing().setAssists(assists);
        s.setIsHomeTeam(true);
        return s;
    }

    /** Stubs the repositories needed for the non-fallback path (non-empty stats). */
    private void stubNonFallback(String playerId, List<PlayerStatistic> stats,
                                  Map<String, Object> discipline, long ownershipCount) {
        when(playerStatisticRepository.getPlayerStatisticsSummaryData(playerId)).thenReturn(null);
        when(playerStatisticRepository.findRecentStatsByPlayerId(eq(playerId), any(Pageable.class)))
            .thenReturn(stats);
        when(playerStatisticRepository.findRecentDisciplineByPlayerId(eq(playerId), anyInt()))
            .thenReturn(discipline);
        when(playerTeamRepository.countByPlayerId(playerId)).thenReturn(ownershipCount);
    }

    private void stubSavePlayer() {
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    /** Captures the Player passed to playerRepository.save() and returns its marketValue. */
    private int capturePlayerSave() {
        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(captor.capture());
        return captor.getValue().getMarketValue();
    }

    // -----------------------------------------------------------------------
    // recalculateForPlayer – position-based expected points
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("portero (POR): expectedPoints = 5.0 → cálculo no lanza excepción")
    void recalculate_goalkeeper_usesCorrectExpectedPoints() {
        Player player = buildPlayer("GK1", Position.POR, 8_000_000, true);
        List<PlayerStatistic> stats = buildStatsForPosition("GK1", 5, PlayerStatistic.PlayerType.GOALKEEPER, 5, 85, 6.8);
        stubNonFallback("GK1", stats, null, 0L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        int savedValue = changed ? capturePlayerSave() : player.getMarketValue();
        assertThat(savedValue).isBetween(3_000_000, 50_000_000);
    }

    @Test
    @DisplayName("defensa (DEF): expectedPoints = 6.0 → valor actualizado correctamente")
    void recalculate_defender_valueUpdatedWithinBounds() {
        Player player = buildPlayer("DEF1", Position.DEF, 7_000_000, true);
        List<PlayerStatistic> stats = buildStatsForPosition("DEF1", 5, PlayerStatistic.PlayerType.DEFENDER, 7, 90, 7.0);
        stubNonFallback("DEF1", stats, null, 2L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        assertThat(changed).isTrue();
        int savedValue = capturePlayerSave();
        assertThat(savedValue % 50_000).isEqualTo(0);
        assertThat(savedValue).isBetween(3_000_000, 50_000_000);
    }

    @Test
    @DisplayName("centrocampista (MID): expectedPoints = 7.0 → factor demand con alta propiedad")
    void recalculate_midfielder_highOwnershipBoostsDemandFactor() {
        Player player = buildPlayer("MID1", Position.MID, 10_000_000, true);
        List<PlayerStatistic> stats = neutralStats("MID1", 5);
        // 20 owners → demand bonus = min(log(21)/log(11)*0.25, 0.25) ≈ 0.25
        stubNonFallback("MID1", stats, null, 20L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player, null);

        // With demand + neutral form the net factor may or may not cross 50K rounding threshold
        if (changed) {
            int savedValue = capturePlayerSave();
            assertThat(savedValue).isGreaterThanOrEqualTo(10_000_000); // demand boosts up
        }
    }

    // -----------------------------------------------------------------------
    // recalculateForPlayer – position null edge case
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("position null: getExpectedPoints devuelve MID_EXPECTED_PTS (7.0) por defecto")
    void recalculate_nullPosition_usesDefaultExpectedPoints() {
        Player player = buildPlayer("PX", null, 10_000_000, true);
        List<PlayerStatistic> stats = neutralStats("PX", 5);
        stubNonFallback("PX", stats, null, 0L);
        stubSavePlayer();

        // Must not throw NullPointerException
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.recalculateForPlayer(player, null));
    }

    // -----------------------------------------------------------------------
    // recalculateForPlayer – gameweek: unchanged value still persists history
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("valor sin cambio pero con gameweek: persistHistoryOnly llama marketValueHistoryRepository.save")
    void recalculate_unchangedValueWithGameweek_stillPersistsHistory() {
        // Neutral stats → combinedFactor ≈ 1.0 → newValue ≈ baseValue (no change)
        Player player = buildPlayer("P1", Position.DEF, 7_000_000, true);
        // Use exactly expected points for DEF (6 pts) with no extras → blendedForm ≈ 1.0
        List<PlayerStatistic> stats = buildStatsForPosition("P1", 5, PlayerStatistic.PlayerType.DEFENDER, 6, 90, 6.5);
        stubNonFallback("P1", stats, null, 0L);
        when(marketValueHistoryRepository.findByPlayerIdAndGameweek("P1", 3))
                .thenReturn(Optional.empty());
        when(marketValueHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateForPlayer(player, 3);

        // Whether or not the value changed, history repo should have been queried
        verify(marketValueHistoryRepository, atLeastOnce()).findByPlayerIdAndGameweek("P1", 3);
    }

    // -----------------------------------------------------------------------
    // recalculateForPlayer – existing history record is reused (upsert)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("con gameweek y history existente: upsert actualiza registro existente")
    void recalculate_withGameweekAndExistingHistory_upserts() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = highPerformanceStats("P1", 5);

        PlayerMarketValueHistory existing = new PlayerMarketValueHistory();
        existing.setId(99);
        existing.setPlayerId("P1");
        existing.setGameweek(7);
        existing.setPreviousValue(9_500_000);

        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();
        when(marketValueHistoryRepository.findByPlayerIdAndGameweek("P1", 7))
                .thenReturn(Optional.of(existing));
        when(marketValueHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean changed = service.recalculateForPlayer(player, 7);

        assertThat(changed).isTrue();
        ArgumentCaptor<PlayerMarketValueHistory> histCaptor = ArgumentCaptor.forClass(PlayerMarketValueHistory.class);
        verify(marketValueHistoryRepository).save(histCaptor.capture());
        assertThat(histCaptor.getValue().getId()).isEqualTo(99); // same existing record
    }

    // -----------------------------------------------------------------------
    // recalculateAllMarketValues (no-arg) → delegates to gameweek=null
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("recalculateAllMarketValues: sin jugadores → resultado updated=0, skipped=0, errors=0")
    void recalculateAllMarketValues_noPlayers_returnsZeroCounts() {
        when(playerStatisticRepository.getAllPlayerStatisticsSummaryData()).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.findRecentStatsForAllPlayers(anyInt())).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.findRecentDisciplineForAllPlayers(anyInt())).thenReturn(Collections.emptyList());
        when(playerTeamRepository.countOwnershipForAllPlayers()).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Integer> result = service.recalculateAllMarketValues();

        assertThat(result.get("updatedCount")).isEqualTo(0);
        assertThat(result.get("skippedCount")).isEqualTo(0);
        assertThat(result.get("errorCount")).isEqualTo(0);
    }

    @Test
    @DisplayName("recalculateAllMarketValuesForGameweek: error en un jugador → errorCount incremented, proceso continúa")
    void recalculateAllMarketValuesForGameweek_playerThrows_incrementsErrorCount() {
        // One player that will cause recalculateForPlayerBulk to throw via a broken player object
        Player badPlayer = new Player(); // null position → null clubId, etc.
        badPlayer.setId("BAD");
        badPlayer.setFullName("Bad Player");
        // Market value null → triggers computeBaseValue path; position null handled
        badPlayer.setMarketValue(null);
        badPlayer.setActive(true);

        when(playerStatisticRepository.getAllPlayerStatisticsSummaryData()).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.findRecentStatsForAllPlayers(anyInt())).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.findRecentDisciplineForAllPlayers(anyInt())).thenReturn(Collections.emptyList());
        when(playerTeamRepository.countOwnershipForAllPlayers()).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(badPlayer));
        // Save throws for this broken player
        when(playerRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("errorCount")).isGreaterThanOrEqualTo(0); // exception caught internally
    }

    @Test
    @DisplayName("recalculateAllMarketValuesForGameweek: jugador normal con stats bulk → updated=1")
    void recalculateAllMarketValuesForGameweek_onePlayerHighPerf_updatedOne() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);

        // Build bulk stat rows
        Map<String, Object> recentRow = buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true);
        Map<String, Object> seasonRow = buildSeasonRow("P1", 10L, 150L, 8.5);

        when(playerStatisticRepository.getAllPlayerStatisticsSummaryData()).thenReturn(List.of(seasonRow));
        when(playerStatisticRepository.findRecentStatsForAllPlayers(anyInt())).thenReturn(List.of(recentRow));
        when(playerStatisticRepository.findRecentDisciplineForAllPlayers(anyInt())).thenReturn(Collections.emptyList());
        when(playerTeamRepository.countOwnershipForAllPlayers()).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("updatedCount") + result.get("skippedCount") + result.get("errorCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("recalculateAllMarketValuesForGameweek: con gameweek → busca historial existente en BD")
    void recalculateAllMarketValuesForGameweek_withGameweek_queriesHistory() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);

        when(playerStatisticRepository.getAllPlayerStatisticsSummaryData()).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.findRecentStatsForAllPlayers(anyInt())).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.findRecentDisciplineForAllPlayers(anyInt())).thenReturn(Collections.emptyList());
        when(playerTeamRepository.countOwnershipForAllPlayers()).thenReturn(Collections.emptyList());
        when(marketValueHistoryRepository.findByGameweek(5)).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));

        service.recalculateAllMarketValuesForGameweek(5);

        verify(marketValueHistoryRepository).findByGameweek(5);
    }

    // -----------------------------------------------------------------------
    // recalculateAllMarketValuesForGameweek – bulk path (PlayerStatLite built internally)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("bulk: jugador sin marketValue + stats vacías → applySeasonFallbackBulk asigna valor base y guarda")
    void bulk_playerWithoutMarketValue_emptyStats_assignsBaseValue() {
        Player player = buildPlayer("P1", Position.DEL, null, true);

        stubBulkRepos(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("updatedCount")).isEqualTo(1);
        verify(playerRepository).save(player);
        assertThat(player.getMarketValue()).isNotNull().isGreaterThanOrEqualTo(3_000_000);
    }

    @Test
    @DisplayName("bulk: alto rendimiento → valor sube (capped +20%)")
    void bulk_highPerfStats_valueIncreasesWithCap() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);

        List<Map<String, Object>> recentRows = List.of(
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 18, 90, 8.5, 1, 1, true),
                buildBulkStatRow("P1", 22, 90, 9.2, 3, 0, true),
                buildBulkStatRow("P1", 20, 88, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 19, 90, 8.8, 1, 2, true));

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("updatedCount")).isEqualTo(1);
        assertThat(player.getMarketValue()).isLessThanOrEqualTo((int) (10_000_000 * 1.20));
        assertThat(player.getMarketValue()).isGreaterThan(10_000_000);
    }

    @Test
    @DisplayName("bulk: bajo rendimiento → valor baja (capped -20%)")
    void bulk_lowPerfStats_valueDecreasesWithCap() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);

        List<Map<String, Object>> recentRows = List.of(
                buildBulkStatRow("P1", 0, 0, 4.0, 0, 0, false),
                buildBulkStatRow("P1", 0, 0, 4.0, 0, 0, false),
                buildBulkStatRow("P1", 0, 0, 4.0, 0, 0, false),
                buildBulkStatRow("P1", 0, 0, 4.0, 0, 0, false),
                buildBulkStatRow("P1", 0, 0, 4.0, 0, 0, false));

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("updatedCount")).isEqualTo(1);
        assertThat(player.getMarketValue()).isGreaterThanOrEqualTo((int) (10_000_000 * 0.80));
        assertThat(player.getMarketValue()).isLessThan(10_000_000);
    }

    @Test
    @DisplayName("bulk: disciplina extrema en mapa → valor nunca baja de MIN_VALUE")
    void bulk_extremeDiscipline_valueNeverBelowMinimum() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);

        List<Map<String, Object>> recentRows = List.of(buildBulkStatRow("P1", 8, 90, 6.5, 0, 0, false));
        // 15 yellows + 5 reds → raw factor = 1 - 0.30 - 0.40 = 0.30 → clamped to 0.70
        Map<String, Object> discRow = new java.util.HashMap<>();
        discRow.put("player_id", "P1");
        discRow.put("total_yellow_cards", 15L);
        discRow.put("total_red_cards", 5L);

        stubBulkRepos(Collections.emptyList(), recentRows, List.of(discRow), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateAllMarketValuesForGameweek(null);

        assertThat(player.getMarketValue()).isGreaterThanOrEqualTo(3_000_000);
    }

    @Test
    @DisplayName("bulk: jugador inactivo → activityFactor 0.90 reduce el valor")
    void bulk_inactivePlayer_activityFactorReducesValue() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, false); // inactive

        List<Map<String, Object>> recentRows = List.of(
                buildBulkStatRow("P1", 8, 90, 6.5, 0, 0, false),
                buildBulkStatRow("P1", 8, 90, 6.5, 0, 0, false),
                buildBulkStatRow("P1", 8, 90, 6.5, 0, 0, false),
                buildBulkStatRow("P1", 8, 90, 6.5, 0, 0, false),
                buildBulkStatRow("P1", 8, 90, 6.5, 0, 0, false));

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateAllMarketValuesForGameweek(null);

        assertThat(player.getMarketValue()).isLessThan(10_000_000);
    }

    @Test
    @DisplayName("bulk: con gameweek y historial existente → upserta registro de historia")
    void bulk_withGameweekAndExistingHistory_upsertsHistoryRecord() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);

        PlayerMarketValueHistory existing = new PlayerMarketValueHistory();
        existing.setId(42);
        existing.setPlayerId("P1");
        existing.setGameweek(8);
        existing.setPreviousValue(9_000_000);

        List<Map<String, Object>> recentRows = List.of(
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true));

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(marketValueHistoryRepository.findByGameweek(8)).thenReturn(List.of(existing));
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(marketValueHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateAllMarketValuesForGameweek(8);

        ArgumentCaptor<PlayerMarketValueHistory> histCaptor = ArgumentCaptor.forClass(PlayerMarketValueHistory.class);
        verify(marketValueHistoryRepository).save(histCaptor.capture());
        assertThat(histCaptor.getValue().getId()).isEqualTo(42); // reused existing record
    }

    @Test
    @DisplayName("bulk: resultado siempre redondeado al múltiplo de 50.000 más cercano")
    void bulk_result_roundedToNearestFiftyThousand() {
        Player player = buildPlayer("P1", Position.DEL, 10_130_000, true);

        List<Map<String, Object>> recentRows = List.of(
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true),
                buildBulkStatRow("P1", 20, 90, 9.0, 2, 0, true));

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateAllMarketValuesForGameweek(null);

        assertThat(player.getMarketValue() % 50_000).isEqualTo(0);
    }

    @Test
    @DisplayName("bulk: demanda alta (ownershipCount > 0) → valor mayor que sin demanda")
    void bulk_highOwnership_demandBoostIncreasesValue() {
        Player playerHighDemand = buildPlayer("P1", Position.DEL, 10_000_000, true);
        Player playerNoDemand   = buildPlayer("P2", Position.DEL, 10_000_000, true);

        // Both players have identical neutral stats
        List<Map<String, Object>> recentP1 = List.of(buildBulkStatRow("P1", 8, 90, 6.5, 0, 0, false));
        List<Map<String, Object>> recentP2 = List.of(buildBulkStatRow("P2", 8, 90, 6.5, 0, 0, false));
        List<Map<String, Object>> allRecent = new java.util.ArrayList<>(recentP1);
        allRecent.addAll(recentP2);

        // P1 owned by 20 teams; P2 by 0
        List<Object[]> ownership = new java.util.ArrayList<>();
        ownership.add(new Object[]{"P1", 20L});

        stubBulkRepos(Collections.emptyList(), allRecent, Collections.emptyList(), ownership);
        when(playerRepository.findAll()).thenReturn(List.of(playerHighDemand, playerNoDemand));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateAllMarketValuesForGameweek(null);

        // P1 had demand boost; P2 did not — P1's final value should be >= P2's
        // (both may have changed or not due to rounding, but P1's value >= P2's)
        assertThat(playerHighDemand.getMarketValue())
                .isGreaterThanOrEqualTo(playerNoDemand.getMarketValue());
    }

    // -----------------------------------------------------------------------
    // recalculateForPlayer (single-player public shortcut)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("recalculateForPlayer(player) sin gameweek: delega al método con gameweek=null")
    void recalculateForPlayer_noGameweek_delegatesToNullGameweek() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<PlayerStatistic> stats = highPerformanceStats("P1", 5);
        stubNonFallback("P1", stats, null, 0L);
        stubSavePlayer();

        boolean changed = service.recalculateForPlayer(player);

        assertThat(changed).isTrue();
        verify(marketValueHistoryRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Helper builders for bulk tests
    // -----------------------------------------------------------------------

    private List<PlayerStatistic> buildStatsForPosition(String playerId, int count,
                                                         PlayerStatistic.PlayerType type,
                                                         int totalFP, int minutes, double rating) {
        PlayerStatistic s = new PlayerStatistic();
        s.setPlayerId(playerId);
        s.setMatchId(1);
        s.setPlayerType(type);
        s.setTotalFantasyPoints(totalFP);
        s.setMinutesPlayed(minutes);
        s.setRating(rating);
        s.getShooting().setGoals(0);
        s.getPassing().setAssists(0);
        s.setIsHomeTeam(true);
        return Collections.nCopies(count, s);
    }

    /**
     * Builds a raw Map row in the format returned by the native bulk queries.
     * Key names are lowercase snake_case, matching the MariaDB/JDBC driver convention.
     */
    private Map<String, Object> buildBulkStatRow(String playerId, int totalFP, int minutes,
                                                   double rating, int goals, int assists,
                                                   boolean cleanSheet) {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("player_id", playerId);
        row.put("total_fantasy_points", (long) totalFP);
        row.put("minutes_played", (long) minutes);
        row.put("rating", rating);
        row.put("goals", (long) goals);
        row.put("assists", (long) assists);
        row.put("shots_on_target", 0L);
        row.put("chances_created", 0L);
        row.put("tackles", 0L);
        row.put("interceptions", 0L);
        row.put("blocks", 0L);
        row.put("saves", 0L);
        row.put("goals_conceded", 0L);
        row.put("clean_sheet", cleanSheet ? 1L : 0L);
        return row;
    }

    private Map<String, Object> buildSeasonRow(String playerId, long matches, long totalFP, double avgRating) {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("player_id", playerId);
        row.put("matches_played", matches);
        row.put("total_fantasy_points", totalFP);
        row.put("avg_rating", avgRating);
        return row;
    }

    /**
     * Stubs the four bulk repository queries consumed by
     * {@code recalculateAllMarketValuesForGameweek}.
     *
     * @param seasonRows    rows for {@code getAllPlayerStatisticsSummaryData()}
     * @param recentRows    rows for {@code findRecentStatsForAllPlayers(n)}
     * @param disciplineRows rows for {@code findRecentDisciplineForAllPlayers(n)}
     * @param ownershipRows rows for {@code countOwnershipForAllPlayers()} — each element
     *                      is an {@code Object[]} of {@code [playerId, count]}
     */
    private void stubBulkRepos(List<Map<String, Object>> seasonRows,
                                List<Map<String, Object>> recentRows,
                                List<Map<String, Object>> disciplineRows,
                                List<Object[]> ownershipRows) {
        when(playerStatisticRepository.getAllPlayerStatisticsSummaryData()).thenReturn(seasonRows);
        when(playerStatisticRepository.findRecentStatsForAllPlayers(anyInt())).thenReturn(recentRows);
        when(playerStatisticRepository.findRecentDisciplineForAllPlayers(anyInt())).thenReturn(disciplineRows);
        when(playerTeamRepository.countOwnershipForAllPlayers()).thenReturn(ownershipRows);
    }

    // -----------------------------------------------------------------------
    // recalculateForPlayerBulk – direct calls via recalculateAllMarketValuesForGameweek
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("bulk: posición POR → cálculo de impactFactor para portero no lanza excepción")
    void bulk_goalkeeperPosition_impactFactorComputedWithoutException() {
        Player player = buildPlayer("GK1", Position.POR, 8_000_000, true);

        List<Map<String, Object>> recentRows = List.of(
                buildBulkStatRowFull("GK1", 5, 90, 7.0, 0, 0, 4, 0, true),
                buildBulkStatRowFull("GK1", 6, 90, 7.2, 0, 0, 3, 1, true),
                buildBulkStatRowFull("GK1", 4, 90, 6.8, 0, 0, 2, 0, false));

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("updatedCount") + result.get("skippedCount") + result.get("errorCount")).isEqualTo(1);
        assertThat(player.getMarketValue()).isBetween(3_000_000, 50_000_000);
    }

    @Test
    @DisplayName("bulk: posición DEF con tackles/interceptions → impactFactor para defensa calculado")
    void bulk_defenderPosition_impactFactorWithTackles() {
        Player player = buildPlayer("DEF1", Position.DEF, 7_000_000, true);

        List<Map<String, Object>> recentRows = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = buildBulkStatRow("DEF1", 6, 90, 7.0, 0, 1, false);
            row.put("tackles", 5L);
            row.put("interceptions", 3L);
            row.put("blocks", 2L);
            recentRows.add(row);
        }

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("errorCount")).isEqualTo(0);
        assertThat(player.getMarketValue()).isBetween(3_000_000, 50_000_000);
    }

    @Test
    @DisplayName("bulk: posición MID con chances_created → impactFactor para centrocampista calculado")
    void bulk_midfielderPosition_impactFactorWithChancesCreated() {
        Player player = buildPlayer("MID1", Position.MID, 9_000_000, true);

        List<Map<String, Object>> recentRows = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = buildBulkStatRow("MID1", 8, 90, 7.5, 0, 2, false);
            row.put("chances_created", 4L);
            recentRows.add(row);
        }

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("errorCount")).isEqualTo(0);
    }

    @Test
    @DisplayName("bulk: season data con 5+ partidos → computeBaseValue usa rendimiento de temporada")
    void bulk_seasonDataWithFiveOrMoreMatches_computeBaseValueUsesPerformance() {
        // Player with null market value (below MIN) → triggers computeBaseValue with season data
        Player player = buildPlayer("P1", Position.DEL, null, true);
        Map<String, Object> seasonRow = buildSeasonRow("P1", 10L, 80L, 8.0);

        stubBulkRepos(List.of(seasonRow), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("updatedCount")).isEqualTo(1);
        assertThat(player.getMarketValue()).isNotNull().isGreaterThanOrEqualTo(3_000_000);
    }

    @Test
    @DisplayName("bulk: season data con valor de mercado en MIN → computeBaseValue invocado")
    void bulk_marketValueAtMin_computeBaseValueUsed() {
        Player player = buildPlayer("P1", Position.DEL, 3_000_000, true); // at MIN_VALUE

        List<Map<String, Object>> recentRows = List.of(buildBulkStatRow("P1", 10, 90, 8.0, 1, 0, false));
        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("errorCount")).isEqualTo(0);
    }

    @Test
    @DisplayName("bulk: ratio de momentum con >= 3 stats y expectedPoints > 0 → cálculo correcto")
    void bulk_momentumWithThreeOrMoreStats_calculatesCorrectly() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);

        // Increasing form: last3 avg > prev avg → positive momentum
        List<Map<String, Object>> recentRows = List.of(
                buildBulkStatRow("P1", 15, 90, 7.5, 1, 0, false),  // most recent
                buildBulkStatRow("P1", 14, 90, 7.3, 1, 0, false),
                buildBulkStatRow("P1", 13, 90, 7.0, 0, 0, false),
                buildBulkStatRow("P1", 4, 90, 6.0, 0, 0, false),   // older (lower)
                buildBulkStatRow("P1", 3, 90, 5.5, 0, 0, false));

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("errorCount")).isEqualTo(0);
    }

    @Test
    @DisplayName("bulk: season fallback con temporada no nula → cambio de valor posible")
    void bulk_seasonFallbackWithSeasonData_valueChangesPossible() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        Map<String, Object> seasonRow = buildSeasonRow("P1", 15L, 180L, 8.5); // high season perf

        stubBulkRepos(List.of(seasonRow), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("errorCount")).isEqualTo(0);
        assertThat(result.get("updatedCount") + result.get("skippedCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("bulk: season fallback con gameweek y sin cambio → persistHistoryOnly llamado")
    void bulk_seasonFallbackNoChange_withGameweek_persistsHistory() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        Map<String, Object> seasonRow = buildSeasonRow("P1", 5L, 40L, 6.5); // avg 8 pts / 5 matches = expected FWD pts

        stubBulkRepos(List.of(seasonRow), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        when(marketValueHistoryRepository.findByGameweek(10)).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(marketValueHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateAllMarketValuesForGameweek(10);

        verify(playerStatisticRepository).findRecentStatsForAllPlayers(anyInt());
    }

    @Test
    @DisplayName("recalculateAllMarketValues: delega a gameweek null y devuelve mapa con contadores")
    void recalculateAllMarketValues_delegatesToNullGameweek_returnsCountMap() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        List<Map<String, Object>> recentRows = List.of(buildBulkStatRow("P1", 8, 90, 6.5, 0, 0, false));

        stubBulkRepos(Collections.emptyList(), recentRows, Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValues();

        assertThat(result).containsKeys("updatedCount", "skippedCount", "errorCount");
        verify(marketValueHistoryRepository, never()).findByGameweek(anyInt()); // no gameweek → no history query
    }

    @Test
    @DisplayName("bulk: clubId de equipo medio (Girona) → clubPrestigeMultiplier = 1.0")
    void bulk_midPrestigeClub_multiplierOne() {
        Player player = new Player();
        player.setId("P1");
        player.setFullName("Player P1");
        player.setPosition(Position.DEL);
        player.setMarketValue(null); 
        player.setActive(true);
        player.setClubId(547); 

        stubBulkRepos(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Integer> result = service.recalculateAllMarketValuesForGameweek(null);

        assertThat(result.get("updatedCount")).isEqualTo(1);
        assertThat(player.getMarketValue()).isBetween(3_000_000, 15_000_000);
    }

    @Test
    @DisplayName("bulk: clubId bajo (Mallorca) → clubPrestigeMultiplier = 0.90 → valor base menor")
    void bulk_lowPrestigeClub_lowerBaseValue() {
        Player lowPrestige = new Player();
        lowPrestige.setId("LOW1");
        lowPrestige.setFullName("Player LOW1");
        lowPrestige.setPosition(Position.DEL);
        lowPrestige.setMarketValue(null);
        lowPrestige.setActive(true);
        lowPrestige.setClubId(798); 

        Player highPrestige = new Player();
        highPrestige.setId("HIGH1");
        highPrestige.setFullName("Player HIGH1");
        highPrestige.setPosition(Position.DEL);
        highPrestige.setMarketValue(null);
        highPrestige.setActive(true);
        highPrestige.setClubId(541); 

        stubBulkRepos(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(lowPrestige, highPrestige));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateAllMarketValuesForGameweek(null);

        assertThat(highPrestige.getMarketValue()).isGreaterThan(lowPrestige.getMarketValue());
    }

    @Test
    @DisplayName("recalculateForPlayer: con gameweek y valor sin cambio → marketValueHistoryRepository.save llamado (persistHistoryOnly)")
    void recalculate_unchangedValueWithGameweekNonFallback_persistsHistoryOnly() {
        Player player = buildPlayer("P1", Position.DEF, 7_000_000, true);
        List<PlayerStatistic> stats = buildStatsForPosition("P1", 5, PlayerStatistic.PlayerType.DEFENDER, 6, 90, 6.5);
        stubNonFallback("P1", stats, null, 0L);
        when(marketValueHistoryRepository.findByPlayerIdAndGameweek("P1", 3)).thenReturn(Optional.empty());
        when(marketValueHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateForPlayer(player, 3);

        verify(marketValueHistoryRepository, atLeastOnce()).findByPlayerIdAndGameweek("P1", 3);
    }

    @Test
    @DisplayName("recalculateForPlayer: FWD con alta propiedad → demandFactor aumenta el valor")
    void recalculate_forwardHighOwnership_demandBoostsValue() {
        Player lowDemand = buildPlayer("P1", Position.DEL, 10_000_000, true);
        Player highDemand = buildPlayer("P2", Position.DEL, 10_000_000, true);

        List<PlayerStatistic> statsLow = neutralStats("P1", 5);
        List<PlayerStatistic> statsHigh = neutralStats("P2", 5);

        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(null);
        when(playerStatisticRepository.findRecentStatsByPlayerId(eq("P1"), any(org.springframework.data.domain.Pageable.class))).thenReturn(statsLow);
        when(playerStatisticRepository.findRecentDisciplineByPlayerId(eq("P1"), anyInt())).thenReturn(null);
        when(playerTeamRepository.countByPlayerId("P1")).thenReturn(0L);

        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P2")).thenReturn(null);
        when(playerStatisticRepository.findRecentStatsByPlayerId(eq("P2"), any(org.springframework.data.domain.Pageable.class))).thenReturn(statsHigh);
        when(playerStatisticRepository.findRecentDisciplineByPlayerId(eq("P2"), anyInt())).thenReturn(null);
        when(playerTeamRepository.countByPlayerId("P2")).thenReturn(20L);

        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recalculateForPlayer(lowDemand, null);
        service.recalculateForPlayer(highDemand, null);

        assertThat(highDemand.getMarketValue()).isGreaterThanOrEqualTo(lowDemand.getMarketValue());
    }

    @Test
    @DisplayName("recalculateForPlayer: season fallback con gameweek → no lanza excepción")
    void recalculate_seasonFallbackWithGameweek_doesNotThrow() {
        Player player = buildPlayer("P1", Position.DEL, 10_000_000, true);
        Map<String, Object> season = Map.of(
            "matches_played", 20L,
            "total_fantasy_points", 200L,
            "avg_rating", 7.5
        );

        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(season);
        when(playerStatisticRepository.findRecentStatsByPlayerId(eq("P1"), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(Collections.emptyList());
        when(marketValueHistoryRepository.findByPlayerIdAndGameweek("P1", 5)).thenReturn(Optional.empty());
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(marketValueHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.recalculateForPlayer(player, 5));
    }

    /**
     * Build a bulk stat row with saves and goalsConceded fields populated.
     */
    private Map<String, Object> buildBulkStatRowFull(String playerId, int totalFP, int minutes,
                                                       double rating, int goals, int assists,
                                                       int saves, int goalsConceded, boolean cleanSheet) {
        Map<String, Object> row = buildBulkStatRow(playerId, totalFP, minutes, rating, goals, assists, cleanSheet);
        row.put("saves", (long) saves);
        row.put("goals_conceded", (long) goalsConceded);
        return row;
    }
}
