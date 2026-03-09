package com.DraftLeague.services;

import com.DraftLeague.dto.CreateLeagueRequest;
import com.DraftLeague.models.League.League;
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

    // ─── createLeague ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createLeague: request válida → persiste liga con código generado")
    void createLeague_validRequest_savesLeagueWithCode() {
        CreateLeagueRequest req = buildRequest("Liga Test", 10, 10_000_000);
        User alice = buildUser(1, "alice");

        when(leagueRepository.findByCode(anyString())).thenReturn(Optional.empty()); // code unique
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

    // ─── getLeagueById ───────────────────────────────────────────────────────────

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

    // ─── getRanking ──────────────────────────────────────────────────────────────

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

    // ─── joinLeagueByCode ────────────────────────────────────────────────────────

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

    // ─── helpers ─────────────────────────────────────────────────────────────────

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
}
