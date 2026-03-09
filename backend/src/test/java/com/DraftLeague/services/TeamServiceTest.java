package com.DraftLeague.services;

import com.DraftLeague.dto.UpdateTeamPlayersRequest;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
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
import static org.mockito.ArgumentMatchers.any;
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

    // ─── getTeamByUserAndLeague ───────────────────────────────────────────────────

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

    // ─── updateTeamPlayers ───────────────────────────────────────────────────────

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

    // ─── getTeamById ─────────────────────────────────────────────────────────────

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

    // ─── helpers ─────────────────────────────────────────────────────────────────

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
}
