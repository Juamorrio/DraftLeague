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
import static org.mockito.ArgumentMatchers.*;
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

    // -------------------------------------------------------------------------
    // getAvailableMarketPlayers
    // -------------------------------------------------------------------------

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

    @Test
    @DisplayName("getAvailableMarketPlayers: mercado vacío → inicializa automáticamente y devuelve jugadores")
    void getAvailableMarketPlayers_emptyMarket_autoInitializesAndReturns() {
        League league = buildLeague(1);
        Player p1 = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, p1, league);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        // First call returns empty (triggers auto-init), second call returns the populated list
        when(marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(mp));
        when(teamRepository.findByLeague(league)).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(p1));
        when(playerTeamRepository.findPlayerTeamsByTeamIdIn(any())).thenReturn(Collections.emptyList());
        when(marketPlayerRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<MarketPlayer> result = marketService.getAvailableMarketPlayers(1);

        assertThat(result).hasSize(1);
        verify(marketPlayerRepository, times(2))
                .findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE);
    }

    @Test
    @DisplayName("getAvailableMarketPlayers: liga no encontrada → IllegalStateException")
    void getAvailableMarketPlayers_leagueNotFound_throwsException() {
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketService.getAvailableMarketPlayers(99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Liga no encontrada");
    }

    // -------------------------------------------------------------------------
    // initializeMarket
    // -------------------------------------------------------------------------

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

    @Test
    @DisplayName("initializeMarket: liga no encontrada → IllegalStateException")
    void initializeMarket_leagueNotFound_throwsException() {
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketService.initializeMarket(99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Liga no encontrada");
    }

    // -------------------------------------------------------------------------
    // getAvailableMarketPlayersForUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAvailableMarketPlayersForUser: usuario sin puja → hasBid=false, myBid=null")
    void getAvailableMarketPlayersForUser_noBid_returnsDtoWithNoBid() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        User user = buildUser(1, "alice");

        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE))
                .thenReturn(List.of(mp));

        var result = marketService.getAvailableMarketPlayersForUser(1, "alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHasBid()).isFalse();
        assertThat(result.get(0).getMyBid()).isNull();
    }

    @Test
    @DisplayName("getAvailableMarketPlayersForUser: usuario con puja activa → hasBid=true, myBid correcto")
    void getAvailableMarketPlayersForUser_withBid_returnsDtoWithBid() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        mp.addBid(1, 2_000_000L); // register a bid for user id=1
        User user = buildUser(1, "alice");

        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE))
                .thenReturn(List.of(mp));

        var result = marketService.getAvailableMarketPlayersForUser(1, "alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHasBid()).isTrue();
        assertThat(result.get(0).getMyBid()).isEqualTo(2_000_000L);
    }

    @Test
    @DisplayName("getAvailableMarketPlayersForUser: usuario no encontrado → IllegalStateException")
    void getAvailableMarketPlayersForUser_userNotFound_throwsException() {
        // The service looks up the user first before touching the market, so no league stub needed.
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketService.getAvailableMarketPlayersForUser(1, "ghost"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // -------------------------------------------------------------------------
    // placeBid
    // -------------------------------------------------------------------------

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

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));

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
        // budget 500_000 is less than the bid of 2_000_000
        Team team = buildTeam(1, user, league, 500_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        // mp.getUserBid(user.getId()) returns null (no existing bid) so previousBidAmount stays 0
        // budget(500_000) + 0 < bidAmount(2_000_000) → throws
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

    @Test
    @DisplayName("placeBid: puja existente del mismo usuario → devuelve puja anterior antes de registrar la nueva")
    void placeBid_existingBidUpdated_refundsPreviousBidFirst() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        mp.addBid(1, 1_200_000L); // existing bid
        User user = buildUser(1, "alice");
        Team team = buildTeam(1, user, league, 3_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Budget: 3_000_000 + 1_200_000 (refund) - 2_000_000 (new bid) = 2_200_000
        marketService.placeBid(1, "alice", 2_000_000L);

        assertThat(team.getBudget()).isEqualTo(3_000_000 + 1_200_000 - 2_000_000);
    }

    // -------------------------------------------------------------------------
    // finalizeAuction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("finalizeAuction: jugador de mercado no encontrado → IllegalStateException")
    void finalizeAuction_marketPlayerNotFound_throwsException() {
        when(marketPlayerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketService.finalizeAuction(99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("finalizeAuction: sin pujas → marca como SOLD sin crear PlayerTeam")
    void finalizeAuction_noBids_marksAsSold() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);

        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        marketService.finalizeAuction(1);

        assertThat(mp.getStatus()).isEqualTo(StatusMarketPlayer.SOLD);
        verify(playerTeamRepository, never()).save(any());
        verify(marketPlayerRepository).save(mp);
    }

    @Test
    @DisplayName("finalizeAuction: con puja ganadora → transfiere jugador al ganador")
    void finalizeAuction_withBid_transfersPlayerToWinner() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        User winner = buildUser(1, "alice");
        mp.addBid(1, 2_000_000L);

        Team winnerTeam = buildTeam(1, winner, league, 10_000_000);

        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(userRepository.findById(1)).thenReturn(Optional.of(winner));
        when(teamRepository.findByLeagueAndUser(league, winner)).thenReturn(winnerTeam);
        when(teamRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(playerTeamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).createMarketBuyNotification(anyInt(), any(), any(), anyLong());

        marketService.finalizeAuction(1);

        assertThat(mp.getStatus()).isEqualTo(StatusMarketPlayer.SOLD);
        verify(playerTeamRepository).save(any());
    }

    @Test
    @DisplayName("finalizeAuction: múltiples pujas → devuelve presupuesto a los no ganadores")
    void finalizeAuction_multipleBids_refundsLosers() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);

        User winner = buildUser(1, "alice");
        User loser = buildUser(2, "bob");
        mp.addBid(2, 1_500_000L); // loser bid
        mp.addBid(1, 2_000_000L); // winner bid (highest)

        Team winnerTeam = buildTeam(1, winner, league, 5_000_000);
        Team loserTeam = buildTeam(2, loser, league, 3_000_000);

        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(userRepository.findById(1)).thenReturn(Optional.of(winner));
        when(userRepository.findById(2)).thenReturn(Optional.of(loser));
        when(teamRepository.findByLeagueAndUser(league, winner)).thenReturn(winnerTeam);
        when(teamRepository.findByLeagueAndUser(league, loser)).thenReturn(loserTeam);
        when(teamRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(playerTeamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).createMarketBuyNotification(anyInt(), any(), any(), anyLong());

        marketService.finalizeAuction(1);

        assertThat(loserTeam.getBudget()).isEqualTo(3_000_000 + 1_500_000);
        verify(teamRepository).saveAll(any());
    }

    // -------------------------------------------------------------------------
    // finalizeExpiredAuctions
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("finalizeExpiredAuctions: sin subastas expiradas → no hace nada")
    void finalizeExpiredAuctions_none_doesNothing() {
        when(marketPlayerRepository.findByStatusAndAuctionEndTimeBefore(
                eq(StatusMarketPlayer.AVAILABLE), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        marketService.finalizeExpiredAuctions();

        verify(marketPlayerRepository, never()).findById(any());
    }

    @Test
    @DisplayName("finalizeExpiredAuctions: subasta expirada sin pujas → la marca como SOLD")
    void finalizeExpiredAuctions_withExpired_finalizesEach() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        mp.setAuctionEndTime(LocalDateTime.now().minusHours(1));

        when(marketPlayerRepository.findByStatusAndAuctionEndTimeBefore(
                eq(StatusMarketPlayer.AVAILABLE), any(LocalDateTime.class)))
                .thenReturn(List.of(mp));
        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        marketService.finalizeExpiredAuctions();

        verify(marketPlayerRepository).findById(1);
        assertThat(mp.getStatus()).isEqualTo(StatusMarketPlayer.SOLD);
    }

    // -------------------------------------------------------------------------
    // cancelBid
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cancelBid: jugador de mercado no encontrado → RuntimeException")
    void cancelBid_marketPlayerNotFound_throwsException() {
        User user = buildUser(1, "alice");
        when(marketPlayerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketService.cancelBid(99, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("cancelBid: usuario sin puja activa → RuntimeException")
    void cancelBid_noBidForUser_throwsException() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);
        User user = buildUser(1, "alice");

        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));

        assertThatThrownBy(() -> marketService.cancelBid(1, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tienes una puja activa");
    }

    @Test
    @DisplayName("cancelBid: puja activa → devuelve presupuesto y actualiza highest bidder al siguiente")
    void cancelBid_withBid_refundsBudgetAndUpdatesHighest() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);

        User alice = buildUser(1, "alice");
        User bob = buildUser(2, "bob");
        mp.addBid(2, 1_200_000L); // bob stays
        mp.addBid(1, 1_800_000L); // alice cancels

        Team aliceTeam = buildTeam(1, alice, league, 2_000_000);

        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(teamRepository.findByLeagueAndUser(league, alice)).thenReturn(aliceTeam);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2)).thenReturn(Optional.of(bob));
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        marketService.cancelBid(1, alice);

        assertThat(aliceTeam.getBudget()).isEqualTo(2_000_000 + 1_800_000);
        verify(teamRepository).save(aliceTeam);
        verify(marketPlayerRepository).save(mp);
    }

    @Test
    @DisplayName("cancelBid: única puja cancelada → currentBid=0 y highestBidder=null")
    void cancelBid_lastBidCancelled_resetsCurrentBid() {
        League league = buildLeague(1);
        Player player = buildPlayer("P1", 1_000_000);
        MarketPlayer mp = buildMarketPlayer(1, player, league);

        User alice = buildUser(1, "alice");
        mp.addBid(1, 1_500_000L);

        Team aliceTeam = buildTeam(1, alice, league, 3_000_000);

        when(marketPlayerRepository.findById(1)).thenReturn(Optional.of(mp));
        when(teamRepository.findByLeagueAndUser(league, alice)).thenReturn(aliceTeam);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        marketService.cancelBid(1, alice);

        assertThat(mp.getCurrentBid()).isZero();
        assertThat(mp.getHighestBidder()).isNull();
    }

    // -------------------------------------------------------------------------
    // refreshMarket
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("refreshMarket: liga no encontrada → IllegalStateException")
    void refreshMarket_leagueNotFound_throwsException() {
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketService.refreshMarket(99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Liga no encontrada");
    }

    @Test
    @DisplayName("refreshMarket: liga sin equipos → crea nuevo mercado con jugadores libres")
    void refreshMarket_noTeams_populatesMarket() {
        League league = buildLeague(1);
        Player p1 = buildPlayer("P1", 1_000_000);
        Player p2 = buildPlayer("P2", 2_000_000);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE))
                .thenReturn(Collections.emptyList());
        when(marketPlayerRepository.findByLeague(league)).thenReturn(Collections.emptyList());
        when(teamRepository.findByLeague(league)).thenReturn(Collections.emptyList());
        when(playerTeamRepository.findPlayerTeamsByTeamIdIn(any())).thenReturn(Collections.emptyList());
        when(playerRepository.findAll()).thenReturn(List.of(p1, p2));
        when(marketPlayerRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        marketService.refreshMarket(1);

        verify(marketPlayerRepository).saveAll(any());
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
