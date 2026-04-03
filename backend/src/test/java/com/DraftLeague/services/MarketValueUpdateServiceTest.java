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
        // Use a base value that is NOT a multiple of 50K
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
        // Base close to MIN, bad stats → raw result < 3M → clamped
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
        s.setGoals(goals);
        s.setAssists(assists);
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
}
