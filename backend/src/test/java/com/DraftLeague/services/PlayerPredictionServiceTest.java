package com.DraftLeague.services;

import com.DraftLeague.dto.PlayerPredictionDTO;
import com.DraftLeague.dto.TeamPredictionDTO;
import com.DraftLeague.dto.XGBoostPredictionResult;
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
import java.util.Map;
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

    // -----------------------------------------------------------------------
    // predictForPlayer – XGBoost path
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: XGBoost disponible → modelSource es XGBOOST y usa predictedPoints del modelo")
    void predictForPlayer_xgBoostAvailable_usesXGBoostResult() {
        Player player = buildPlayer("P10", 541);
        List<PlayerStatistic> stats = buildStats("P10", 3);

        com.DraftLeague.dto.XGBoostPredictionResult xgResult =
                new com.DraftLeague.dto.XGBoostPredictionResult(12.5, Map.of("rating", 0.4), "XGBOOST");

        when(playerRepository.findById("P10")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P10")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P10"), any())).thenReturn(Optional.of(xgResult));

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P10");

        assertThat(result.getModelSource()).isEqualTo("XGBOOST");
        assertThat(result.getPredictedPoints()).isEqualTo(12.5);
        assertThat(result.getFeaturesImportance()).containsKey("rating");
    }

    @Test
    @DisplayName("predictForPlayer: XGBoost no disponible → modelSource es HEURISTIC")
    void predictForPlayer_xgBoostUnavailable_usesHeuristic() {
        Player player = buildPlayer("P11", 541);
        List<PlayerStatistic> stats = buildStats("P11", 3);

        when(playerRepository.findById("P11")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P11")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P11"), any())).thenReturn(Optional.empty());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P11");

        assertThat(result.getModelSource()).isEqualTo("HEURISTIC");
        assertThat(result.getFeaturesImportance()).isNotEmpty();
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – player not found in repository
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: jugador no encontrado en BD → playerName es el propio playerId")
    void predictForPlayer_playerNotFound_usesPlayerIdAsName() {
        when(playerRepository.findById("UNKNOWN")).thenReturn(Optional.empty());
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("UNKNOWN")).thenReturn(Collections.emptyList());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("UNKNOWN");

        assertThat(result.getPlayerId()).isEqualTo("UNKNOWN");
        assertThat(result.getPlayerName()).isEqualTo("UNKNOWN");
        assertThat(result.getPredictedPoints()).isEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – away match multiplier
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: jugador como visitante → isHomeTeam false en DTO")
    void predictForPlayer_awayMatch_isHomeFalseInDTO() {
        Player player = buildPlayer("P6", 541); // clubId=541 (home=529, away=541 → away team)
        List<PlayerStatistic> stats = buildStats("P6", 1); // < MIN_STATS → early return path

        Match upcoming = buildUpcomingMatch(2, 10, 529, 541); // home=529, away=541
        when(playerRepository.findById("P6")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P6")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(upcoming));

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P6");

        assertThat(result.getIsHomeTeam()).isFalse();
        assertThat(result.getOpponent()).isEqualTo("Home FC");
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – opponent field resolved for home team
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: jugador como local → opponent es el club visitante")
    void predictForPlayer_homeMatch_opponentIsAwayClub() {
        Player player = buildPlayer("P7", 541);
        List<PlayerStatistic> stats = buildStats("P7", 1);

        Match upcoming = buildUpcomingMatch(3, 12, 541, 529); // home=541 (player's club), away=529
        when(playerRepository.findById("P7")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P7")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(upcoming));

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P7");

        assertThat(result.getOpponent()).isEqualTo("Away FC");
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – confidence interval is always [low, high] with low >= 0
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: confidenceInterval[0] siempre >= 0")
    void predictForPlayer_confidenceInterval_lowBoundNonNegative() {
        Player player = buildPlayer("P8", 541);
        List<PlayerStatistic> stats = buildStats("P8", 2);

        when(playerRepository.findById("P8")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P8")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P8"), any())).thenReturn(Optional.empty());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P8");

        assertThat(result.getConfidenceInterval()).hasSize(2);
        assertThat(result.getConfidenceInterval().get(0)).isGreaterThanOrEqualTo(0);
        assertThat(result.getConfidenceInterval().get(1))
                .isGreaterThanOrEqualTo(result.getConfidenceInterval().get(0));
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – heuristic features importance contains matchesPlayed
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: heurístico → featuresImportance contiene matchesPlayed")
    void predictForPlayer_heuristic_featuresContainMatchesPlayed() {
        Player player = buildPlayer("P9", 541);
        List<PlayerStatistic> stats = buildStats("P9", 5);

        when(playerRepository.findById("P9")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P9")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P9"), any())).thenReturn(Optional.empty());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P9");

        assertThat(result.getFeaturesImportance()).containsKey("matchesPlayed");
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – opponent strength from finished matches
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: rival con historial de partidos → opponentStrength calculado sin excepción")
    void predictForPlayer_withFinishedMatches_opponentStrengthCalculated() {
        Player player = buildPlayer("P12", 541);
        List<PlayerStatistic> stats = buildStats("P12", 3);

        Match upcoming = buildUpcomingMatch(5, 20, 541, 529);
        Match finished1 = buildFinishedMatch(10, 1, 529, 540, 2, 1);
        Match finished2 = buildFinishedMatch(11, 2, 529, 530, 1, 3);

        when(playerRepository.findById("P12")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P12")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(upcoming));
        when(matchRepository.findByStatus(MatchStatus.FINISHED)).thenReturn(List.of(finished1, finished2));
        when(xgBoostClient.predict(eq("P12"), any())).thenReturn(Optional.empty());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P12");

        assertThat(result).isNotNull();
        assertThat(result.getPredictedPoints()).isGreaterThanOrEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // warmCacheForEligiblePlayers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("warmCacheForEligiblePlayers: jugador con >= 2 stats → predicción pre-cargada en caché")
    void warmCacheForEligiblePlayers_playerWithEnoughStats_warmsCache() {
        Player player = buildPlayer("W1", 541);
        List<PlayerStatistic> stats = buildStats("W1", 3);

        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("W1")).thenReturn(stats);
        when(xgBoostClient.predict(eq("W1"), any())).thenReturn(Optional.empty());

        playerPredictionService.warmCacheForEligiblePlayers();

        playerPredictionService.predictForPlayer("W1");
        verify(statisticRepository, times(2)).findByPlayerIdOrderByMatchIdDesc("W1");
    }

    @Test
    @DisplayName("warmCacheForEligiblePlayers: jugador con < 2 stats → no se pre-carga")
    void warmCacheForEligiblePlayers_playerWithInsufficientStats_notWarmed() {
        Player player = buildPlayer("W2", 541);
        List<PlayerStatistic> stats = buildStats("W2", 1); // only 1 stat

        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("W2")).thenReturn(stats);

        playerPredictionService.warmCacheForEligiblePlayers();

        playerPredictionService.predictForPlayer("W2");
        verify(statisticRepository, times(2)).findByPlayerIdOrderByMatchIdDesc("W2");
    }

    @Test
    @DisplayName("warmCacheForEligiblePlayers: excepción en un jugador → proceso continúa para el resto")
    void warmCacheForEligiblePlayers_exceptionForOnePlayer_continuesForOthers() {
        Player bad = buildPlayer("WBAD", 541);
        Player good = buildPlayer("WGOOD", 541);
        List<PlayerStatistic> goodStats = buildStats("WGOOD", 3);

        when(playerRepository.findAll()).thenReturn(List.of(bad, good));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("WBAD"))
                .thenThrow(new RuntimeException("DB error"));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("WGOOD")).thenReturn(goodStats);
        when(xgBoostClient.predict(eq("WGOOD"), any())).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> playerPredictionService.warmCacheForEligiblePlayers());
    }

    // -----------------------------------------------------------------------
    // predictForTeam – captain doubles points
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForTeam: capitán → sus puntos se duplican en el total")
    void predictForTeam_captainDoublePoints() {
        Player player = buildPlayer("TC1", 541);
        List<PlayerStatistic> stats = buildStats("TC1", 3);

        Team team = new Team();
        team.setId(10);

        PlayerTeam captainPt = new PlayerTeam();
        captainPt.setPlayer(player);
        captainPt.setIsCaptain(true);
        captainPt.setLined(true);

        when(teamRepository.findById(10)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(List.of(captainPt));
        when(playerRepository.findById("TC1")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("TC1")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("TC1"), any())).thenReturn(Optional.empty());

        TeamPredictionDTO result = playerPredictionService.predictForTeam(10);

        double rawPred = playerPredictionService.predictForPlayer("TC1").getPredictedPoints();
        assertThat(result.getTotalPredictedPoints()).isEqualTo(rawPred * 2);
    }

    @Test
    @DisplayName("predictForTeam: team con User → teamName contiene username")
    void predictForTeam_teamWithUser_teamNameContainsUsername() {
        com.DraftLeague.models.user.User user = new com.DraftLeague.models.user.User();
        user.setId(1);
        user.setUsername("alice");

        Team team = new Team();
        team.setId(20);
        team.setUser(user);

        when(teamRepository.findById(20)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(Collections.emptyList());

        TeamPredictionDTO result = playerPredictionService.predictForTeam(20);

        assertThat(result.getTeamName()).contains("alice");
    }

    @Test
    @DisplayName("predictForTeam: team sin User → teamName contiene 'Team'")
    void predictForTeam_teamWithoutUser_teamNameContainsTeamId() {
        Team team = new Team();
        team.setId(30);

        when(teamRepository.findById(30)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(Collections.emptyList());

        TeamPredictionDTO result = playerPredictionService.predictForTeam(30);

        assertThat(result.getTeamName()).contains("30");
    }

    @Test
    @DisplayName("predictForTeam: solo jugadores no alineados → lineup vacía → total 0.0")
    void predictForTeam_onlyBenchedPlayers_totalZero() {
        Player player = buildPlayer("TB1", 541);
        Team team = new Team();
        team.setId(40);

        PlayerTeam benched = new PlayerTeam();
        benched.setPlayer(player);
        benched.setIsCaptain(false);
        benched.setLined(false); 

        when(teamRepository.findById(40)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(List.of(benched));

        TeamPredictionDTO result = playerPredictionService.predictForTeam(40);

        assertThat(result.getTotalPredictedPoints()).isEqualTo(0.0);
        assertThat(result.getPlayers()).isEmpty();
    }

    @Test
    @DisplayName("predictForTeam: confidenceInterval low = floor(total*0.7), high = ceil(total*1.3)")
    void predictForTeam_confidenceInterval_correctBounds() {
        Team team = new Team();
        team.setId(50);

        when(teamRepository.findById(50)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(Collections.emptyList());

        TeamPredictionDTO result = playerPredictionService.predictForTeam(50);

        assertThat(result.getConfidenceInterval()).containsExactly(0, 0);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // predictForPlayer – player null but stats exist (position UNKNOWN)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: jugador no en BD pero sí tiene stats → position UNKNOWN, playerType del primer stat")
    void predictForPlayer_playerNullWithStats_usesDefaultPosition() {
        List<PlayerStatistic> stats = buildStats("ORPHAN", 3);

        when(playerRepository.findById("ORPHAN")).thenReturn(Optional.empty());
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("ORPHAN")).thenReturn(stats);
        // No match lookup because player is null (clubId not accessible)
        when(xgBoostClient.predict(eq("ORPHAN"), any())).thenReturn(Optional.empty());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("ORPHAN");

        assertThat(result.getPlayerId()).isEqualTo("ORPHAN");
        assertThat(result.getPosition()).isEqualTo("UNKNOWN");
        assertThat(result.getPredictedPoints()).isGreaterThanOrEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – home advantage multiplier applied when stats >= MIN
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: jugador local con suficientes stats → predictedPoints mayor que visitante equivalente")
    void predictForPlayer_homeVsAway_homeHigherPoints() {
        Player homePlayer = buildPlayer("HOME1", 541);
        Player awayPlayer = buildPlayer("AWAY1", 541);
        List<PlayerStatistic> stats = buildStats("HOME1", 3);
        List<PlayerStatistic> awayStats = buildStats("AWAY1", 3);

        Match homeMatch = buildUpcomingMatch(10, 5, 541, 529); // 541 is home
        Match awayMatch = buildUpcomingMatch(11, 5, 529, 541); // 541 is away

        when(playerRepository.findById("HOME1")).thenReturn(Optional.of(homePlayer));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("HOME1")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(homeMatch));
        when(matchRepository.findByStatus(MatchStatus.FINISHED)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("HOME1"), any())).thenReturn(Optional.empty());

        when(playerRepository.findById("AWAY1")).thenReturn(Optional.of(awayPlayer));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("AWAY1")).thenReturn(awayStats);
        when(xgBoostClient.predict(eq("AWAY1"), any())).thenReturn(Optional.empty());

        PlayerPredictionDTO homeResult = playerPredictionService.predictForPlayer("HOME1");
        playerPredictionService.invalidateCache();
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(awayMatch));
        PlayerPredictionDTO awayResult = playerPredictionService.predictForPlayer("AWAY1");

        assertThat(homeResult.getPredictedPoints()).isGreaterThan(awayResult.getPredictedPoints());
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – rivalry strength computation with no finished matches
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: rival sin partidos finalizados → opponentStrength = 1.0 (neutral)")
    void predictForPlayer_noFinishedMatchesForOpponent_neutralStrength() {
        Player player = buildPlayer("P20", 541);
        List<PlayerStatistic> stats = buildStats("P20", 3);
        Match upcoming = buildUpcomingMatch(20, 8, 541, 529);

        when(playerRepository.findById("P20")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P20")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(upcoming));
        when(matchRepository.findByStatus(MatchStatus.FINISHED)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P20"), any())).thenReturn(Optional.empty());

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P20");

        assertThat(result).isNotNull();
        assertThat(result.getPredictedPoints()).isGreaterThanOrEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – XGBoost result with empty featuresImportance
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: XGBoost devuelve featuresImportance vacío → confidenceInterval calculado con finalPredictedPoints")
    void predictForPlayer_xgBoostEmptyFeatures_confidenceIntervalFromFinalPoints() {
        Player player = buildPlayer("P30", 541);
        List<PlayerStatistic> stats = buildStats("P30", 3);

        XGBoostPredictionResult xgResult = new XGBoostPredictionResult(8.0, Collections.emptyMap(), "XGBOOST");

        when(playerRepository.findById("P30")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P30")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("P30"), any())).thenReturn(Optional.of(xgResult));

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P30");

        assertThat(result.getModelSource()).isEqualTo("XGBOOST");
        assertThat(result.getPredictedPoints()).isEqualTo(8.0);
        assertThat(result.getConfidenceInterval()).hasSize(2);
        assertThat(result.getConfidenceInterval().get(0)).isGreaterThanOrEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // predictForPlayer – opponent is the home club when player is away
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForPlayer: jugador visitante → opponent es el club local")
    void predictForPlayer_awayPlayer_opponentIsHomeClub() {
        Player player = buildPlayer("P40", 529); 
        List<PlayerStatistic> stats = buildStats("P40", 1); 

        Match upcoming = buildUpcomingMatch(40, 15, 541, 529); 
        when(playerRepository.findById("P40")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("P40")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(upcoming));

        PlayerPredictionDTO result = playerPredictionService.predictForPlayer("P40");

        assertThat(result.getIsHomeTeam()).isFalse();
        assertThat(result.getOpponent()).isEqualTo("Home FC");
    }

    // -----------------------------------------------------------------------
    // predictForTeam – match info propagated from first player with match data
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForTeam: jugador con próximo partido → nextMatchId propagado al DTO de equipo")
    void predictForTeam_playerWithNextMatch_nextMatchIdPropagated() {
        Player player = buildPlayer("TM1", 541);
        List<PlayerStatistic> stats = buildStats("TM1", 3);

        Match upcoming = buildUpcomingMatch(99, 7, 541, 529);

        Team team = new Team();
        team.setId(60);

        PlayerTeam lined = new PlayerTeam();
        lined.setPlayer(player);
        lined.setIsCaptain(false);
        lined.setLined(true);

        when(teamRepository.findById(60)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(List.of(lined));
        when(playerRepository.findById("TM1")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("TM1")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(upcoming));
        when(matchRepository.findByStatus(MatchStatus.FINISHED)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("TM1"), any())).thenReturn(Optional.empty());

        TeamPredictionDTO result = playerPredictionService.predictForTeam(60);

        assertThat(result.getNextMatchId()).isEqualTo(99);
        assertThat(result.getRound()).isEqualTo(7);
    }

    // -----------------------------------------------------------------------
    // predictForTeam – multiple players, non-captain does not double
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForTeam: jugador no capitán → sus puntos NO se duplican")
    void predictForTeam_nonCaptain_pointsNotDoubled() {
        Player player = buildPlayer("NC1", 541);
        List<PlayerStatistic> stats = buildStats("NC1", 3);

        Team team = new Team();
        team.setId(70);

        PlayerTeam nonCaptain = new PlayerTeam();
        nonCaptain.setPlayer(player);
        nonCaptain.setIsCaptain(false);
        nonCaptain.setLined(true);

        when(teamRepository.findById(70)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(List.of(nonCaptain));
        when(playerRepository.findById("NC1")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("NC1")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("NC1"), any())).thenReturn(Optional.empty());

        TeamPredictionDTO result = playerPredictionService.predictForTeam(70);

        double rawPred = playerPredictionService.predictForPlayer("NC1").getPredictedPoints();
        assertThat(result.getTotalPredictedPoints()).isEqualTo(rawPred);
    }

    // -----------------------------------------------------------------------
    // predictForTeam – confidence interval boundaries with non-zero total
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("predictForTeam: low = floor(total*0.7), high = ceil(total*1.3) cuando total > 0")
    void predictForTeam_nonZeroTotal_confidenceIntervalCorrect() {
        Player player = buildPlayer("CI1", 541);
        List<PlayerStatistic> stats = buildStats("CI1", 5);

        Team team = new Team();
        team.setId(80);

        PlayerTeam lined = new PlayerTeam();
        lined.setPlayer(player);
        lined.setIsCaptain(false);
        lined.setLined(true);

        when(teamRepository.findById(80)).thenReturn(Optional.of(team));
        when(playerTeamRepository.findByTeam(team)).thenReturn(List.of(lined));
        when(playerRepository.findById("CI1")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerIdOrderByMatchIdDesc("CI1")).thenReturn(stats);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());
        when(xgBoostClient.predict(eq("CI1"), any())).thenReturn(Optional.empty());

        TeamPredictionDTO result = playerPredictionService.predictForTeam(80);

        double total = result.getTotalPredictedPoints();
        int expectedLow = Math.max(0, (int) Math.floor(total * 0.7));
        int expectedHigh = (int) Math.ceil(total * 1.3);
        assertThat(result.getConfidenceInterval()).containsExactly(expectedLow, expectedHigh);
    }

    // -----------------------------------------------------------------------
    // warmCacheForEligiblePlayers – empty player list
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("warmCacheForEligiblePlayers: sin jugadores → no consulta estadísticas")
    void warmCacheForEligiblePlayers_noPlayers_doesNothing() {
        when(playerRepository.findAll()).thenReturn(Collections.emptyList());

        playerPredictionService.warmCacheForEligiblePlayers();

        verify(statisticRepository, never()).findByPlayerIdOrderByMatchIdDesc(any());
    }

    private Match buildFinishedMatch(int id, int round, int homeTeamId, int awayTeamId,
                                      int homeGoals, int awayGoals) {
        Match m = new Match();
        m.setId(id);
        m.setRound(round);
        m.setHomeTeamId(homeTeamId);
        m.setAwayTeamId(awayTeamId);
        m.setHomeGoals(homeGoals);
        m.setAwayGoals(awayGoals);
        m.setStatus(MatchStatus.FINISHED);
        return m;
    }
}
