package com.DraftLeague.services;

import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Team.TeamGameweekPoints;
import com.DraftLeague.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FantasyPointsService Unit Tests")
class FantasyPointsServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamGameweekPointsRepository gwPointsRepository;
    @Mock private PlayerStatisticRepository statisticRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private TeamPlayerGameweekPointsRepository tpgwPointsRepository;
    @Mock private PlayerPredictionService playerPredictionService;

    @InjectMocks
    private FantasyPointsService fantasyPointsService;


    @Test
    @DisplayName("sin partidos en la jornada → gwPoints.getPoints() == 0")
    void calculateTeam_noMatches_returnsZeroPoints() {
        Team team = buildTeam(1);
        setupBasicMocks(team, 1, Collections.emptyList());

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(0);
    }

    @Test
    @DisplayName("portero 90 min + clean sheet + 6 paradas → 9 puntos (3+4+2)")
    void calculateTeam_goalkeeperCleanSheetAndSaves_correctPoints() {
        // 3 (≥60 min) + 4 (clean sheet GK) + 2 (6 saves / 3) = 9
        Player gk = buildPlayer("GK1", Position.POR);
        PlayerStatistic stat = buildStat("GK1", 101, PlayerStatistic.PlayerType.GOALKEEPER, 90, 0, 0);
        stat.setCleanSheet(true);
        stat.setSaves(6);

        Team team = buildTeam(1);
        team.setPlayerTeams(List.of(buildPT(gk, team, false, true)));
        setupWithStat(team, 1, stat);

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(9);
        assertThat(result.getGoalkeeperPoints()).isEqualTo(9);
    }

    @Test
    @DisplayName("defensa 90 min + gol + clean sheet → 13 puntos (3+6+4)")
    void calculateTeam_defenderGoalAndCleanSheet_correctPoints() {
        // 3 (min) + 6 (goal DEF) + 4 (clean sheet DEF) = 13
        Player def = buildPlayer("DEF1", Position.DEF);
        PlayerStatistic stat = buildStat("DEF1", 101, PlayerStatistic.PlayerType.DEFENDER, 90, 1, 0);
        stat.setCleanSheet(true);

        Team team = buildTeam(1);
        team.setPlayerTeams(List.of(buildPT(def, team, false, true)));
        setupWithStat(team, 1, stat);

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(13);
        assertThat(result.getDefenderPoints()).isEqualTo(13);
    }

    @Test
    @DisplayName("centrocampista 90 min + gol + asistencia → 11 puntos (3+5+3)")
    void calculateTeam_midfielderGoalAndAssist_correctPoints() {
        // 3 (min) + 5 (goal MID) + 3 (assist) = 11
        Player mid = buildPlayer("MID1", Position.MID);
        PlayerStatistic stat = buildStat("MID1", 101, PlayerStatistic.PlayerType.MIDFIELDER, 90, 1, 1);

        Team team = buildTeam(1);
        team.setPlayerTeams(List.of(buildPT(mid, team, false, true)));
        setupWithStat(team, 1, stat);

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(11);
        assertThat(result.getMidfielderPoints()).isEqualTo(11);
    }

    @Test
    @DisplayName("delantero hat-trick → bonus incluido (3 + 4×3 + 5 = 20)")
    void calculateTeam_forwardHatTrick_includesHatTrickBonus() {
        // 3 (min) + 12 (3 goals × 4) + 5 (hat-trick bonus) = 20
        Player fwd = buildPlayer("FWD1", Position.DEL);
        PlayerStatistic stat = buildStat("FWD1", 101, PlayerStatistic.PlayerType.FORWARD, 90, 3, 0);

        Team team = buildTeam(1);
        team.setPlayerTeams(List.of(buildPT(fwd, team, false, true)));
        setupWithStat(team, 1, stat);

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(20);
        assertThat(result.getForwardPoints()).isEqualTo(20);
    }

    @Test
    @DisplayName("tarjeta amarilla → resta 1 punto (3 - 1 = 2)")
    void calculateTeam_yellowCard_subtractsOnePoint() {
        Player mid = buildPlayer("MID1", Position.MID);
        PlayerStatistic stat = buildStat("MID1", 101, PlayerStatistic.PlayerType.MIDFIELDER, 90, 0, 0);
        stat.setYellowCards(1);

        Team team = buildTeam(1);
        team.setPlayerTeams(List.of(buildPT(mid, team, false, true)));
        setupWithStat(team, 1, stat);

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(2);
    }

    @Test
    @DisplayName("tarjeta roja → resultado clampeado a 0 (3 - 3 = 0)")
    void calculateTeam_redCard_pointsClampedToZero() {
        Player mid = buildPlayer("MID1", Position.MID);
        PlayerStatistic stat = buildStat("MID1", 101, PlayerStatistic.PlayerType.MIDFIELDER, 90, 0, 0);
        stat.setRedCards(1);

        Team team = buildTeam(1);
        team.setPlayerTeams(List.of(buildPT(mid, team, false, true)));
        setupWithStat(team, 1, stat);

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(0);
    }

    @Test
    @DisplayName("chip TRIPLE_CAP → capitán puntúa ×3, appliedChip guardado")
    void calculateTeam_tripleCap_captainTriples() {
        // Base points = 6, with TRIPLE_CAP captain should score 6×3 = 18
        Player fwd = buildPlayer("FWD1", Position.DEL);
        PlayerStatistic stat = buildStat("FWD1", 101, PlayerStatistic.PlayerType.FORWARD, 90, 0, 0);
        stat.setTotalFantasyPoints(6);

        Team team = buildTeam(1);
        team.setActiveChip("TRIPLE_CAP");
        team.setPlayerTeams(List.of(buildPT(fwd, team, true, true)));

        setupWithStat(team, 1, stat);
        when(teamRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(18);
        assertThat(result.getCaptainBonus()).isEqualTo(12); // 18 - 6
        assertThat(result.getAppliedChip()).isEqualTo("TRIPLE_CAP");
    }

    @Test
    @DisplayName("chip BENCH_BOOST → jugadores del banquillo suman al total y a benchPoints")
    void calculateTeam_benchBoost_benchPlayersCountInTotal() {
        Player lined   = buildPlayer("P1", Position.DEL);
        Player benched = buildPlayer("P2", Position.DEL);

        PlayerStatistic s1 = buildStat("P1", 101, PlayerStatistic.PlayerType.FORWARD, 90, 0, 0);
        s1.setTotalFantasyPoints(5);
        PlayerStatistic s2 = buildStat("P2", 101, PlayerStatistic.PlayerType.FORWARD, 90, 0, 0);
        s2.setTotalFantasyPoints(4);

        Team team = buildTeam(1);
        team.setActiveChip("BENCH_BOOST");
        team.setPlayerTeams(List.of(
            buildPT(lined,   team, false, true),
            buildPT(benched, team, false, false) // benched
        ));

        setupWithStats(team, 1, List.of(s1, s2));
        when(teamRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(9);       // 5 + 4
        assertThat(result.getBenchPoints()).isEqualTo(4);   // only bench contribution
    }

    @Test
    @DisplayName("sin chip → banquillo NO suma al total")
    void calculateTeam_noChip_benchPlayersDoNotCount() {
        Player lined   = buildPlayer("P1", Position.DEL);
        Player benched = buildPlayer("P2", Position.DEL);

        PlayerStatistic s1 = buildStat("P1", 101, PlayerStatistic.PlayerType.FORWARD, 90, 0, 0);
        s1.setTotalFantasyPoints(5);
        PlayerStatistic s2 = buildStat("P2", 101, PlayerStatistic.PlayerType.FORWARD, 90, 0, 0);
        s2.setTotalFantasyPoints(8);

        Team team = buildTeam(1); // no chip
        team.setPlayerTeams(List.of(
            buildPT(lined,   team, false, true),
            buildPT(benched, team, false, false)
        ));

        setupWithStats(team, 1, List.of(s1, s2));

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(5);       // only lined player
        assertThat(result.getBenchPoints()).isEqualTo(0);
    }

    @Test
    @DisplayName("jugador alineado sin estadística → contribuye 0 puntos")
    void calculateTeam_playerWithNoStat_contributesZero() {
        Player p = buildPlayer("P1", Position.MID);

        Team team = buildTeam(1);
        team.setPlayerTeams(List.of(buildPT(p, team, false, true)));

        setupBasicMocks(team, 1, Collections.emptyList()); // no stats for any match

        TeamGameweekPoints result = fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(result.getPoints()).isEqualTo(0);
    }

    @Test
    @DisplayName("chip consumido → activeChip=null y usedChips actualizado")
    void calculateTeam_chipConsumed_teamStateUpdated() {
        Player p = buildPlayer("P1", Position.MID);
        PlayerStatistic stat = buildStat("P1", 101, PlayerStatistic.PlayerType.MIDFIELDER, 90, 0, 0);
        stat.setTotalFantasyPoints(3);

        Team team = buildTeam(1);
        team.setActiveChip("TRIPLE_CAP");
        team.setUsedChips(null);
        team.setPlayerTeams(List.of(buildPT(p, team, false, true)));

        setupWithStat(team, 1, stat);
        when(teamRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fantasyPointsService.calculateTeamPointsForGameweek(1, 1);

        assertThat(team.getActiveChip()).isNull();
        assertThat(team.getUsedChips()).isEqualTo("TRIPLE_CAP");
        verify(teamRepository).save(team);
    }


    @Test
    @DisplayName("updateTeamTotalPoints: suma todos los gameweek points del equipo")
    void updateTeamTotalPoints_sumsAllGameweekPoints() {
        Team team = buildTeam(1);
        TeamGameweekPoints gw1 = new TeamGameweekPoints(); gw1.setPoints(10);
        TeamGameweekPoints gw2 = new TeamGameweekPoints(); gw2.setPoints(15);
        TeamGameweekPoints gw3 = new TeamGameweekPoints(); gw3.setPoints(20);

        when(teamRepository.findById(1)).thenReturn(Optional.of(team));
        when(gwPointsRepository.findByTeamOrderByGameweekAsc(team)).thenReturn(List.of(gw1, gw2, gw3));
        when(teamRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fantasyPointsService.updateTeamTotalPoints(1);

        assertThat(team.getTotalPoints()).isEqualTo(45);
        verify(teamRepository).save(team);
    }


    @Test
    @DisplayName("updatePlayerTotalPoints: suma totalFantasyPoints de todas las estadísticas")
    void updatePlayerTotalPoints_sumsTotalFantasyPoints() {
        Player player = buildPlayer("P1", Position.MID);
        PlayerStatistic s1 = new PlayerStatistic(); s1.setTotalFantasyPoints(8);
        PlayerStatistic s2 = new PlayerStatistic(); s2.setTotalFantasyPoints(6);
        PlayerStatistic s3 = new PlayerStatistic(); s3.setTotalFantasyPoints(12);

        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(statisticRepository.findByPlayerId("P1")).thenReturn(List.of(s1, s2, s3));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fantasyPointsService.updatePlayerTotalPoints("P1");

        assertThat(player.getTotalPoints()).isEqualTo(26);
        verify(playerRepository).save(player);
    }


    /** Sets up mocks for a team with a match and pre-set stats list. */
    private void setupBasicMocks(Team team, int gameweek, List<PlayerStatistic> stats) {
        Match match = buildMatch(101, gameweek);
        when(matchRepository.findByRound(gameweek)).thenReturn(List.of(match));
        when(statisticRepository.findByMatchIdIn(any())).thenReturn(stats);
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(gwPointsRepository.findByTeamAndGameweek(team, gameweek)).thenReturn(Optional.empty());
        when(tpgwPointsRepository.findByTeamAndGameweek(team, gameweek)).thenReturn(Collections.emptyList());
        when(gwPointsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    /** Sets up mocks with a single stat. The match ID is taken from the stat. */
    private void setupWithStat(Team team, int gameweek, PlayerStatistic stat) {
        setupWithStats(team, gameweek, List.of(stat));
    }

    /** Sets up mocks with a list of stats. */
    private void setupWithStats(Team team, int gameweek, List<PlayerStatistic> stats) {
        Match match = buildMatch(101, gameweek);
        when(matchRepository.findByRound(gameweek)).thenReturn(List.of(match));
        when(statisticRepository.findByMatchIdIn(any())).thenReturn(stats);
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(gwPointsRepository.findByTeamAndGameweek(team, gameweek)).thenReturn(Optional.empty());
        when(tpgwPointsRepository.findByTeamAndGameweek(team, gameweek)).thenReturn(Collections.emptyList());
        when(gwPointsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Team buildTeam(int id) {
        Team t = new Team();
        t.setId(id);
        t.setUser(buildUser());
        t.setBudget(5_000_000);
        t.setTotalPoints(0);
        t.setWildcardUsed(false);
        t.setCreatedAt(new Date(System.currentTimeMillis() - 86_400_000L));
        t.setUsedChips("");
        t.setPlayerTeams(new ArrayList<>());
        return t;
    }

    private com.DraftLeague.models.user.User buildUser() {
        com.DraftLeague.models.user.User u = new com.DraftLeague.models.user.User();
        u.setId(1);
        u.setUsername("alice");
        u.setEmail("alice@test.com");
        u.setPassword("encoded");
        u.setDisplayName("Alice");
        u.setRole("USER");
        return u;
    }

    private Player buildPlayer(String id, Position position) {
        Player p = new Player();
        p.setId(id);
        p.setFullName("Player " + id);
        p.setPosition(position);
        p.setMarketValue(5_000_000);
        p.setActive(true);
        p.setTotalPoints(0);
        p.setClubId(541);
        return p;
    }

    private PlayerTeam buildPT(Player player, Team team, boolean isCaptain, boolean lined) {
        PlayerTeam pt = new PlayerTeam();
        pt.setPlayer(player);
        pt.setTeam(team);
        pt.setIsCaptain(isCaptain);
        pt.setLined(lined);
        pt.setSellPrice(player.getMarketValue());
        return pt;
    }

    /**
     * Builds a PlayerStatistic with the minimal fields used by calculateFantasyPoints().
     * totalFantasyPoints is left null so calculateFantasyPoints() is called.
     */
    private PlayerStatistic buildStat(String playerId, int matchId,
                                       PlayerStatistic.PlayerType type,
                                       int minutes, int goals, int assists) {
        PlayerStatistic s = new PlayerStatistic();
        s.setPlayerId(playerId);
        s.setMatchId(matchId);
        s.setPlayerType(type);
        s.setMinutesPlayed(minutes);
        s.setGoals(goals);
        s.setAssists(assists);
        s.setYellowCards(0);
        s.setRedCards(0);
        s.setIsHomeTeam(true);
        s.setTotalFantasyPoints(null); // force calculateFantasyPoints() path (field defaults to 0)
        return s;
    }

    private Match buildMatch(int id, int round) {
        Match m = new Match();
        m.setId(id);
        return m;
    }
}
