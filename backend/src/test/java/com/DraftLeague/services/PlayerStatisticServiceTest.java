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

    // -----------------------------------------------------------------------
    // getPlayerStatisticsSummary – computed averages
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getPlayerStatisticsSummary: avgGoals calculado correctamente (totalGoals / matches)")
    void getPlayerStatisticsSummary_avgGoalsCalculatedCorrectly() {
        Map<String, Object> data = buildSummaryData("FORWARD", 10);
        data.put("total_goals", 20);
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result.getAvgGoals()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("getPlayerStatisticsSummary: avgPassAccuracy = 0 cuando total_passes es 0")
    void getPlayerStatisticsSummary_zeroPasses_avgPassAccuracyIsZero() {
        Map<String, Object> data = buildSummaryData("MIDFIELDER", 5);
        data.put("total_passes", 0);
        data.put("total_accurate_passes", 0);
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result.getAvgPassAccuracy()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getPlayerStatisticsSummary: datos vacíos (mapa vacío) → devuelve null")
    void getPlayerStatisticsSummary_emptyMap_returnsNull() {
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1"))
                .thenReturn(new HashMap<>());

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getPlayerStatisticsSummary: matchesPlayed como Number (Long) → correcto")
    void getPlayerStatisticsSummary_matchesPlayedAsLong_parsedCorrectly() {
        Map<String, Object> data = buildSummaryData("DEFENDER", 8);
        data.put("matches_played", 8L); // Long, not Integer
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result).isNotNull();
        assertThat(result.getMatchesPlayed()).isEqualTo(8);
    }

    // -----------------------------------------------------------------------
    // getPlayerMatchStatistic
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getPlayerMatchStatistic: datos nulos → devuelve null")
    void getPlayerMatchStatistic_nullData_returnsNull() {
        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1)).thenReturn(null);

        Object result = playerStatisticService.getPlayerMatchStatistic("P1", 1);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getPlayerMatchStatistic: mapa vacío → devuelve null")
    void getPlayerMatchStatistic_emptyData_returnsNull() {
        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1))
                .thenReturn(new HashMap<>());

        Object result = playerStatisticService.getPlayerMatchStatistic("P1", 1);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getPlayerMatchStatistic: player_type nulo → devuelve null")
    void getPlayerMatchStatistic_nullPlayerType_returnsNull() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        data.put("player_id", "P1");
        // player_type deliberately absent
        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1)).thenReturn(data);

        Object result = playerStatisticService.getPlayerMatchStatistic("P1", 1);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getPlayerMatchStatistic: GOALKEEPER → devuelve GoalkeeperStatisticDTO")
    void getPlayerMatchStatistic_goalkeeper_returnsGoalkeeperDTO() {
        Map<String, Object> data = buildMatchStatData("GOALKEEPER");
        data.put("saves", 4);
        data.put("goals_conceded", 1);
        data.put("penalties_saved", 0);
        data.put("clean_sheet", false);

        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1)).thenReturn(data);

        Object result = playerStatisticService.getPlayerMatchStatistic("P1", 1);

        assertThat(result).isInstanceOf(com.DraftLeague.dto.GoalkeeperStatisticDTO.class);
        com.DraftLeague.dto.GoalkeeperStatisticDTO dto = (com.DraftLeague.dto.GoalkeeperStatisticDTO) result;
        assertThat(dto.getSaves()).isEqualTo(4);
    }

    @Test
    @DisplayName("getPlayerMatchStatistic: DEFENDER → devuelve DefenderStatisticDTO")
    void getPlayerMatchStatistic_defender_returnsDefenderDTO() {
        Map<String, Object> data = buildMatchStatData("DEFENDER");
        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1)).thenReturn(data);

        Object result = playerStatisticService.getPlayerMatchStatistic("P1", 1);

        assertThat(result).isInstanceOf(com.DraftLeague.dto.DefenderStatisticDTO.class);
    }

    @Test
    @DisplayName("getPlayerMatchStatistic: MIDFIELDER → devuelve MidfielderStatisticDTO")
    void getPlayerMatchStatistic_midfielder_returnsMidfielderDTO() {
        Map<String, Object> data = buildMatchStatData("MIDFIELDER");
        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1)).thenReturn(data);

        Object result = playerStatisticService.getPlayerMatchStatistic("P1", 1);

        assertThat(result).isInstanceOf(com.DraftLeague.dto.MidfielderStatisticDTO.class);
    }

    @Test
    @DisplayName("getPlayerMatchStatistic: FORWARD → devuelve ForwardStatisticDTO")
    void getPlayerMatchStatistic_forward_returnsForwardDTO() {
        Map<String, Object> data = buildMatchStatData("FORWARD");
        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1)).thenReturn(data);

        Object result = playerStatisticService.getPlayerMatchStatistic("P1", 1);

        assertThat(result).isInstanceOf(com.DraftLeague.dto.ForwardStatisticDTO.class);
    }

    @Test
    @DisplayName("getPlayerMatchStatistic: passAccuracy calculado cuando total_passes > 0")
    void getPlayerMatchStatistic_withPasses_passAccuracyCalculated() {
        Map<String, Object> data = buildMatchStatData("MIDFIELDER");
        data.put("total_passes", 50);
        data.put("accurate_passes", 40);
        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1)).thenReturn(data);

        Object result = playerStatisticService.getPlayerMatchStatistic("P1", 1);

        assertThat(result).isInstanceOf(com.DraftLeague.dto.MidfielderStatisticDTO.class);
        com.DraftLeague.dto.MidfielderStatisticDTO dto = (com.DraftLeague.dto.MidfielderStatisticDTO) result;
        assertThat(dto.getPassAccuracy()).isEqualTo(80.0);
    }

    // -----------------------------------------------------------------------
    // saveBulkFromJson
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("saveBulkFromJson: jugador desconocido es ignorado → saveAll con lista vacía")
    void saveBulkFromJson_unknownPlayer_ignored() {
        when(playerRepository.findAllIds()).thenReturn(List.of("KNOWN"));
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(List.of());

        Map<String, Object> unknownRow = new HashMap<>();
        unknownRow.put("playerId", "UNKNOWN");
        unknownRow.put("playerType", "MIDFIELDER");

        List<PlayerStatistic> result = playerStatisticService.saveBulkFromJson(List.of(unknownRow));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("saveBulkFromJson: jugador conocido → factory invocada y stat guardada")
    void saveBulkFromJson_knownPlayer_factoryCalledAndSaved() {
        PlayerStatistic stat = new PlayerStatistic();
        stat.setPlayerType(PlayerStatistic.PlayerType.MIDFIELDER);
        stat.setPlayerId("KNOWN");

        when(playerRepository.findAllIds()).thenReturn(List.of("KNOWN"));
        when(playerStatisticFactory.createStatistic("MIDFIELDER")).thenReturn(stat);
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> row = buildJsonRow("KNOWN", "MIDFIELDER");

        List<PlayerStatistic> result = playerStatisticService.saveBulkFromJson(List.of(row));

        assertThat(result).hasSize(1);
        verify(playerStatisticFactory).createStatistic("MIDFIELDER");
    }

    @Test
    @DisplayName("saveBulkFromJson: lista vacía → saveAll con lista vacía")
    void saveBulkFromJson_emptyList_savesNothing() {
        when(playerRepository.findAllIds()).thenReturn(List.of("P1"));
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(List.of());

        List<PlayerStatistic> result = playerStatisticService.saveBulkFromJson(List.of());

        assertThat(result).isEmpty();
        verify(playerStatisticRepository).saveAll(argThat(l -> ((java.util.List<?>) l).isEmpty()));
    }

    // -----------------------------------------------------------------------
    // getPlayerMatchesSummary
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getPlayerMatchesSummary: sin resultados del repositorio → lista vacía")
    void getPlayerMatchesSummary_noResults_returnsEmptyList() {
        when(playerStatisticRepository.getPlayerMatchesSummary("P1")).thenReturn(List.of());
        when(playerStatisticRepository.findByPlayerId("P1")).thenReturn(List.of());
        when(gwPointsRepository.findByTeamId(1)).thenReturn(List.of());
        when(tpgwPointsRepository.findByTeamIdAndPlayerId(1, "P1")).thenReturn(List.of());

        List<?> result = playerStatisticService.getPlayerMatchesSummary("P1", 1);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPlayerMatchesSummary: teamId null → no consulta chips")
    void getPlayerMatchesSummary_nullTeamId_skipsChipLookup() {
        when(playerStatisticRepository.getPlayerMatchesSummary("P1")).thenReturn(List.of());
        when(playerStatisticRepository.findByPlayerId("P1")).thenReturn(List.of());

        List<?> result = playerStatisticService.getPlayerMatchesSummary("P1", null);

        assertThat(result).isEmpty();
        verify(gwPointsRepository, never()).findByTeamId(anyInt());
        verify(tpgwPointsRepository, never()).findByTeamIdAndPlayerId(anyInt(), anyString());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Map<String, Object> buildMatchStatData(String playerType) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        data.put("player_id", "P1");
        data.put("match_id", 1);
        data.put("player_type", playerType);
        data.put("is_home_team", true);
        data.put("role", "starter");
        data.put("rating", 7.0);
        data.put("minutes_played", 90);
        data.put("total_fantasy_points", 10);
        data.put("goals", 1);
        data.put("assists", 0);
        data.put("total_shots", 3);
        data.put("shots_on_target", 2);
        data.put("chances_created", 1);
        data.put("successful_dribbles", 0);
        data.put("total_dribbles", 0);
        data.put("dribbled_past", 0);
        data.put("offsides", 0);
        data.put("total_passes", 0);
        data.put("accurate_passes", 0);
        data.put("accurate_crosses", 0);
        data.put("total_crosses", 0);
        data.put("tackles", 2);
        data.put("blocks", 1);
        data.put("interceptions", 1);
        data.put("duels_won", 4);
        data.put("duels_lost", 2);
        data.put("was_fouled", 1);
        data.put("fouls_committed", 1);
        data.put("yellow_cards", 0);
        data.put("red_cards", 0);
        data.put("penalties_won", 0);
        data.put("penalty_scored", 0);
        data.put("penalty_missed", 0);
        data.put("is_substitute", false);
        data.put("is_captain", false);
        data.put("shirt_number", 10);
        data.put("penalty_committed", 0);
        return data;
    }

    private Map<String, Object> buildJsonRow(String playerId, String playerType) {
        Map<String, Object> row = new HashMap<>();
        row.put("playerId", playerId);
        row.put("playerType", playerType);
        row.put("minutesPlayed", 90);
        row.put("goals", 0);
        row.put("assists", 0);
        row.put("yellowCards", 0);
        row.put("redCards", 0);
        row.put("isHomeTeam", true);
        row.put("isSubstitute", false);
        row.put("isCaptain", false);
        return row;
    }

    // -----------------------------------------------------------------------
    // getPlayerStatisticsSummary – additional field coverage
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getPlayerStatisticsSummary: avgFantasyPoints calculado correctamente")
    void getPlayerStatisticsSummary_avgFantasyPointsCalculated() {
        Map<String, Object> data = buildSummaryData("FORWARD", 5);
        data.put("total_fantasy_points", 50);
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result.getAvgFantasyPoints()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("getPlayerStatisticsSummary: avgDuelsWon calculado correctamente")
    void getPlayerStatisticsSummary_avgDuelsWonCalculated() {
        Map<String, Object> data = buildSummaryData("MIDFIELDER", 4);
        data.put("total_duels_won", 20);
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result.getAvgDuelsWon()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("getPlayerStatisticsSummary: playerType guardado en DTO")
    void getPlayerStatisticsSummary_playerTypePropagated() {
        Map<String, Object> data = buildSummaryData("GOALKEEPER", 3);
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("GK1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("GK1");

        assertThat(result.getPlayerType()).isEqualTo("GOALKEEPER");
    }

    @Test
    @DisplayName("getPlayerStatisticsSummary: avgPassAccuracy calculado con passes > 0")
    void getPlayerStatisticsSummary_passesNonZero_passAccuracyCalculated() {
        Map<String, Object> data = buildSummaryData("MIDFIELDER", 5);
        data.put("total_passes", 100);
        data.put("total_accurate_passes", 75);
        when(playerStatisticRepository.getPlayerStatisticsSummaryData("P1")).thenReturn(data);

        PlayerStatisticsSummaryDTO result = playerStatisticService.getPlayerStatisticsSummary("P1");

        assertThat(result.getAvgPassAccuracy()).isEqualTo(75.0);
    }

    // -----------------------------------------------------------------------
    // getPlayerMatchesSummary – with actual data rows
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getPlayerMatchesSummary: una fila válida con round → agrupa en JornadaMatchesDTO")
    void getPlayerMatchesSummary_oneValidRow_groupedByRound() {
        Map<String, Object> row = new HashMap<>();
        row.put("matchId", 1);
        row.put("round", 5);
        row.put("homeTeam", "Real Madrid");
        row.put("awayTeam", "Barcelona");
        row.put("opponent", "Barcelona");
        row.put("isHomeTeam", true);
        row.put("goalsScored", 2);
        row.put("goalsConceded", 1);
        row.put("minutesPlayed", 90);
        row.put("rating", 7.5);
        row.put("fantasyPoints", 10);
        row.put("goals", 1);
        row.put("assists", 0);
        row.put("totalShots", 3);
        row.put("shotsOnTarget", 2);
        row.put("chancesCreated", 2);
        row.put("totalPasses", 50);
        row.put("accuratePasses", 40);
        row.put("tackles", 2);
        row.put("interceptions", 1);
        row.put("blocks", 0);
        row.put("duelsWon", 5);
        row.put("duelsLost", 2);
        row.put("yellowCards", 0);
        row.put("redCards", 0);
        row.put("saves", 0);
        row.put("goalsAllowed", 0);
        row.put("isCaptain", false);
        row.put("shirtNumber", 10);
        row.put("penaltyCommitted", 0);

        when(playerStatisticRepository.getPlayerMatchesSummary("P1")).thenReturn(List.of(row));
        when(playerStatisticRepository.findByPlayerId("P1")).thenReturn(List.of());
        when(gwPointsRepository.findByTeamId(1)).thenReturn(List.of());
        when(tpgwPointsRepository.findByTeamIdAndPlayerId(1, "P1")).thenReturn(List.of());

        List<?> result = playerStatisticService.getPlayerMatchesSummary("P1", 1);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getPlayerMatchesSummary: passAccuracy calculada cuando totalPasses > 0")
    void getPlayerMatchesSummary_withPasses_passAccuracySet() {
        Map<String, Object> row = new HashMap<>();
        row.put("matchId", 1);
        row.put("round", 3);
        row.put("homeTeam", "Team A");
        row.put("awayTeam", "Team B");
        row.put("totalPasses", 50);
        row.put("accuratePasses", 40);
        row.put("fantasyPoints", 8);
        row.put("minutesPlayed", 90);

        when(playerStatisticRepository.getPlayerMatchesSummary("P1")).thenReturn(List.of(row));
        when(playerStatisticRepository.findByPlayerId("P1")).thenReturn(List.of());
        when(gwPointsRepository.findByTeamId(2)).thenReturn(List.of());
        when(tpgwPointsRepository.findByTeamIdAndPlayerId(2, "P1")).thenReturn(List.of());

        List<?> result = playerStatisticService.getPlayerMatchesSummary("P1", 2);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getPlayerMatchesSummary: chip points disponibles → fantasyPoints sobreescrito")
    void getPlayerMatchesSummary_chipPointsAvailable_fantasyPointsOverridden() {
        com.DraftLeague.models.Team.TeamPlayerGameweekPoints pgp = new com.DraftLeague.models.Team.TeamPlayerGameweekPoints();
        pgp.setMatchId(1);
        pgp.setBasePoints(15);

        Map<String, Object> row = new HashMap<>();
        row.put("matchId", 1);
        row.put("round", 4);
        row.put("homeTeam", "Team A");
        row.put("awayTeam", "Team B");
        row.put("fantasyPoints", 10); // will be overridden by chip
        row.put("minutesPlayed", 90);

        when(playerStatisticRepository.getPlayerMatchesSummary("P1")).thenReturn(List.of(row));
        when(playerStatisticRepository.findByPlayerId("P1")).thenReturn(List.of());
        when(gwPointsRepository.findByTeamId(3)).thenReturn(List.of());
        when(tpgwPointsRepository.findByTeamIdAndPlayerId(3, "P1")).thenReturn(List.of(pgp));

        List<?> result = playerStatisticService.getPlayerMatchesSummary("P1", 3);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getPlayerMatchesSummary: fila sin round → no incluida en groupingBy")
    void getPlayerMatchesSummary_rowWithoutRound_notGrouped() {
        Map<String, Object> row = new HashMap<>();
        row.put("matchId", 1);
        row.put("round", null); // no round → filtered out by groupingBy
        row.put("homeTeam", "Team A");
        row.put("awayTeam", "Team B");
        row.put("fantasyPoints", 8);

        when(playerStatisticRepository.getPlayerMatchesSummary("P1")).thenReturn(List.of(row));
        when(playerStatisticRepository.findByPlayerId("P1")).thenReturn(List.of());
        when(gwPointsRepository.findByTeamId(1)).thenReturn(List.of());
        when(tpgwPointsRepository.findByTeamIdAndPlayerId(1, "P1")).thenReturn(List.of());

        List<?> result = playerStatisticService.getPlayerMatchesSummary("P1", 1);

        // rows with null round are filtered out in groupingBy
        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // saveBulkFromJson – goalkeeper type with fixtureId (match found)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("saveBulkFromJson: portero con fixtureId y partido encontrado → cleanSheet y goalsConceded establecidos")
    void saveBulkFromJson_goalkeeperWithFixtureId_matchFound_setsCleanSheet() {
        PlayerStatistic stat = new PlayerStatistic();
        stat.setPlayerType(PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setPlayerId("GK1");

        com.DraftLeague.models.Match.Match match = new com.DraftLeague.models.Match.Match();
        match.setId(42);
        match.setHomeGoals(0);
        match.setAwayGoals(2);

        when(playerRepository.findAllIds()).thenReturn(List.of("GK1"));
        when(playerStatisticFactory.createStatistic("GOALKEEPER")).thenReturn(stat);
        when(matchRepository.findByApiFootballFixtureId(999)).thenReturn(java.util.Optional.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> row = new HashMap<>();
        row.put("playerId", "GK1");
        row.put("playerType", "GOALKEEPER");
        row.put("minutesPlayed", 90);
        row.put("goals", 0);
        row.put("assists", 0);
        row.put("yellowCards", 0);
        row.put("redCards", 0);
        row.put("isHomeTeam", true);   
        row.put("isSubstitute", false);
        row.put("isCaptain", false);
        row.put("fixtureId", 999);    
        row.put("saves", 3);
        row.put("goalsConceded", null);
        row.put("penaltiesSaved", 0);

        List<com.DraftLeague.models.Statistics.PlayerStatistic> result =
                playerStatisticService.saveBulkFromJson(List.of(row));

        assertThat(result).hasSize(1);
        assertThat(stat.getCleanSheet()).isFalse();
    }

    @Test
    @DisplayName("saveBulkFromJson: playerId como número → convertido a String")
    void saveBulkFromJson_playerIdAsNumber_convertedToString() {
        PlayerStatistic stat = new PlayerStatistic();
        stat.setPlayerType(PlayerStatistic.PlayerType.MIDFIELDER);
        stat.setPlayerId("123");

        when(playerRepository.findAllIds()).thenReturn(List.of("123"));
        when(playerStatisticFactory.createStatistic("MIDFIELDER")).thenReturn(stat);
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> row = buildJsonRow(123, "MIDFIELDER"); // integer key

        List<com.DraftLeague.models.Statistics.PlayerStatistic> result =
                playerStatisticService.saveBulkFromJson(List.of(row));

        assertThat(result).hasSize(1);
        verify(playerStatisticFactory).createStatistic("MIDFIELDER");
    }

    // -----------------------------------------------------------------------
    // getPlayerMatchStatistic – invalid playerType throws wrapped exception
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getPlayerMatchStatistic: player_type inválido → RuntimeException")
    void getPlayerMatchStatistic_invalidPlayerType_throwsRuntimeException() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        data.put("player_id", "P1");
        data.put("player_type", "INVALID_TYPE");

        when(playerStatisticRepository.findPlayerMatchStatisticData("P1", 1)).thenReturn(data);

        assertThatThrownBy(() -> playerStatisticService.getPlayerMatchStatistic("P1", 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error getting player match statistic");
    }

    private Map<String, Object> buildJsonRow(Object playerId, String playerType) {
        Map<String, Object> row = new HashMap<>();
        row.put("playerId", playerId);
        row.put("playerType", playerType);
        row.put("minutesPlayed", 90);
        row.put("goals", 0);
        row.put("assists", 0);
        row.put("yellowCards", 0);
        row.put("redCards", 0);
        row.put("isHomeTeam", true);
        row.put("isSubstitute", false);
        row.put("isCaptain", false);
        return row;
    }
}
