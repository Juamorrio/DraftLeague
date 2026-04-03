package com.DraftLeague.services;

import com.DraftLeague.dto.CreatePlayerStatisticRequest;
import com.DraftLeague.dto.PlayerStatisticsSummaryDTO;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerStatisticService Unit Tests")
class PlayerStatisticServiceTest {

    @Mock
    private PlayerStatisticRepository playerStatisticRepository;

    @Mock
    private PlayerStatisticFactory playerStatisticFactory;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamGameweekPointsRepository gwPointsRepository;

    @Mock
    private TeamPlayerGameweekPointsRepository tpgwPointsRepository;

    @InjectMocks
    private PlayerStatisticService playerStatisticService;


    @Test
    @DisplayName("saveStatistic: petición válida → guarda y devuelve la estadística")
    void saveStatistic_validRequest_savesAndReturns() {
        CreatePlayerStatisticRequest req = buildRequest("P1", 1);
        PlayerStatistic saved = new PlayerStatistic();
        saved.setPlayerId("P1");
        when(playerStatisticRepository.save(any(PlayerStatistic.class))).thenReturn(saved);

        PlayerStatistic result = playerStatisticService.saveStatistic(req);

        assertThat(result.getPlayerId()).isEqualTo("P1");
        verify(playerStatisticRepository).save(any(PlayerStatistic.class));
    }

    @Test
    @DisplayName("saveStatistic: llamada única a save")
    void saveStatistic_callsSaveExactlyOnce() {
        when(playerStatisticRepository.save(any())).thenReturn(new PlayerStatistic());

        playerStatisticService.saveStatistic(buildRequest("P2", 2));

        verify(playerStatisticRepository, times(1)).save(any());
    }


    @Test
    @DisplayName("saveStatistics: lista de estadísticas → delega en saveAll")
    void saveStatistics_delegatesToSaveAll() {
        List<PlayerStatistic> stats = List.of(new PlayerStatistic(), new PlayerStatistic());
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(stats);

        List<PlayerStatistic> result = playerStatisticService.saveStatistics(stats);

        assertThat(result).hasSize(2);
        verify(playerStatisticRepository).saveAll(stats);
    }


    @Test
    @DisplayName("getPlayerStatistics: devuelve estadísticas del repositorio")
    void getPlayerStatistics_returnsFromRepository() {
        List<PlayerStatistic> stats = List.of(new PlayerStatistic());
        when(playerStatisticRepository.findByPlayerId("P1")).thenReturn(stats);

        List<PlayerStatistic> result = playerStatisticService.getPlayerStatistics("P1");

        assertThat(result).hasSize(1);
    }


    @Test
    @DisplayName("getMatchStatistics: devuelve estadísticas del partido")
    void getMatchStatistics_returnsMatchStats() {
        List<PlayerStatistic> stats = List.of(new PlayerStatistic(), new PlayerStatistic());
        when(playerStatisticRepository.findByMatchId(1)).thenReturn(stats);

        List<PlayerStatistic> result = playerStatisticService.getMatchStatistics(1);

        assertThat(result).hasSize(2);
    }


    @Test
    @DisplayName("getPlayerStatisticsSummary: datos válidos → devuelve DTO no nulo")
    void getPlayerStatisticsSummary_withData_returnsNonNullDTO() {
        Map<String, Object> data = buildSummaryData("MIDFIELDER", 5);
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result).isNotNull();
        assertThat(result.getMatchesPlayed()).isEqualTo(5);
        assertThat(result.getPlayerId()).isEqualTo("P1");
    }

    @Test
    @DisplayName("getPlayerStatisticsSummary: datos nulos → devuelve null")
    void getPlayerStatisticsSummary_nullData_returnsNull() {
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(null);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getPlayerStatisticsSummary: matchesPlayed == 0 → devuelve null")
    void getPlayerStatisticsSummary_zeroMatchesPlayed_returnsNull() {
        Map<String, Object> data = buildSummaryData("MIDFIELDER", 0);
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result).isNull();
    }


    private CreatePlayerStatisticRequest buildRequest(String playerId, Integer matchId) {
        CreatePlayerStatisticRequest req = new CreatePlayerStatisticRequest();
        req.setPlayerId(playerId);
        req.setMatchId(matchId);
        req.setPlayerType(PlayerStatistic.PlayerType.MIDFIELDER);
        req.setMinutesPlayed(90);
        req.setGoals(0);
        req.setAssists(0);
        req.setYellowCards(0);
        req.setRedCards(0);
        return req;
    }

    private Map<String, Object> buildSummaryData(String playerType, int matchesPlayed) {
        Map<String, Object> data = new HashMap<>();
        data.put("matches_played", matchesPlayed);
        data.put("player_type", playerType);
        data.put("total_goals", 3);
        data.put("total_assists", 2);
        data.put("total_minutes_played", 900);
        data.put("total_fantasy_points", 50);
        data.put("total_shots", 10);
        data.put("total_accurate_passes", 80);
        data.put("total_passes", 100);
        data.put("total_chances_created", 5);
        data.put("total_tackles", 15);
        data.put("total_interceptions", 8);
        data.put("total_duels_won", 20);
        data.put("total_duels_lost", 10);
        data.put("total_yellow_cards", 1);
        data.put("total_red_cards", 0);
        data.put("total_penalty_committed", 0);
        data.put("times_captain", 2);
        data.put("avg_rating", 7.5);
        return data;
    }
}
