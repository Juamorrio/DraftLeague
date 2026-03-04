package com.DraftLeague.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.DraftLeague.models.Trade.TradeOffer;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.TradeOfferService;

@RestController
@RequestMapping("/api/v1/trade-offers")
public class TradeOfferController {

    private final TradeOfferService tradeOfferService;
    private final UserRepository userRepository;

    public TradeOfferController(TradeOfferService tradeOfferService, UserRepository userRepository) {
        this.tradeOfferService = tradeOfferService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }

    /**
     * Create a trade offer.
     * Body: { toTeamId, playerId, offerPrice, leagueId }
     */
    @PostMapping
    public ResponseEntity<?> createOffer(@RequestBody Map<String, Object> body) {
        try {
            Integer toTeamId = (Integer) body.get("toTeamId");
            String playerId = (String) body.get("playerId");
            Integer offerPrice = (Integer) body.get("offerPrice");
            Integer leagueId = (Integer) body.get("leagueId");

            if (toTeamId == null || playerId == null || offerPrice == null || leagueId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parámetros incompletos"));
            }

            User buyer = getCurrentUser();
            TradeOffer offer = tradeOfferService.createOffer(buyer, toTeamId, playerId, offerPrice, leagueId);
            return ResponseEntity.ok(Map.of(
                "offerId", offer.getId(),
                "status", offer.getStatus().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Accept a trade offer (called by the seller/toTeam owner).
     */
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptOffer(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            TradeOffer offer = tradeOfferService.acceptOffer(id, user);
            return ResponseEntity.ok(Map.of("status", offer.getStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject a trade offer (called by the seller/toTeam owner).
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectOffer(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            TradeOffer offer = tradeOfferService.rejectOffer(id, user);
            return ResponseEntity.ok(Map.of("status", offer.getStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancel a trade offer (called by the buyer/fromTeam owner).
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOffer(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            TradeOffer offer = tradeOfferService.cancelOffer(id, user);
            return ResponseEntity.ok(Map.of("status", offer.getStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all incoming offers (offers on your players) for a league.
     */
    @GetMapping("/league/{leagueId}/incoming")
    public ResponseEntity<?> getIncomingOffers(@PathVariable Integer leagueId) {
        try {
            User user = getCurrentUser();
            List<TradeOffer> offers = tradeOfferService.getIncomingOffers(user, leagueId);
            return ResponseEntity.ok(offers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all outgoing offers (offers you made) for a league.
     */
    @GetMapping("/league/{leagueId}/outgoing")
    public ResponseEntity<?> getOutgoingOffers(@PathVariable Integer leagueId) {
        try {
            User user = getCurrentUser();
            List<TradeOffer> offers = tradeOfferService.getOutgoingOffers(user, leagueId);
            return ResponseEntity.ok(offers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
