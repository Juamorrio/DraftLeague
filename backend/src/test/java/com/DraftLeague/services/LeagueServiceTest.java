package com.DraftLeague.services;

import com.DraftLeague.dto.CreateLeagueRequest;
import com.DraftLeague.dto.UpdateLeagueRequest;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueService Unit Tests")
class LeagueServiceTest {

    @Mock private LeagueRepository leagueRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private NotificationLeagueRepository notificationLeagueRepository;
    @Mock private com.DraftLeague.repositories.NotificationRepository notificationRepository;
    @Mock private TradeOfferRepository tradeOfferRepository;
    @Mock private MarketPlayerRepository marketPlayerRepository;
    @Mock private TeamGameweekPointsRepository teamGameweekPointsRepository;
    @Mock private TeamPlayerGameweekPointsRepository teamPlayerGameweekPointsRepository;

    @InjectMocks
    private LeagueService leagueService;

    @BeforeEach
    void setUpAuth() {
        // Set a fake authenticated user in Spring Security context
        var auth = new UsernamePasswordAuthenticationToken("alice", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }


    @Test
    @DisplayName("createLeague: request válida → persiste liga con código generado")
    void createLeague_validRequest_savesLeagueWithCode() {
        CreateLeagueRequest req = buildRequest("Liga Test", 10, 10_000_000);
        User alice = buildUser(1, "alice");

        when(leagueRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> {
            League l = inv.getArgument(0);
            l.setId(1);
            return l;
        });
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        League result = leagueService.createLeague(req);

        assertThat(result.getName()).isEqualTo("Liga Test");
        assertThat(result.getCode()).isNotBlank().hasSize(6);
        verify(leagueRepository).save(any(League.class));
    }


    @Test
    @DisplayName("getLeagueById: id existente → devuelve la liga")
    void getLeagueById_exists_returnsLeague() {
        League league = buildLeague(1, "Liga Test");
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

        League result = leagueService.getLeagueById(1L);

        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getLeagueById: id inexistente → RuntimeException")
    void getLeagueById_notFound_throwsRuntimeException() {
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.getLeagueById(99L))
                .isInstanceOf(RuntimeException.class);
    }


    @Test
    @DisplayName("getRanking: devuelve equipos ordenados por puntos descendente")
    void getRanking_returnsTeamsSortedByPointsDesc() {
        League league = buildLeague(1, "Liga Test");
        User u1 = buildUser(1, "alice");
        User u2 = buildUser(2, "bob");
        Team t1 = buildTeam(1, u1, league, 30);
        Team t2 = buildTeam(2, u2, league, 10);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueOrderByTotalPointsDesc(league)).thenReturn(List.of(t1, t2));

        List<Map<String, Object>> ranking = leagueService.getRanking(1L);

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).get("position")).isEqualTo(1);
        assertThat(ranking.get(1).get("position")).isEqualTo(2);
    }


    @Test
    @DisplayName("joinLeagueByCode: código inválido → RuntimeException")
    void joinLeagueByCode_invalidCode_throwsException() {
        when(leagueRepository.findByCode("BADCD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.joinLeagueByCode("BADCD"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    @DisplayName("joinLeagueByCode: liga llena → RuntimeException")
    void joinLeagueByCode_leagueFull_throwsException() {
        League league = buildLeague(1, "Liga Llena");
        league.setMaxTeams(2);
        User alice = buildUser(1, "alice");

        when(leagueRepository.findByCode("ABCDEF")).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));
        when(teamRepository.findByLeagueAndUser(league, alice)).thenReturn(null);
        when(teamRepository.countByLeague(league)).thenReturn(2L); // full

        assertThatThrownBy(() -> leagueService.joinLeagueByCode("ABCDEF"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("completa");
    }

    @Test
    @DisplayName("joinLeagueByCode: usuario ya en la liga → RuntimeException")
    void joinLeagueByCode_alreadyInLeague_throwsException() {
        League league = buildLeague(1, "Liga Test");
        User alice = buildUser(1, "alice");
        Team existingTeam = buildTeam(1, alice, league, 0);

        when(leagueRepository.findByCode("ABCDEF")).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));
        when(teamRepository.findByLeagueAndUser(league, alice)).thenReturn(existingTeam);

        assertThatThrownBy(() -> leagueService.joinLeagueByCode("ABCDEF"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("en esta liga");
    }


    // -------------------------------------------------------------------------
    // getAllLeagues
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAllLeagues: devuelve todas las ligas del repositorio")
    void getAllLeagues_returnsList() {
        League l1 = buildLeague(1, "Liga A");
        League l2 = buildLeague(2, "Liga B");
        when(leagueRepository.findAll()).thenReturn(List.of(l1, l2));

        List<League> result = leagueService.getAllLeagues();

        assertThat(result).hasSize(2);
        verify(leagueRepository).findAll();
    }

    @Test
    @DisplayName("getAllLeagues: sin ligas → lista vacía")
    void getAllLeagues_noLeagues_returnsEmptyList() {
        when(leagueRepository.findAll()).thenReturn(Collections.emptyList());

        List<League> result = leagueService.getAllLeagues();

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // updateLeague
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateLeague: creador de la liga → actualiza nombre y descripción")
    void updateLeague_byCreator_updatesSuccessfully() {
        User alice = buildUser(1, "alice");
        alice.setRole("USER");
        League league = buildLeague(1, "Old Name");
        league.setCreatedBy(alice);

        UpdateLeagueRequest req = new UpdateLeagueRequest();
        req.setName("New Name");
        req.setDescription("New desc");

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));

        League result = leagueService.updateLeague(1L, req);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("New desc");
        verify(leagueRepository).save(league);
    }

    @Test
    @DisplayName("updateLeague: admin que no es creador → actualiza correctamente")
    void updateLeague_byAdmin_updatesSuccessfully() {
        User creator = buildUser(2, "bob");
        User admin = buildUser(1, "alice");
        admin.setRole("ADMIN");

        League league = buildLeague(1, "Old Name");
        league.setCreatedBy(creator);

        UpdateLeagueRequest req = new UpdateLeagueRequest();
        req.setName("Admin Name");
        req.setDescription(null);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(admin));
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));

        League result = leagueService.updateLeague(1L, req);

        assertThat(result.getName()).isEqualTo("Admin Name");
    }

    @Test
    @DisplayName("updateLeague: usuario sin permisos → RuntimeException")
    void updateLeague_noPermission_throwsException() {
        User creator = buildUser(2, "bob");
        User other = buildUser(1, "alice");
        other.setRole("USER");

        League league = buildLeague(1, "Old Name");
        league.setCreatedBy(creator);

        UpdateLeagueRequest req = new UpdateLeagueRequest();
        req.setName("Hacked Name");

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> leagueService.updateLeague(1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permisos");
    }

    // -------------------------------------------------------------------------
    // deleteLeague
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteLeague: creador elimina la liga → borra todas las entidades relacionadas")
    void deleteLeague_byCreator_deletesAllRelatedEntities() {
        User alice = buildUser(1, "alice");
        alice.setRole("USER");
        League league = buildLeague(1, "Liga Test");
        league.setCreatedBy(alice);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));
        when(teamRepository.findByLeague(league)).thenReturn(Collections.emptyList());
        when(marketPlayerRepository.findByLeague(league)).thenReturn(Collections.emptyList());

        leagueService.deleteLeague(1L);

        verify(leagueRepository).delete(league);
        verify(notificationRepository).deleteAllByLeagueId(league.getId());
        verify(notificationLeagueRepository).deleteAllByLeagueId(league.getId());
        verify(tradeOfferRepository).deleteByLeague(league);
    }

    @Test
    @DisplayName("deleteLeague: admin elimina liga ajena → lo permite")
    void deleteLeague_byAdmin_deletesSuccessfully() {
        User creator = buildUser(2, "bob");
        User admin = buildUser(1, "alice");
        admin.setRole("ADMIN");
        League league = buildLeague(1, "Liga Test");
        league.setCreatedBy(creator);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(admin));
        when(teamRepository.findByLeague(league)).thenReturn(Collections.emptyList());
        when(marketPlayerRepository.findByLeague(league)).thenReturn(Collections.emptyList());

        leagueService.deleteLeague(1L);

        verify(leagueRepository).delete(league);
    }

    @Test
    @DisplayName("deleteLeague: usuario sin permisos → RuntimeException")
    void deleteLeague_noPermission_throwsException() {
        User creator = buildUser(2, "bob");
        User other = buildUser(1, "alice");
        other.setRole("USER");
        League league = buildLeague(1, "Liga Test");
        league.setCreatedBy(creator);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> leagueService.deleteLeague(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("deleteLeague: elimina también los equipos y sus jugadores")
    void deleteLeague_withTeams_deletesTeamsAndPlayerTeams() {
        User alice = buildUser(1, "alice");
        alice.setRole("USER");
        League league = buildLeague(1, "Liga Test");
        league.setCreatedBy(alice);

        Team team = buildTeam(1, alice, league, 0);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));
        when(teamRepository.findByLeague(league)).thenReturn(List.of(team));
        when(marketPlayerRepository.findByLeague(league)).thenReturn(Collections.emptyList());
        when(playerTeamRepository.findByTeam(team)).thenReturn(Collections.emptyList());

        leagueService.deleteLeague(1L);

        verify(teamRepository).deleteAll(List.of(team));
        verify(teamPlayerGameweekPointsRepository).deleteAllByTeam(team);
        verify(teamGameweekPointsRepository).deleteAllByTeam(team);
    }

    // -------------------------------------------------------------------------
    // getLeaguesByUserId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getLeaguesByUserId: usuario en una liga → devuelve fila con datos correctos")
    void getLeaguesByUserId_oneLeague_returnsCorrectRow() {
        User alice = buildUser(1, "alice");
        League league = buildLeague(1, "Liga Test");
        league.setCreatedBy(alice);
        Team myTeam = buildTeam(1, alice, league, 42);

        when(userRepository.findById(1)).thenReturn(Optional.of(alice));
        when(teamRepository.findDistinctLeaguesByUser(alice)).thenReturn(List.of(league));
        when(teamRepository.countByLeague(league)).thenReturn(3L);
        when(teamRepository.findByLeagueAndUser(league, alice)).thenReturn(myTeam);
        when(teamRepository.findByLeagueOrderByTotalPointsDesc(league)).thenReturn(List.of(myTeam));

        List<Map<String, Object>> result = leagueService.getLeaguesByUserId(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("Liga Test");
        assertThat(result.get(0).get("totalPoints")).isEqualTo(42);
        assertThat(result.get(0).get("position")).isEqualTo(1);
        assertThat(result.get(0).get("participants")).isEqualTo(3L);
    }

    @Test
    @DisplayName("getLeaguesByUserId: usuario sin ligas → lista vacía")
    void getLeaguesByUserId_noLeagues_returnsEmptyList() {
        User alice = buildUser(1, "alice");

        when(userRepository.findById(1)).thenReturn(Optional.of(alice));
        when(teamRepository.findDistinctLeaguesByUser(alice)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = leagueService.getLeaguesByUserId(1);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getLeaguesByUserId: usuario no encontrado → RuntimeException")
    void getLeaguesByUserId_userNotFound_throwsException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.getLeaguesByUserId(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getLeaguesByUserId: usuario no está en el ranking → position=null")
    void getLeaguesByUserId_notInRanking_positionIsNull() {
        User alice = buildUser(1, "alice");
        User bob = buildUser(2, "bob");
        League league = buildLeague(1, "Liga Test");
        Team bobTeam = buildTeam(2, bob, league, 10);

        when(userRepository.findById(1)).thenReturn(Optional.of(alice));
        when(teamRepository.findDistinctLeaguesByUser(alice)).thenReturn(List.of(league));
        when(teamRepository.countByLeague(league)).thenReturn(1L);
        when(teamRepository.findByLeagueAndUser(league, alice)).thenReturn(null);
        when(teamRepository.findByLeagueOrderByTotalPointsDesc(league)).thenReturn(List.of(bobTeam));

        List<Map<String, Object>> result = leagueService.getLeaguesByUserId(1);

        assertThat(result.get(0).get("position")).isNull();
        assertThat(result.get(0).get("totalPoints")).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // joinLeagueByCode — success path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("joinLeagueByCode: código válido, liga con espacio → crea equipo y asigna jugadores")
    void joinLeagueByCode_valid_createsTeamAndReturnsLeague() {
        League league = buildLeague(1, "Liga Test");
        User alice = buildUser(1, "alice");

        when(leagueRepository.findByCode("ABCDEF")).thenReturn(Optional.of(league));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));
        when(teamRepository.findByLeagueAndUser(league, alice)).thenReturn(null);
        when(teamRepository.countByLeague(league)).thenReturn(1L); // not full
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        // assignInitialSquad: no existing teams in league → inner playerTeamRepository loop never runs
        when(teamRepository.findByLeagueOrderByTotalPointsDesc(league)).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(Collections.emptyList());

        League result = leagueService.joinLeagueByCode("ABCDEF");

        assertThat(result).isEqualTo(league);
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("joinLeagueByCode: código null → RuntimeException")
    void joinLeagueByCode_nullCode_throwsException() {
        assertThatThrownBy(() -> leagueService.joinLeagueByCode(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("joinLeagueByCode: código vacío → RuntimeException")
    void joinLeagueByCode_blankCode_throwsException() {
        assertThatThrownBy(() -> leagueService.joinLeagueByCode("   "))
                .isInstanceOf(RuntimeException.class);
    }

    // -------------------------------------------------------------------------
    // createLeague — unauthenticated path (team auto-creation skipped gracefully)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createLeague: usuario no encontrado → liga guardada pero sin equipo")
    void createLeague_userNotFound_savesLeagueWithoutTeam() {
        CreateLeagueRequest req = buildRequest("Liga X", 8, 5_000_000);

        when(leagueRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> {
            League l = inv.getArgument(0);
            l.setId(2);
            return l;
        });
        // findUserByUsername called twice inside createLeague (once for setCreatedBy, once for team)
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.empty());

        League result = leagueService.createLeague(req);

        assertThat(result.getName()).isEqualTo("Liga X");
        verify(leagueRepository).save(any(League.class));
        verify(teamRepository, never()).save(any(Team.class));
    }

    // -------------------------------------------------------------------------
    // getRanking — empty league
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getRanking: liga sin equipos → lista vacía")
    void getRanking_noTeams_returnsEmptyList() {
        League league = buildLeague(1, "Liga Test");
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueOrderByTotalPointsDesc(league)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = leagueService.getRanking(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRanking: totalPoints null → lo trata como 0")
    void getRanking_nullTotalPoints_treatedAsZero() {
        League league = buildLeague(1, "Liga Test");
        User alice = buildUser(1, "alice");
        Team team = buildTeam(1, alice, league, 0);
        team.setTotalPoints(null);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueOrderByTotalPointsDesc(league)).thenReturn(List.of(team));

        List<Map<String, Object>> result = leagueService.getRanking(1L);

        assertThat(result.get(0).get("totalPoints")).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CreateLeagueRequest buildRequest(String name, int maxTeams, int budget) {
        CreateLeagueRequest r = new CreateLeagueRequest();
        r.setName(name);
        r.setMaxTeams(maxTeams);
        r.setInitialBudget(budget);
        r.setMarketEndHour("23:00");
        r.setCaptainEnable(true);
        return r;
    }

    private User buildUser(int id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@mail.com");
        u.setPassword("$2a$encoded");
        u.setDisplayName(username);
        u.setRole("USER");
        return u;
    }

    private League buildLeague(Integer id, String name) {
        League l = new League();
        l.setId(id);
        l.setName(name);
        l.setCode("ABCDEF");
        l.setMaxTeams(10);
        l.setInitialBudget(10_000_000);
        return l;
    }

    private Team buildTeam(int id, User user, League league, int totalPoints) {
        Team t = new Team();
        t.setId(id);
        t.setUser(user);
        t.setLeague(league);
        t.setTotalPoints(totalPoints);
        t.setBudget(10_000_000);
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

    private PlayerTeam buildPlayerTeam(Player player, Team team) {
        PlayerTeam pt = new PlayerTeam();
        pt.setPlayer(player);
        pt.setTeam(team);
        pt.setLined(false);
        pt.setIsCaptain(false);
        return pt;
    }
}
