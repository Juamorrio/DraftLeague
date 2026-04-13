package com.DraftLeague.services;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Trade.TradeOffer;
import com.DraftLeague.models.Trade.TradeOfferStatus;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeOfferService Unit Tests")
class TradeOfferServiceTest {

    @Mock private TradeOfferRepository tradeOfferRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private LeagueRepository leagueRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private GameweekStateService gameweekStateService;

    @InjectMocks
    private TradeOfferService tradeOfferService;


    @Test
    @DisplayName("createOffer: oferta válida → persiste con estado PENDING")
    void createOffer_valid_savesPendingOffer() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 5_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(fromTeam);
        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(playerTeamRepository.existsByTeamAndPlayer(toTeam, player)).thenReturn(true);
        when(playerTeamRepository.existsByTeamAndPlayer(fromTeam, player)).thenReturn(false);
        when(tradeOfferRepository.save(any(TradeOffer.class))).thenAnswer(inv -> {
            TradeOffer offer = inv.getArgument(0);
            offer.setId(1L);
            return offer;
        });
        doNothing().when(notificationService).createTradeOfferNotification(any(), any(), any(), any(), any(), any());

        TradeOffer result = tradeOfferService.createOffer(buyer, 2, "P1", 500_000, 1);

        assertThat(result.getStatus()).isEqualTo(TradeOfferStatus.PENDING);
        verify(tradeOfferRepository).save(any(TradeOffer.class));
    }

    @Test
    @DisplayName("createOffer: jugador no pertenece al equipo receptor → IllegalStateException")
    void createOffer_playerNotInToTeam_throwsException() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 5_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(fromTeam);
        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(playerTeamRepository.existsByTeamAndPlayer(toTeam, player)).thenReturn(false);

        assertThatThrownBy(() -> tradeOfferService.createOffer(buyer, 2, "P1", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no pertenece al equipo receptor");
    }

    @Test
    @DisplayName("createOffer: presupuesto insuficiente → IllegalStateException")
    void createOffer_insufficientBudget_throwsException() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 100); // very low budget
        Team toTeam = buildTeam(2, seller, league, 5_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(fromTeam);
        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(playerTeamRepository.existsByTeamAndPlayer(toTeam, player)).thenReturn(true);
        when(playerTeamRepository.existsByTeamAndPlayer(fromTeam, player)).thenReturn(false);

        assertThatThrownBy(() -> tradeOfferService.createOffer(buyer, 2, "P1", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Presupuesto insuficiente");
    }

    @Test
    @DisplayName("createOffer: equipos bloqueados por jornada → IllegalStateException")
    void createOffer_teamsLocked_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(true);

        assertThatThrownBy(() -> tradeOfferService.createOffer(new User(), 2, "P1", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bloqueadas");
    }


    @Test
    @DisplayName("acceptOffer: oferta PENDING → cambia estado a ACCEPTED")
    void acceptOffer_pendingOffer_changesStatusToAccepted() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 5_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setPlayer(player);
        offer.setOfferPrice(500_000);
        offer.setLeague(league);

        PlayerTeam pt = new PlayerTeam();
        pt.setPlayer(player);
        pt.setTeam(toTeam);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(playerTeamRepository.findByTeam(toTeam)).thenReturn(List.of(pt));
        when(playerTeamRepository.save(any())).thenReturn(pt);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).createTradeResultNotification(any(), any(), any(), any(), any(), any(), anyBoolean());
        doNothing().when(notificationService).deleteTradeOfferNotification(any(), any());

        TradeOffer result = tradeOfferService.acceptOffer(1L, seller);

        assertThat(result.getStatus()).isEqualTo(TradeOfferStatus.ACCEPTED);
    }

    @Test
    @DisplayName("acceptOffer: oferta ya no está pendiente → IllegalStateException")
    void acceptOffer_alreadyAccepted_throwsException() {
        User seller = buildUser(2, "bob");
        Team toTeam = buildTeam(2, seller, buildLeague(1), 0);
        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.ACCEPTED);
        offer.setToTeam(toTeam);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> tradeOfferService.acceptOffer(1L, seller))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está pendiente");
    }


    @Test
    @DisplayName("rejectOffer: oferta PENDING → cambia estado a REJECTED")
    void rejectOffer_pendingOffer_changesStatusToRejected() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 0);
        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setLeague(league);
        offer.setPlayer(buildPlayer("P1", Position.DEL, 1_000_000));

        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        TradeOffer result = tradeOfferService.rejectOffer(1L, seller);

        assertThat(result.getStatus()).isEqualTo(TradeOfferStatus.REJECTED);
    }


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

    private Team buildTeam(int id, User user, League league, int budget) {
        Team t = new Team();
        t.setId(id);
        t.setUser(user);
        t.setLeague(league);
        t.setBudget(budget);
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

    // -----------------------------------------------------------------------
    // createOffer – additional validation branches
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createOffer: liga no encontrada → IllegalStateException")
    void createOffer_leagueNotFound_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeOfferService.createOffer(new User(), 2, "P1", 500_000, 99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Liga no encontrada");
    }

    @Test
    @DisplayName("createOffer: equipo destinatario no encontrado → IllegalStateException")
    void createOffer_toTeamNotFound_throwsException() {
        League league = buildLeague(1);
        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeOfferService.createOffer(new User(), 99, "P1", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Equipo destinatario no encontrado");
    }

    @Test
    @DisplayName("createOffer: equipo no pertenece a la liga → IllegalStateException")
    void createOffer_toTeamNotInLeague_throwsException() {
        League league1 = buildLeague(1);
        League league2 = buildLeague(2);
        User buyer = buildUser(1, "alice");
        Team toTeam = buildTeam(2, buildUser(2, "bob"), league2, 5_000_000); // different league

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league1));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));

        assertThatThrownBy(() -> tradeOfferService.createOffer(buyer, 2, "P1", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no pertenece a esta liga");
    }

    @Test
    @DisplayName("createOffer: comprador sin equipo en la liga → IllegalStateException")
    void createOffer_buyerHasNoTeamInLeague_throwsException() {
        User buyer = buildUser(1, "alice");
        League league = buildLeague(1);
        Team toTeam = buildTeam(2, buildUser(2, "bob"), league, 5_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(null);

        assertThatThrownBy(() -> tradeOfferService.createOffer(buyer, 2, "P1", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tienes un equipo en esta liga");
    }

    @Test
    @DisplayName("createOffer: oferta a uno mismo → IllegalStateException")
    void createOffer_offerToSelf_throwsException() {
        User buyer = buildUser(1, "alice");
        League league = buildLeague(1);
        // fromTeam and toTeam share the same id
        Team team = buildTeam(5, buyer, league, 5_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(5)).thenReturn(Optional.of(team));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(team);

        assertThatThrownBy(() -> tradeOfferService.createOffer(buyer, 5, "P1", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No puedes hacerte una oferta a ti mismo");
    }

    @Test
    @DisplayName("createOffer: jugador no encontrado → IllegalStateException")
    void createOffer_playerNotFound_throwsException() {
        User buyer = buildUser(1, "alice");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, buildUser(2, "bob"), league, 5_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(fromTeam);
        when(playerRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeOfferService.createOffer(buyer, 2, "MISSING", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Jugador no encontrado");
    }

    @Test
    @DisplayName("createOffer: comprador ya posee el jugador → IllegalStateException")
    void createOffer_buyerAlreadyOwnsPlayer_throwsException() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 5_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(fromTeam);
        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(playerTeamRepository.existsByTeamAndPlayer(toTeam, player)).thenReturn(true);
        when(playerTeamRepository.existsByTeamAndPlayer(fromTeam, player)).thenReturn(true); // buyer owns it

        assertThatThrownBy(() -> tradeOfferService.createOffer(buyer, 2, "P1", 500_000, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya posees este jugador");
    }

    @Test
    @DisplayName("createOffer: precio de oferta <= 0 → IllegalStateException")
    void createOffer_zeroPriceOffer_throwsException() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 5_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(fromTeam);
        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(playerTeamRepository.existsByTeamAndPlayer(toTeam, player)).thenReturn(true);
        when(playerTeamRepository.existsByTeamAndPlayer(fromTeam, player)).thenReturn(false);

        assertThatThrownBy(() -> tradeOfferService.createOffer(buyer, 2, "P1", 0, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mayor que 0");
    }

    // -----------------------------------------------------------------------
    // acceptOffer – additional branches
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("acceptOffer: equipos bloqueados → IllegalStateException")
    void acceptOffer_teamsLocked_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(true);

        assertThatThrownBy(() -> tradeOfferService.acceptOffer(1L, new User()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bloqueadas");
    }

    @Test
    @DisplayName("acceptOffer: oferta no encontrada → IllegalStateException")
    void acceptOffer_offerNotFound_throwsException() {
        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeOfferService.acceptOffer(99L, new User()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Oferta no encontrada");
    }

    @Test
    @DisplayName("acceptOffer: usuario no es el destinatario → IllegalStateException")
    void acceptOffer_wrongUser_throwsException() {
        User seller = buildUser(2, "bob");
        User wrongUser = buildUser(99, "eve");
        League league = buildLeague(1);
        Team toTeam = buildTeam(2, seller, league, 0);

        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setToTeam(toTeam);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> tradeOfferService.acceptOffer(1L, wrongUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tienes permiso para aceptar");
    }

    @Test
    @DisplayName("acceptOffer: comprador ya no tiene presupuesto → IllegalStateException")
    void acceptOffer_buyerInsufficientBudget_throwsException() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 0); // no budget left
        Team toTeam = buildTeam(2, seller, league, 0);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setPlayer(player);
        offer.setOfferPrice(500_000);
        offer.setLeague(league);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> tradeOfferService.acceptOffer(1L, seller))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("suficiente presupuesto");
    }

    @Test
    @DisplayName("acceptOffer: jugador ya no pertenece al vendedor → IllegalStateException")
    void acceptOffer_playerNoLongerInSellerTeam_throwsException() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 0);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setPlayer(player);
        offer.setOfferPrice(500_000);
        offer.setLeague(league);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(playerTeamRepository.findByTeam(toTeam)).thenReturn(Collections.emptyList()); // player gone

        assertThatThrownBy(() -> tradeOfferService.acceptOffer(1L, seller))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya no pertenece al equipo vendedor");
    }

    @Test
    @DisplayName("acceptOffer: otras ofertas pendientes del mismo jugador son rechazadas")
    void acceptOffer_otherPendingOffersForSamePlayer_areRejected() {
        User buyer = buildUser(1, "alice");
        User buyer2 = buildUser(3, "charlie");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team fromTeam2 = buildTeam(3, buyer2, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 5_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        TradeOffer acceptedOffer = new TradeOffer();
        acceptedOffer.setId(1L);
        acceptedOffer.setStatus(TradeOfferStatus.PENDING);
        acceptedOffer.setFromTeam(fromTeam);
        acceptedOffer.setToTeam(toTeam);
        acceptedOffer.setPlayer(player);
        acceptedOffer.setOfferPrice(500_000);
        acceptedOffer.setLeague(league);

        TradeOffer otherOffer = new TradeOffer();
        otherOffer.setId(2L);
        otherOffer.setStatus(TradeOfferStatus.PENDING);
        otherOffer.setFromTeam(fromTeam2);
        otherOffer.setToTeam(toTeam);
        otherOffer.setPlayer(player);
        otherOffer.setOfferPrice(400_000);
        otherOffer.setLeague(league);

        PlayerTeam pt = new PlayerTeam();
        pt.setPlayer(player);
        pt.setTeam(toTeam);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(acceptedOffer));
        when(playerTeamRepository.findByTeam(toTeam)).thenReturn(List.of(pt));
        when(playerTeamRepository.save(any())).thenReturn(pt);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeOfferRepository.findByLeagueAndPlayerAndStatus(league, player, TradeOfferStatus.PENDING))
                .thenReturn(List.of(acceptedOffer, otherOffer));
        doNothing().when(notificationService).createTradeResultNotification(any(), any(), any(), any(), any(), any(), anyBoolean());
        doNothing().when(notificationService).deleteTradeOfferNotification(any(), any());

        tradeOfferService.acceptOffer(1L, seller);

        // otherOffer should have been saved with REJECTED status
        verify(tradeOfferRepository, atLeast(2)).save(any(TradeOffer.class));
        assertThat(otherOffer.getStatus()).isEqualTo(TradeOfferStatus.REJECTED);
    }

    @Test
    @DisplayName("acceptOffer: budgets transferidos correctamente (comprador pierde, vendedor gana)")
    void acceptOffer_budgetsTransferredCorrectly() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 1_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);
        int price = 500_000;

        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setPlayer(player);
        offer.setOfferPrice(price);
        offer.setLeague(league);

        PlayerTeam pt = new PlayerTeam();
        pt.setPlayer(player);
        pt.setTeam(toTeam);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(playerTeamRepository.findByTeam(toTeam)).thenReturn(List.of(pt));
        when(playerTeamRepository.save(any())).thenReturn(pt);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeOfferRepository.findByLeagueAndPlayerAndStatus(league, player, TradeOfferStatus.PENDING))
                .thenReturn(Collections.emptyList());
        doNothing().when(notificationService).createTradeResultNotification(any(), any(), any(), any(), any(), any(), anyBoolean());
        doNothing().when(notificationService).deleteTradeOfferNotification(any(), any());

        tradeOfferService.acceptOffer(1L, seller);

        assertThat(fromTeam.getBudget()).isEqualTo(5_000_000 - price);
        assertThat(toTeam.getBudget()).isEqualTo(1_000_000 + price);
    }

    // -----------------------------------------------------------------------
    // rejectOffer – additional branches
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("rejectOffer: oferta no encontrada → IllegalStateException")
    void rejectOffer_offerNotFound_throwsException() {
        when(tradeOfferRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeOfferService.rejectOffer(99L, new User()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Oferta no encontrada");
    }

    @Test
    @DisplayName("rejectOffer: oferta ya no pendiente → IllegalStateException")
    void rejectOffer_notPending_throwsException() {
        User seller = buildUser(2, "bob");
        Team toTeam = buildTeam(2, seller, buildLeague(1), 0);
        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.CANCELLED);
        offer.setToTeam(toTeam);

        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> tradeOfferService.rejectOffer(1L, seller))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está pendiente");
    }

    @Test
    @DisplayName("rejectOffer: usuario no es el destinatario → IllegalStateException")
    void rejectOffer_wrongUser_throwsException() {
        User seller = buildUser(2, "bob");
        User wrongUser = buildUser(99, "eve");
        League league = buildLeague(1);
        Team toTeam = buildTeam(2, seller, league, 0);
        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setToTeam(toTeam);

        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> tradeOfferService.rejectOffer(1L, wrongUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tienes permiso para rechazar");
    }

    @Test
    @DisplayName("rejectOffer: rechazar emite notificación de resultado")
    void rejectOffer_sendTradeResultNotification() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 0);
        Team toTeam = buildTeam(2, seller, league, 0);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        TradeOffer offer = new TradeOffer();
        offer.setId(5L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setLeague(league);
        offer.setPlayer(player);
        offer.setOfferPrice(200_000);

        when(tradeOfferRepository.findById(5L)).thenReturn(Optional.of(offer));
        when(tradeOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).deleteTradeOfferNotification(any(), any());
        doNothing().when(notificationService).createTradeResultNotification(any(), any(), any(), any(), any(), any(), anyBoolean());

        tradeOfferService.rejectOffer(5L, seller);

        verify(notificationService).createTradeResultNotification(
                eq(league.getId()), eq(buyer), eq(seller), eq(player), eq(200_000), eq(5L), eq(false));
    }

    // -----------------------------------------------------------------------
    // cancelOffer
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("cancelOffer: oferta PENDING por su dueño → estado CANCELLED")
    void cancelOffer_byOwner_changeStatusToCancelled() {
        User buyer = buildUser(1, "alice");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 0);
        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);

        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(tradeOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradeOffer result = tradeOfferService.cancelOffer(1L, buyer);

        assertThat(result.getStatus()).isEqualTo(TradeOfferStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelOffer: oferta no encontrada → IllegalStateException")
    void cancelOffer_offerNotFound_throwsException() {
        when(tradeOfferRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeOfferService.cancelOffer(99L, new User()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Oferta no encontrada");
    }

    @Test
    @DisplayName("cancelOffer: oferta no pendiente → IllegalStateException")
    void cancelOffer_notPending_throwsException() {
        User buyer = buildUser(1, "alice");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 0);
        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.ACCEPTED);
        offer.setFromTeam(fromTeam);

        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> tradeOfferService.cancelOffer(1L, buyer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está pendiente");
    }

    @Test
    @DisplayName("cancelOffer: usuario no es el remitente → IllegalStateException")
    void cancelOffer_wrongUser_throwsException() {
        User buyer = buildUser(1, "alice");
        User other = buildUser(99, "eve");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 0);
        TradeOffer offer = new TradeOffer();
        offer.setId(1L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);

        when(tradeOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> tradeOfferService.cancelOffer(1L, other))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tienes permiso para cancelar");
    }

    // -----------------------------------------------------------------------
    // getIncomingOffers / getOutgoingOffers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getIncomingOffers: liga no encontrada → IllegalStateException")
    void getIncomingOffers_leagueNotFound_throwsException() {
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeOfferService.getIncomingOffers(new User(), 99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Liga no encontrada");
    }

    @Test
    @DisplayName("getIncomingOffers: usuario sin equipo en liga → lista vacía")
    void getIncomingOffers_noTeamInLeague_returnsEmptyList() {
        League league = buildLeague(1);
        User user = buildUser(1, "alice");

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(null);

        List<TradeOffer> result = tradeOfferService.getIncomingOffers(user, 1);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getIncomingOffers: usuario con equipo → delega en findByToTeam")
    void getIncomingOffers_withTeam_returnsDelegatedList() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league, 0);
        TradeOffer offer = new TradeOffer();

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(tradeOfferRepository.findByToTeam(team)).thenReturn(List.of(offer));

        List<TradeOffer> result = tradeOfferService.getIncomingOffers(user, 1);

        assertThat(result).hasSize(1);
        verify(tradeOfferRepository).findByToTeam(team);
    }

    @Test
    @DisplayName("getOutgoingOffers: usuario sin equipo en liga → lista vacía")
    void getOutgoingOffers_noTeamInLeague_returnsEmptyList() {
        League league = buildLeague(1);
        User user = buildUser(1, "alice");

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(null);

        List<TradeOffer> result = tradeOfferService.getOutgoingOffers(user, 1);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getOutgoingOffers: usuario con equipo → delega en findByFromTeam")
    void getOutgoingOffers_withTeam_returnsDelegatedList() {
        User user = buildUser(1, "alice");
        League league = buildLeague(1);
        Team team = buildTeam(1, user, league, 0);
        TradeOffer offer1 = new TradeOffer();
        TradeOffer offer2 = new TradeOffer();

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeagueAndUser(league, user)).thenReturn(team);
        when(tradeOfferRepository.findByFromTeam(team)).thenReturn(List.of(offer1, offer2));

        List<TradeOffer> result = tradeOfferService.getOutgoingOffers(user, 1);

        assertThat(result).hasSize(2);
        verify(tradeOfferRepository).findByFromTeam(team);
    }

    // -----------------------------------------------------------------------
    // getOutgoingOffers – league not found
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getOutgoingOffers: liga no encontrada → IllegalStateException")
    void getOutgoingOffers_leagueNotFound_throwsException() {
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeOfferService.getOutgoingOffers(new User(), 99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Liga no encontrada");
    }

    // -----------------------------------------------------------------------
    // acceptOffer – send notification after successful transfer
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("acceptOffer: aceptación exitosa → notificación de resultado llamada con isAccepted=true")
    void acceptOffer_success_sendsTradeResultNotificationAccepted() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 1_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);
        int price = 300_000;

        TradeOffer offer = new TradeOffer();
        offer.setId(10L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setPlayer(player);
        offer.setOfferPrice(price);
        offer.setLeague(league);

        PlayerTeam pt = new PlayerTeam();
        pt.setPlayer(player);
        pt.setTeam(toTeam);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(tradeOfferRepository.findById(10L)).thenReturn(Optional.of(offer));
        when(playerTeamRepository.findByTeam(toTeam)).thenReturn(List.of(pt));
        when(playerTeamRepository.save(any())).thenReturn(pt);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeOfferRepository.findByLeagueAndPlayerAndStatus(league, player, TradeOfferStatus.PENDING))
                .thenReturn(Collections.emptyList());
        doNothing().when(notificationService).deleteTradeOfferNotification(any(), any());
        doNothing().when(notificationService).createTradeResultNotification(any(), any(), any(), any(), any(), any(), anyBoolean());

        tradeOfferService.acceptOffer(10L, seller);

        verify(notificationService).createTradeResultNotification(
                eq(league.getId()), eq(buyer), eq(seller), eq(player), eq(price), eq(10L), eq(true));
    }

    // -----------------------------------------------------------------------
    // cancelOffer – notification NOT sent (no createTradeResultNotification)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("cancelOffer: cancelación exitosa → tradeOfferRepository.save llamado con estado CANCELLED")
    void cancelOffer_success_savesWithCancelledStatus() {
        User buyer = buildUser(1, "alice");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 0);
        TradeOffer offer = new TradeOffer();
        offer.setId(7L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);

        when(tradeOfferRepository.findById(7L)).thenReturn(Optional.of(offer));
        when(tradeOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        tradeOfferService.cancelOffer(7L, buyer);

        verify(tradeOfferRepository).save(argThat(o -> ((TradeOffer) o).getStatus() == TradeOfferStatus.CANCELLED));
    }

    // -----------------------------------------------------------------------
    // rejectOffer – playerTeam ownership NOT transferred on rejection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("rejectOffer: rechazo → playerTeamRepository.save NO llamado")
    void rejectOffer_rejection_playerTeamNotMoved() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 0);
        Team toTeam = buildTeam(2, seller, league, 0);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        TradeOffer offer = new TradeOffer();
        offer.setId(3L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setLeague(league);
        offer.setPlayer(player);
        offer.setOfferPrice(100_000);

        when(tradeOfferRepository.findById(3L)).thenReturn(Optional.of(offer));
        when(tradeOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).deleteTradeOfferNotification(any(), any());
        doNothing().when(notificationService).createTradeResultNotification(any(), any(), any(), any(), any(), any(), anyBoolean());

        tradeOfferService.rejectOffer(3L, seller);

        verify(playerTeamRepository, never()).save(any());
        verify(teamRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // createOffer – notification sent with correct parameters
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createOffer: oferta creada → notificación enviada al equipo destinatario")
    void createOffer_valid_sendsNotificationToToTeamUser() {
        User buyer = buildUser(1, "alice");
        User seller = buildUser(2, "bob");
        League league = buildLeague(1);
        Team fromTeam = buildTeam(1, buyer, league, 5_000_000);
        Team toTeam = buildTeam(2, seller, league, 5_000_000);
        Player player = buildPlayer("P1", Position.DEL, 1_000_000);

        when(gameweekStateService.isTeamsLocked()).thenReturn(false);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findById(2)).thenReturn(Optional.of(toTeam));
        when(teamRepository.findByLeagueAndUser(league, buyer)).thenReturn(fromTeam);
        when(playerRepository.findById("P1")).thenReturn(Optional.of(player));
        when(playerTeamRepository.existsByTeamAndPlayer(toTeam, player)).thenReturn(true);
        when(playerTeamRepository.existsByTeamAndPlayer(fromTeam, player)).thenReturn(false);
        when(tradeOfferRepository.save(any(TradeOffer.class))).thenAnswer(inv -> {
            TradeOffer offer = inv.getArgument(0);
            offer.setId(99L);
            return offer;
        });
        doNothing().when(notificationService).createTradeOfferNotification(any(), any(), any(), any(), any(), any());

        tradeOfferService.createOffer(buyer, 2, "P1", 400_000, 1);

        verify(notificationService).createTradeOfferNotification(
                eq(1), eq(buyer), eq(seller), eq(player), eq(400_000), eq(99L));
    }
}
