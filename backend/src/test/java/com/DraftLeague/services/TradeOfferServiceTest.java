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

    // ─── createOffer ─────────────────────────────────────────────────────────────

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

    // ─── acceptOffer ─────────────────────────────────────────────────────────────

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

    // ─── rejectOffer ─────────────────────────────────────────────────────────────

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
}
