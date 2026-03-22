package com.DraftLeague.services;

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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService Unit Tests")
class PlayerServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private LeagueRepository leagueRepository;
    @Mock private PlayerMarketValueHistoryRepository marketValueHistoryRepository;

    @InjectMocks
    private PlayerService playerService;

    // ─── purchasePlayer ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("purchasePlayer: jugador disponible con presupuesto suficiente → crea PlayerTeam")
    void purchasePlayer_sufficientBudget_createsPlayerTeam() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);
        Team team = buildTeam(1, user, league, 5_000_000);

        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(playerTeamRepository.existsByTeamAndPlayer(team, player)).thenReturn(false);
        when(playerTeamRepository.save(any(PlayerTeam.class))).thenAnswer(inv -> inv.getArgument(0));

        playerService.purchasePlayer("P1", 1, user);

        verify(playerTeamRepository).save(any(PlayerTeam.class));
    }

    @Test
    @DisplayName("purchasePlayer: usuario ya posee el jugador → IllegalStateException")
    void purchasePlayer_alreadyOwned_throwsException() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);
        Team team = buildTeam(1, user, league, 5_000_000);

        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(playerTeamRepository.existsByTeamAndPlayer(team, player)).thenReturn(true);

        assertThatThrownBy(() -> playerService.purchasePlayer("P1", 1, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya posees este jugador");
    }

    // ─── getAvailablePlayersForUserInLeague ───────────────────────────────────────

    // @Test
    // @DisplayName("getAvailablePlayersForUserInLeague: excluye jugadores ya en posesión")
    // void getAvailablePlayersForUserInLeague_excludesOwnedPlayers() {
    //     User user = buildUser(1, "alice");
    //     League league = buildLeague(1);
    //     Player ownedPlayer = buildPlayer("P1", Position.DEL, 1_000_000);
    //     Player freePlayer = buildPlayer("P2", Position.MID, 2_000_000);
    //     Team team = buildTeam(1, user, league, 5_000_000);
    //     PlayerTeam pt = new PlayerTeam();
    //     pt.setPlayer(ownedPlayer);

    //     when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
    //     when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
    //     when(playerTeamRepository.findByTeam(team)).thenReturn(List.of(pt));
    //     when(playerRepository.findAll()).thenReturn(List.of(ownedPlayer, freePlayer));

    //     List<Player> result = playerService.getAvailablePlayersForUserInLeague(user, 1);

    //     assertThat(result).hasSize(1);
    //     assertThat(result.get(0).getId()).isEqualTo("P2");
    // }

    // ─── getPlayerById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPlayerById: id inexistente → RuntimeException")
    void getPlayerById_notFound_throwsRuntimeException() {
        when(playerRepository.findById("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.getPlayerById("GHOST"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
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

    private Player buildPlayer(String id, Position position, int marketValue) {
        Player p = new Player();
        p.setId(id);
        p.setFullName("Player " + id);
        p.setPosition(position);
        p.setMarketValue(marketValue);
        return p;
    }

    private Team buildTeam(int id, User user, League league, int budget) {
        Team t = new Team();
        t.setId(id);
        t.setUser(user);
        t.setLeague(league);
        t.setBudget(budget);
        t.setTotalPoints(0);
        return t;
    }
}
