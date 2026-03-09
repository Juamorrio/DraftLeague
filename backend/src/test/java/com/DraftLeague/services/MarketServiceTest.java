package com.DraftLeague.services;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Market.MarketPlayer;
import com.DraftLeague.models.Market.StatusMarketPlayer;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketService Unit Tests")
class MarketServiceTest {

    @Mock private MarketPlayerRepository marketPlayerRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private LeagueRepository leagueRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private NotificationService notificationService;
    @Mock private GameweekStateService gameweekStateService;

    @InjectMocks
    private MarketService marketService;

    // ─── getAvailableMarketPlayers ────────────────────────────────────────────────

    @Test
    @DisplayName("getAvailableMarketPlayers: mercado con jugadores disponibles → devuelve lista")
    void getAvailableMarketPlayers_existing_returnsList() {
        League league = buildLeague(1);
        MarketPlayer mp = buildMarketPlayer(1, buildPlayer("P1", 1_000_000), league);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE))
                .thenReturn(List.of(mp));

        List<MarketPlayer> result = marketService.getAvailableMarketPlayers(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    // ─── placeBid ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("placeBid: mercado bloqueado durante jornada → IllegalStateException")
    void placeBid_teamsLocked_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(true);

        assertThatThrownBy(() -> marketService.placeBid(1, "alice", 500_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cerrado");
    }

    @Test
    @DisplayName("placeBid: puja inferior al valor de mercado → IllegalStateException")
    void placeBid_bidBelowMarketValue_throwsException() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        User user = buildUser(1, "alice");
        Team team = buildTeam(1, user, league, 5_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));

        // Bid 100 < market value 1_000_000
        assertThatThrownBy(() -> marketService.placeBid(1, "alice", 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inferior al valor de mercado");
    }

    @Test
    @DisplayName("placeBid: presupuesto insuficiente → IllegalStateException")
    void placeBid_insufficientBudget_throwsException() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        User user = buildUser(1, "alice");
        Team team = buildTeam(1, user, league, 500_000); // less than bid

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);

        assertThatThrownBy(() -> marketService.placeBid(1, "alice", 2_000_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insuficiente");
    }

    @Test
    @DisplayName("placeBid: puja válida → actualiza presupuesto y persiste puja")
    void placeBid_valid_updatesBudgetAndSaves() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        User user = buildUser(1, "alice");
        Team team = buildTeam(1, user, league, 5_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        marketService.placeBid(1, "alice", 1_500_000L);

        verify(teamRepository).save(team);
        verify(marketPlayerRepository).save(mp);
        assertThat(team.getBudget()).isEqualTo(5_000_000 - 1_500_000);
    }

    // ─── initializeMarket ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeMarket: liga válida → crea MarketPlayer para cada jugador libre")
    void initializeMarket_validLeague_createsMarketEntries() {
        League league = buildLeague(1);
        Player p1 = buildPlayer("P1", 1_000_000);
        Player p2 = buildPlayer("P2", 2_000_000);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeague(league)).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(p1, p2));
        when(playerTeamRepository.findPlayerTeamsByTeamIdIn(any())).thenReturn(Collections.emptyList());
        when(marketPlayerRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        marketService.initializeMarket(1);

        verify(marketPlayerRepository).saveAll(any());
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

    private Player buildPlayer(String id, int marketValue) {
        Player p = new Player();
        p.setId(id);
        p.setFullName("Player " + id);
        p.setPosition(Position.DEL);
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

    private MarketPlayer buildMarketPlayer(int id, Player player, League league) {
        MarketPlayer mp = new MarketPlayer();
        mp.setId(id);
        mp.setPlayer(player);
        mp.setLeague(league);
        mp.setStatus(StatusMarketPlayer.AVAILABLE);
        mp.setCurrentBid(player.getMarketValue().longValue());
        mp.setAuctionEndTime(LocalDateTime.now().plusHours(24));
        return mp;
    }
}
