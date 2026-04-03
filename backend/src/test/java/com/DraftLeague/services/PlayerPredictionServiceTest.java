package com.DraftLeague.services;

import com.DraftLeague.dto.PlayerPredictionDTO;
import com.DraftLeague.dto.TeamPredictionDTO;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerPredictionService Unit Tests")
class PlayerPredictionServiceTest {

    @Mock
    private PlayerStatisticRepository statisticRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerTeamRepository playerTeamRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private XGBoostClient xgBoostClient;

    @InjectMocks
    private PlayerPredictionService playerPredictionService;


    @Test
    @DisplayName("predictForPlayer: jugador con suficientes stats → devuelve DTO con predictedPoints calculados")
    void predictForPlayer_withEnoughStats_returnsPrediction() {
        Player player = buildPlayer("P1", 541);
        List<PlayerStatistic> stats = buildStats("P1", 3);

        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P1")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P1"), any())).thenReturn(Optional.empty());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P1");

        assertThat(result).isNotNull();
        assertThat(result.getPlayerId()).isEqualTo("P1");
        assertThat(result.getPredictedPoints()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("predictForPlayer: menos de 2 stats → predictedPoints es 0.0")
    void predictForPlayer_fewerThanMinStats_returnsZeroPoints() {
        Player player = buildPlayer("P1", 541);
        List<PlayerStatistic> stats = buildStats("P1", 1); // solo 1, mínimo es 2

        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P1")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P1");

        assertThat(result.getPredictedPoints()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("predictForPlayer: segunda llamada → repositorio consultado solo una vez (caché)")
    void predictForPlayer_secondCall_usesCache() {
        Player player = buildPlayer("P2", 529);
        List<PlayerStatistic> stats = buildStats("P2", 2);

        when(playerRepository.findById("P2")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P2")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P2"), any())).thenReturn(Optional.empty());

        playerPredictionService.predictForPlayer("P2");
        playerPredictionService.predictForPlayer("P2"); // segunda llamada

        // El repositorio debe haberse llamado solo una vez
        verify(statisticRepository, times(1)).findByPlayerIdOrderByMatchIdDesc("P2");
    }

    @Test
    @DisplayName("invalidateCache: la siguiente llamada consulta el repositorio de nuevo")
    void invalidateCache_nextCallQueriesRepository() {
        Player player = buildPlayer("P3", 541);
        List<PlayerStatistic> stats = buildStats("P3", 2);

        when(playerRepository.findById("P3")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P3")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P3"), any())).thenReturn(Optional.empty());

        playerPredictionService.predictForPlayer("P3");
        playerPredictionService.invalidateCache();
        playerPredictionService.predictForPlayer("P3");

        verify(statisticRepository, times(2)).findByPlayerIdOrderByMatchIdDesc("P3");
    }


    @Test
    @DisplayName("predictForTeam: equipo sin alineación → totalPredictedPoints == 0.0")
    void predictForTeam_emptyLineup_returnsZeroPoints() {
        Team team = new Team();
        team.setId(1);
        when(teamRepository.findById(1)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(Collections.emptyList());

        TeamPredictionDTO result = playerPredictionService.predictForTeam(1);

        assertThat(result).isNotNull();
        assertThat(result.getTotalPredictedPoints()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("predictForTeam: equipo no encontrado → lanza RuntimeException")
    void predictForTeam_teamNotFound_throwsException() {
        when(teamRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerPredictionService.predictForTeam(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Team not found");
    }

    @Test
    @DisplayName("predictForPlayer: jugador con partido en casa → isHomeTeam true en DTO")
    void predictForPlayer_homeMatch_isHomeTrueInDTO() {
        Player player = buildPlayer("P4", 541);
        List<PlayerStatistic> stats = buildStats("P4", 1); // < 2 → early return

        Match upcoming = buildUpcomingMatch(1, 5, 541, 529);
        when(playerRepository.findById("P4")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P4")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(upcoming));

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P4");

        assertThat(result.getIsHomeTeam()).isTrue();
    }

    @Test
    @DisplayName("predictForPlayer: sin partidos próximos → nextMatchId es null")
    void predictForPlayer_noUpcomingMatches_nextMatchIdIsNull() {
        Player player = buildPlayer("P5", 541);
        List<PlayerStatistic> stats = buildStats("P5", 1);

        when(playerRepository.findById("P5")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P5")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P5");

        assertThat(result.getNextMatchId()).isNull();
    }


    private Player buildPlayer(String id, int clubId) {
        Player p = new Player();
        p.setId(id);
        p.setFullName("Player " + id);
        p.setPosition(Position.MID);
        p.setClubId(clubId);
        p.setMarketValue(10_000_000);
        p.setActive(true);
        p.setTotalPoints(0);
        return p;
    }

    private List<PlayerStatistic> buildStats(String playerId, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    PlayerStatistic s = new PlayerStatistic();
                    s.setPlayerId(playerId);
                    s.setMatchId(i + 1);
                    s.setPlayerType(PlayerStatistic.PlayerType.MIDFIELDER);
                    s.setMinutesPlayed(90);
                    s.setGoals(1);
                    s.setAssists(1);
                    s.setRating(7.5);
                    s.setYellowCards(0);
                    s.setRedCards(0);
                    return s;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private Match buildUpcomingMatch(int id, int round, int homeTeamId, int awayTeamId) {
        Match m = new Match();
        m.setId(id);
        m.setRound(round);
        m.setHomeTeamId(homeTeamId);
        m.setAwayTeamId(awayTeamId);
        m.setHomeClub("Home FC");
        m.setAwayClub("Away FC");
        m.setStatus(MatchStatus.UPCOMING);
        return m;
    }
}
