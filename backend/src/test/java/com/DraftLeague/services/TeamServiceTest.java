package com.DraftLeague.services;

import com.DraftLeague.dto.CreateTeamRequest;
import com.DraftLeague.dto.UpdateTeamPlayersRequest;
import com.DraftLeague.dto.UpdateTeamRequest;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService Unit Tests")
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlayerService playerService;
    @Mock private NotificationService notificationService;
    @Mock private LeagueRepository leagueRepository;
    @Mock private GameweekStateService gameweekStateService;

    @InjectMocks
    private TeamService teamService;


    @Test
    @DisplayName("getTeamByUserAndLeague: equipo existente → devuelve el equipo")
    void getTeamByUserAndLeague_exists_returnsTeam() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);

        Team result = teamService.getTeamByUserAndLeague(1, 1);

        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getTeamByUserAndLeague: usuario no tiene equipo en la liga → RuntimeException")
    void getTeamByUserAndLeague_noTeam_throwsRuntimeException() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(null);

        assertThatThrownBy(() -> teamService.getTeamByUserAndLeague(1, 1))
                .isInstanceOf(RuntimeException.class);
    }


    @Test
    @DisplayName("updateTeamPlayers: equipos bloqueados → RuntimeException")
    void updateTeamPlayers_teamsLocked_throwsRuntimeException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(true);

        assertThatThrownBy(() -> teamService.updateTeamPlayers(1, 1, new UpdateTeamPlayersRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bloqueadas");
    }

    @Test
    @DisplayName("updateTeamPlayers: lista de jugadores válida → persiste cambios")
    void updateTeamPlayers_validPlayers_persistsChanges() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setPlayerTeams(new ArrayList<>());
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        UpdateTeamPlayersRequest.PlayerSelection selection = new UpdateTeamPlayersRequest.PlayerSelection();
        selection.setPlayerId("P1");
        selection.setLined(true);
        selection.setIsCaptain(false);

        UpdateTeamPlayersRequest request = new UpdateTeamPlayersRequest();
        request.setPlayers(List.of(selection));

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(playerService.getPlayerById("P1")).thenReturn(player);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Team result = teamService.updateTeamPlayers(1, 1, request);

        assertThat(result.getPlayerTeams()).hasSize(1);
        verify(teamRepository).save(team);
    }


    @Test
    @DisplayName("getTeamById: id existente → devuelve el equipo")
    void getTeamById_exists_returnsTeam() {
        Team team = buildTeam(1, buildUser(1, "alice"), buildLeague(1));
        when(teamRepository.findById(1)).thenReturn(Optional.of(team));

        Team result = teamService.getTeamById(1);

        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getTeamById: id inexistente → RuntimeException")
    void getTeamById_notFound_throwsRuntimeException() {
        when(teamRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamById(99))
                .isInstanceOf(RuntimeException.class);
    }


    // -------------------------------------------------------------------------
    // postTeam
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("postTeam: usuario y liga existen → crea y persiste el equipo")
    void postTeam_validRequest_savesTeam() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);

        CreateTeamRequest req = new CreateTeamRequest();
        req.setUserId(1);
        req.setLeagueId(1L);
        req.setBudget(10_000_000);
        req.setWildcardUsed(false);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            t.setId(99);
            return t;
        });

        Team result = teamService.postTeam(req);

        assertThat(result.getId()).isEqualTo(99);
        assertThat(result.getBudget()).isEqualTo(10_000_000);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getLeague()).isEqualTo(league);
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("postTeam: usuario no existe → RuntimeException")
    void postTeam_userNotFound_throwsRuntimeException() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setUserId(99);
        req.setLeagueId(1L);
        req.setBudget(10_000_000);
        req.setWildcardUsed(false);

        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.postTeam(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("postTeam: liga no existe → RuntimeException")
    void postTeam_leagueNotFound_throwsRuntimeException() {
        User user = buildUser(1, "alice");

        CreateTeamRequest req = new CreateTeamRequest();
        req.setUserId(1);
        req.setLeagueId(99L);
        req.setBudget(10_000_000);
        req.setWildcardUsed(false);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.postTeam(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("League not found");
    }

    // -------------------------------------------------------------------------
    // updateTeam
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateTeam: sólo presupuesto actualizado → persiste cambio")
    void updateTeam_budgetOnly_updatesBudget() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);

        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setBudget(8_000_000);

        when(teamRepository.findById(1)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        Team result = teamService.updateTeam(req, 1);

        assertThat(result.getBudget()).isEqualTo(8_000_000);
        verify(teamRepository).save(team);
    }

    @Test
    @DisplayName("updateTeam: todos los campos → actualiza todos")
    void updateTeam_allFields_updatesAll() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);

        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setBudget(7_000_000);
        req.setGameweekPoints(42);
        req.setTotalPoints(150);
        req.setCaptainId(5);

        when(teamRepository.findById(1)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        Team result = teamService.updateTeam(req, 1);

        assertThat(result.getBudget()).isEqualTo(7_000_000);
        assertThat(result.getGameweekPoints()).isEqualTo(42);
        assertThat(result.getTotalPoints()).isEqualTo(150);
        assertThat(result.getCaptainId()).isEqualTo(5);
    }

    @Test
    @DisplayName("updateTeam: equipo no existe → RuntimeException")
    void updateTeam_teamNotFound_throwsRuntimeException() {
        when(teamRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.updateTeam(new UpdateTeamRequest(), 99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Team not found");
    }

    // -------------------------------------------------------------------------
    // deleteTeam
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteTeam: equipo existe → lo elimina del repositorio")
    void deleteTeam_exists_deletesTeam() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);

        when(teamRepository.findById(1)).thenReturn(Optional.of(team));

        teamService.deleteTeam(1);

        verify(teamRepository).delete(team);
    }

    @Test
    @DisplayName("deleteTeam: equipo no existe → RuntimeException")
    void deleteTeam_notFound_throwsRuntimeException() {
        when(teamRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.deleteTeam(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Team not found");
    }

    // -------------------------------------------------------------------------
    // getAllTeams
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAllTeams: repositorio devuelve lista → la retorna")
    void getAllTeams_returnsList() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        List<Team> teams = List.of(buildTeam(1, user, league), buildTeam(2, user, league));

        when(teamRepository.findAll()).thenReturn(teams);

        List<Team> result = teamService.getAllTeams();

        assertThat(result).hasSize(2);
        verify(teamRepository).findAll();
    }

    @Test
    @DisplayName("getAllTeams: sin equipos → lista vacía")
    void getAllTeams_empty_returnsEmptyList() {
        when(teamRepository.findAll()).thenReturn(List.of());

        List<Team> result = teamService.getAllTeams();

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // resetGameweekPoints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resetGameweekPoints: pone gameweekPoints a 0 en todos los equipos")
    void resetGameweekPoints_setsAllPointsToZero() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team t1 = buildTeam(1, user, league);
        t1.setGameweekPoints(10);
        Team t2 = buildTeam(2, user, league);
        t2.setGameweekPoints(20);

        when(teamRepository.findAll()).thenReturn(new ArrayList<>(List.of(t1, t2)));
        when(teamRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        teamService.resetGameweekPoints();

        assertThat(t1.getGameweekPoints()).isZero();
        assertThat(t2.getGameweekPoints()).isZero();
        verify(teamRepository).saveAll(any());
    }

    @Test
    @DisplayName("resetGameweekPoints: sin equipos → no lanza excepción")
    void resetGameweekPoints_noTeams_doesNotThrow() {
        when(teamRepository.findAll()).thenReturn(new ArrayList<>());
        when(teamRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        teamService.resetGameweekPoints();

        verify(teamRepository).saveAll(any());
    }

    // -------------------------------------------------------------------------
    // getTeamByUserId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getTeamByUserId: el usuario tiene un equipo → lo devuelve")
    void getTeamByUserId_found_returnsTeam() {
        User user = buildUser(5, "bob");
        League league = buildLeague(1);
        Team team = buildTeam(3, user, league);

        when(teamRepository.findAll()).thenReturn(List.of(team));

        Team result = teamService.getTeamByUserId(5);

        assertThat(result.getId()).isEqualTo(3);
    }

    @Test
    @DisplayName("getTeamByUserId: el usuario no tiene equipo → RuntimeException")
    void getTeamByUserId_notFound_throwsRuntimeException() {
        when(teamRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> teamService.getTeamByUserId(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Team not found for user");
    }

    // -------------------------------------------------------------------------
    // activateChip
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("activateChip: equipos bloqueados → RuntimeException")
    void activateChip_teamsLocked_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(true);

        assertThatThrownBy(() -> teamService.activateChip(1, 1, "TRIPLE_CAP"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bloqueados");
    }

    @Test
    @DisplayName("activateChip: chip inválido → RuntimeException")
    void activateChip_invalidChip_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(false);

        assertThatThrownBy(() -> teamService.activateChip(1, 1, "INVALID_CHIP"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Chip inválido");
    }

    @Test
    @DisplayName("activateChip: ya tiene chip activo → RuntimeException")
    void activateChip_alreadyActive_throwsException() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setActiveChip("BENCH_BOOST");

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);

        assertThatThrownBy(() -> teamService.activateChip(1, 1, "TRIPLE_CAP"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chip activo");
    }

    @Test
    @DisplayName("activateChip: chip ya usado esta temporada → RuntimeException")
    void activateChip_alreadyUsedThisSeason_throwsException() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setActiveChip(null);
        team.setUsedChips("TRIPLE_CAP,BENCH_BOOST");

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);

        assertThatThrownBy(() -> teamService.activateChip(1, 1, "TRIPLE_CAP"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya fue usado");
    }

    @Test
    @DisplayName("activateChip: chip válido y no usado → activa el chip")
    void activateChip_valid_setsActiveChip() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setActiveChip(null);
        team.setUsedChips("");

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        Team result = teamService.activateChip(1, 1, "TRIPLE_CAP");

        assertThat(result.getActiveChip()).isEqualTo("TRIPLE_CAP");
        verify(teamRepository).save(team);
    }

    @Test
    @DisplayName("activateChip: usedChips null → activa el chip sin NPE")
    void activateChip_nullUsedChips_setsActiveChip() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setActiveChip(null);
        team.setUsedChips(null);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        Team result = teamService.activateChip(1, 1, "BENCH_BOOST");

        assertThat(result.getActiveChip()).isEqualTo("BENCH_BOOST");
    }

    // -------------------------------------------------------------------------
    // cancelChip
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cancelChip: equipos bloqueados → RuntimeException")
    void cancelChip_teamsLocked_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(true);

        assertThatThrownBy(() -> teamService.cancelChip(1, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bloqueados");
    }

    @Test
    @DisplayName("cancelChip: no hay chip activo → RuntimeException")
    void cancelChip_noActiveChip_throwsException() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setActiveChip(null);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);

        assertThatThrownBy(() -> teamService.cancelChip(1, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay ningún chip activo");
    }

    @Test
    @DisplayName("cancelChip: chip activo → lo cancela y persiste")
    void cancelChip_activeChip_clearsIt() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setActiveChip("BENCH_BOOST");

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        Team result = teamService.cancelChip(1, 1);

        assertThat(result.getActiveChip()).isNull();
        verify(teamRepository).save(team);
    }

    // -------------------------------------------------------------------------
    // useWildcard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("useWildcard: equipos NO bloqueados → RuntimeException (wildcard solo en jornada activa)")
    void useWildcard_teamsNotLocked_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(false);

        assertThatThrownBy(() -> teamService.useWildcard(1, 1, new UpdateTeamPlayersRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("jornada activa");
    }

    @Test
    @DisplayName("useWildcard: comodín ya usado → RuntimeException")
    void useWildcard_alreadyUsed_throwsException() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setWildcardUsed(true);

        when(gameweekStateService.isTeamsLocked()).thenReturn(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);

        assertThatThrownBy(() -> teamService.useWildcard(1, 1, new UpdateTeamPlayersRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("comodín");
    }

    @Test
    @DisplayName("useWildcard: válido → aplica jugadores y marca wildcardUsed=true")
    void useWildcard_valid_appliesPlayersAndSetsFlag() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league);
        team.setWildcardUsed(false);
        team.setPlayerTeams(new ArrayList<>());

        Player player = buildPlayer("P1", Position.DEL, 1_500_000);

        UpdateTeamPlayersRequest.PlayerSelection sel = new UpdateTeamPlayersRequest.PlayerSelection();
        sel.setPlayerId("P1");
        sel.setLined(true);
        sel.setIsCaptain(false);

        UpdateTeamPlayersRequest request = new UpdateTeamPlayersRequest();
        request.setPlayers(List.of(sel));

        when(gameweekStateService.isTeamsLocked()).thenReturn(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(playerService.getPlayerById("P1")).thenReturn(player);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        Team result = teamService.useWildcard(1, 1, request);

        assertThat(result.getWildcardUsed()).isTrue();
        assertThat(result.getPlayerTeams()).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // buyoutPlayer
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("buyoutPlayer: equipos bloqueados → RuntimeException")
    void buyoutPlayer_teamsLocked_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(true);

        assertThatThrownBy(() -> teamService.buyoutPlayer(1, 2, "P1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bloqueadas");
    }

    @Test
    @DisplayName("buyoutPlayer: comprador y vendedor son el mismo usuario → RuntimeException")
    void buyoutPlayer_selfBuyout_throwsException() {
        User buyer = buildUser(1, "alice");

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        setupSecurityContext("alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(java.util.Optional.of(buyer));

        try {
            assertThatThrownBy(() -> teamService.buyoutPlayer(1, 1, "P1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ti mismo");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("buyoutPlayer: jugador no pertenece al equipo vendedor → RuntimeException")
    void buyoutPlayer_playerNotInSellerTeam_throwsException() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team buyerTeam = buildTeam(1, buyer, league);
        Team sellerTeam = buildTeam(2, seller, league);
        sellerTeam.setPlayerTeams(new java.util.ArrayList<>());

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        setupSecurityContext("alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(java.util.Optional.of(buyer));
        when(userRepository.findById(1)).thenReturn(java.util.Optional.of(buyer));
        when(userRepository.findById(2)).thenReturn(java.util.Optional.of(seller));
        when(leagueRepository.findById(1L)).thenReturn(java.util.Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(buyerTeam);
        when(teamRepository.findByLeagueAndUser(league, seller)).thenReturn(sellerTeam);

        try {
            assertThatThrownBy(() -> teamService.buyoutPlayer(1, 2, "P1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no pertenece al equipo");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("buyoutPlayer: presupuesto insuficiente → RuntimeException")
    void buyoutPlayer_insufficientBudget_throwsException() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        // buyer has only 100_000 budget
        Team buyerTeam = buildTeam(1, buyer, league);
        buyerTeam.setBudget(100_000);
        Team sellerTeam = buildTeam(2, seller, league);

        Player player = buildPlayer("P1", Position.DEL, 2_000_000);
        PlayerTeam playerTeam = new PlayerTeam();
        playerTeam.setPlayer(player);
        playerTeam.setTeam(sellerTeam);
        playerTeam.setBuyPrice(2_000_000);
        playerTeam.setSellPrice(2_000_000);
        playerTeam.setIsCaptain(false);
        playerTeam.setLined(false);
        sellerTeam.setPlayerTeams(new java.util.ArrayList<>(List.of(playerTeam)));

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        setupSecurityContext("alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(java.util.Optional.of(buyer));
        when(userRepository.findById(1)).thenReturn(java.util.Optional.of(buyer));
        when(userRepository.findById(2)).thenReturn(java.util.Optional.of(seller));
        when(leagueRepository.findById(1L)).thenReturn(java.util.Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(buyerTeam);
        when(teamRepository.findByLeagueAndUser(league, seller)).thenReturn(sellerTeam);

        try {
            assertThatThrownBy(() -> teamService.buyoutPlayer(1, 2, "P1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("insuficiente");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("buyoutPlayer: parámetros válidos → transfiere jugador y actualiza presupuestos")
    void buyoutPlayer_valid_transfersPlayerAndUpdatesBudgets() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team buyerTeam = buildTeam(1, buyer, league);
        buyerTeam.setBudget(10_000_000);
        Team sellerTeam = buildTeam(2, seller, league);
        sellerTeam.setBudget(5_000_000);

        Player player = buildPlayer("P1", Position.DEL, 3_000_000);
        PlayerTeam playerTeam = new PlayerTeam();
        playerTeam.setPlayer(player);
        playerTeam.setTeam(sellerTeam);
        playerTeam.setBuyPrice(3_000_000);
        playerTeam.setSellPrice(3_000_000);
        playerTeam.setIsCaptain(false);
        playerTeam.setLined(false);
        sellerTeam.setPlayerTeams(new java.util.ArrayList<>(List.of(playerTeam)));

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        setupSecurityContext("alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(java.util.Optional.of(buyer));
        when(userRepository.findById(1)).thenReturn(java.util.Optional.of(buyer));
        when(userRepository.findById(2)).thenReturn(java.util.Optional.of(seller));
        when(leagueRepository.findById(1L)).thenReturn(java.util.Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(buyerTeam);
        when(teamRepository.findByLeagueAndUser(league, seller)).thenReturn(sellerTeam);
        when(playerTeamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).createClauseNotification(anyInt(), any(), any(), any(), anyInt());

        try {
            Team result = teamService.buyoutPlayer(1, 2, "P1");

            assertThat(result.getBudget()).isEqualTo(10_000_000 - 3_000_000);
            assertThat(sellerTeam.getBudget()).isEqualTo(5_000_000 + 3_000_000);
            assertThat(playerTeam.getTeam()).isEqualTo(buyerTeam);
            assertThat(playerTeam.getIsCaptain()).isFalse();
            assertThat(playerTeam.getLined()).isFalse();
            verify(playerTeamRepository).save(playerTeam);
            verify(teamRepository, times(2)).save(any());
            verify(notificationService).createClauseNotification(eq(1), eq(buyer), eq(seller), eq(player), eq(3_000_000));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("buyoutPlayer: usa marketValue cuando buyPrice y sellPrice son nulos")
    void buyoutPlayer_fallsBackToMarketValue_whenNoPriceSet() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team buyerTeam = buildTeam(1, buyer, league);
        buyerTeam.setBudget(10_000_000);
        Team sellerTeam = buildTeam(2, seller, league);
        sellerTeam.setBudget(1_000_000);

        Player player = buildPlayer("P1", Position.MID, 2_500_000);
        PlayerTeam playerTeam = new PlayerTeam();
        playerTeam.setPlayer(player);
        playerTeam.setTeam(sellerTeam);
        playerTeam.setBuyPrice(null);
        playerTeam.setSellPrice(null);
        playerTeam.setIsCaptain(false);
        playerTeam.setLined(false);
        sellerTeam.setPlayerTeams(new java.util.ArrayList<>(List.of(playerTeam)));

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        setupSecurityContext("alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(java.util.Optional.of(buyer));
        when(userRepository.findById(1)).thenReturn(java.util.Optional.of(buyer));
        when(userRepository.findById(2)).thenReturn(java.util.Optional.of(seller));
        when(leagueRepository.findById(1L)).thenReturn(java.util.Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(buyerTeam);
        when(teamRepository.findByLeagueAndUser(league, seller)).thenReturn(sellerTeam);
        when(playerTeamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).createClauseNotification(anyInt(), any(), any(), any(), anyInt());

        try {
            Team result = teamService.buyoutPlayer(1, 2, "P1");

            // Falls back to marketValue = 2_500_000
            assertThat(result.getBudget()).isEqualTo(10_000_000 - 2_500_000);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    // -------------------------------------------------------------------------
    // getTeamByUserAndLeague — user not found
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getTeamByUserAndLeague: usuario no encontrado → RuntimeException")
    void getTeamByUserAndLeague_userNotFound_throwsException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamByUserAndLeague(1, 999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("getTeamByUserAndLeague: liga no encontrada → RuntimeException")
    void getTeamByUserAndLeague_leagueNotFound_throwsException() {
        User user = buildUser(1, "alice");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamByUserAndLeague(99, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Liga no encontrada");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(int id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@mail.com");
        u.setPassword("encoded");
        u.setDisplayName(username);
        u.setRole("USER");
        return u;
    }

    private League buildLeague(Integer id) {
        League l = new League();
        l.setId(id);
        l.setName("Liga Test");
        l.setCode("ABCDEF");
        l.setMaxTeams(10);
        l.setInitialBudget(10_000_000);
        return l;
    }

    private Team buildTeam(int id, User user, League league) {
        Team t = new Team();
        t.setId(id);
        t.setUser(user);
        t.setLeague(league);
        t.setBudget(5_000_000);
        t.setTotalPoints(0);
        t.setWildcardUsed(false);
        t.setCreatedAt(new java.util.Date());
        return t;
    }

    private Player buildPlayer(String id, Position position, int marketValue) {
        Player p = new Player();
        p.setId(id);
        p.setFullName("Player " + id);
        p.setPosition(position);
        p.setMarketValue(marketValue);
        return p;
    }

    private void setupSecurityContext(String username) {
        org.springframework.security.core.Authentication mockAuth =
                mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext mockSecCtx =
                mock(org.springframework.security.core.context.SecurityContext.class);
        when(mockAuth.getName()).thenReturn(username);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockSecCtx.getAuthentication()).thenReturn(mockAuth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(mockSecCtx);
    }
}
