package com.DraftLeague.services;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Trade.TradeOffer;
import com.DraftLeague.models.Trade.TradeOfferStatus;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.PlayerTeamRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.TradeOfferRepository;
import com.DraftLeague.repositories.UserRepository;

@Service
public class TradeOfferService {

    private final TradeOfferRepository tradeOfferRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GameweekStateService gameweekStateService;

    public TradeOfferService(TradeOfferRepository tradeOfferRepository,
                             TeamRepository teamRepository,
                             LeagueRepository leagueRepository,
                             PlayerRepository playerRepository,
                             PlayerTeamRepository playerTeamRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             GameweekStateService gameweekStateService) {
        this.tradeOfferRepository = tradeOfferRepository;
        this.teamRepository = teamRepository;
        this.leagueRepository = leagueRepository;
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.gameweekStateService = gameweekStateService;
    }

    @Transactional
    public TradeOffer createOffer(User buyer, Integer toTeamId, String playerId, Integer offerPrice, Integer leagueId) {
        if (gameweekStateService.isTeamsLocked()) {
            throw new IllegalStateException("Las modificaciones de equipo están bloqueadas durante la jornada activa");
        }

        League league = leagueRepository.findById(leagueId.longValue())
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));

        Team toTeam = teamRepository.findById(toTeamId)
                .orElseThrow(() -> new IllegalStateException("Equipo destinatario no encontrado"));

        if (!toTeam.getLeague().getId().equals(league.getId())) {
            throw new IllegalStateException("El equipo no pertenece a esta liga");
        }

        Team fromTeam = teamRepository.findByLeagueAndUser(league, buyer);
        if (fromTeam == null) {
            throw new IllegalStateException("No tienes un equipo en esta liga");
        }

        if (fromTeam.getId().equals(toTeam.getId())) {
            throw new IllegalStateException("No puedes hacerte una oferta a ti mismo");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalStateException("Jugador no encontrado"));

        boolean playerOwnedByToTeam = playerTeamRepository.existsByTeamAndPlayer(toTeam, player);
        if (!playerOwnedByToTeam) {
            throw new IllegalStateException("El jugador no pertenece al equipo receptor");
        }

        boolean alreadyOwnedByBuyer = playerTeamRepository.existsByTeamAndPlayer(fromTeam, player);
        if (alreadyOwnedByBuyer) {
            throw new IllegalStateException("Ya posees este jugador");
        }

        if (fromTeam.getBudget() < offerPrice) {
            throw new IllegalStateException("Presupuesto insuficiente para realizar la oferta");
        }

        if (offerPrice <= 0) {
            throw new IllegalStateException("El precio de la oferta debe ser mayor que 0");
        }

        TradeOffer offer = new TradeOffer();
        offer.setFromTeam(fromTeam);
        offer.setToTeam(toTeam);
        offer.setPlayer(player);
        offer.setLeague(league);
        offer.setOfferPrice(offerPrice);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setCreatedAt(new Date());

        TradeOffer saved = tradeOfferRepository.save(offer);

        notificationService.createTradeOfferNotification(
                leagueId, buyer, toTeam.getUser(), player, offerPrice, saved.getId());

        return saved;
    }

    @Transactional
    public TradeOffer acceptOffer(Long offerId, User acceptingUser) {
        if (gameweekStateService.isTeamsLocked()) {
            throw new IllegalStateException("Las modificaciones de equipo están bloqueadas durante la jornada activa");
        }

        TradeOffer offer = tradeOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalStateException("Oferta no encontrada"));

        if (!offer.getStatus().equals(TradeOfferStatus.PENDING)) {
            throw new IllegalStateException("La oferta ya no está pendiente");
        }

        assertUserIsRecipient(offer, acceptingUser.getId(), "aceptar");

        Team fromTeam = offer.getFromTeam();
        Team toTeam = offer.getToTeam();
        Player player = offer.getPlayer();
        Integer price = offer.getOfferPrice();

        if (fromTeam.getBudget() < price) {
            throw new IllegalStateException("El comprador ya no tiene suficiente presupuesto");
        }

        // Transfer the PlayerTeam from toTeam to fromTeam
        PlayerTeam playerTeam = playerTeamRepository.findByTeam(toTeam).stream()
                .filter(pt -> pt.getPlayer().getId().equals(player.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El jugador ya no pertenece al equipo vendedor"));

        playerTeam.setTeam(fromTeam);
        playerTeam.setIsCaptain(false);
        playerTeam.setLined(false);
        playerTeam.setBuyPrice(price);
        playerTeam.setSellPrice(price);
        playerTeamRepository.save(playerTeam);

        fromTeam.setBudget(fromTeam.getBudget() - price);
        toTeam.setBudget(toTeam.getBudget() + price);
        teamRepository.save(fromTeam);
        teamRepository.save(toTeam);

        offer.setStatus(TradeOfferStatus.ACCEPTED);
        tradeOfferRepository.save(offer);

        notificationService.deleteTradeOfferNotification(
            offer.getLeague().getId(),
            offer.getId());

      
        List<TradeOffer> otherOffers = tradeOfferRepository.findByLeagueAndPlayerAndStatus(
                offer.getLeague(), player, TradeOfferStatus.PENDING);
        for (TradeOffer other : otherOffers) {
            if (!other.getId().equals(offerId)) {
                other.setStatus(TradeOfferStatus.REJECTED);
                tradeOfferRepository.save(other);
            }
        }

        notificationService.createTradeResultNotification(
                offer.getLeague().getId(),
                fromTeam.getUser(), toTeam.getUser(), player, price, offerId, true);

        return offer;
    }

    @Transactional
    public TradeOffer rejectOffer(Long offerId, User rejectingUser) {
        TradeOffer offer = tradeOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalStateException("Oferta no encontrada"));

        if (!offer.getStatus().equals(TradeOfferStatus.PENDING)) {
            throw new IllegalStateException("La oferta ya no está pendiente");
        }

        assertUserIsRecipient(offer, rejectingUser.getId(), "rechazar");

        offer.setStatus(TradeOfferStatus.REJECTED);
        tradeOfferRepository.save(offer);

        notificationService.deleteTradeOfferNotification(
            offer.getLeague().getId(),
            offer.getId());

        notificationService.createTradeResultNotification(
                offer.getLeague().getId(),
                offer.getFromTeam().getUser(), offer.getToTeam().getUser(),
                offer.getPlayer(), offer.getOfferPrice(), offerId, false);

        return offer;
    }

    @Transactional
    public TradeOffer cancelOffer(Long offerId, User cancellingUser) {
        TradeOffer offer = tradeOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalStateException("Oferta no encontrada"));

        if (!offer.getStatus().equals(TradeOfferStatus.PENDING)) {
            throw new IllegalStateException("La oferta ya no está pendiente");
        }

        if (!offer.getFromTeam().getUser().getId().equals(cancellingUser.getId())) {
            throw new IllegalStateException("No tienes permiso para cancelar esta oferta");
        }

        offer.setStatus(TradeOfferStatus.CANCELLED);
        return tradeOfferRepository.save(offer);
    }

    @Transactional(readOnly = true)
    public List<TradeOffer> getIncomingOffers(User user, Integer leagueId) {
        League league = leagueRepository.findById(leagueId.longValue())
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));
        Team team = teamRepository.findByLeagueAndUser(league, user);
        if (team == null) return List.of();
        return tradeOfferRepository.findByToTeam(team);
    }

    @Transactional(readOnly = true)
    public List<TradeOffer> getOutgoingOffers(User user, Integer leagueId) {
        League league = leagueRepository.findById(leagueId.longValue())
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));
        Team team = teamRepository.findByLeagueAndUser(league, user);
        if (team == null) return List.of();
        return tradeOfferRepository.findByFromTeam(team);
    }

    private void assertUserIsRecipient(TradeOffer offer, Integer userId, String action) {
        if (!offer.getToTeam().getUser().getId().equals(userId)) {
            throw new IllegalStateException("No tienes permiso para " + action + " esta oferta");
        }
    }
}
