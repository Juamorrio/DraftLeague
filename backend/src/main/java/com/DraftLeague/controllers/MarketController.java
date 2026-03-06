package com.DraftLeague.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.MarketService;
import com.DraftLeague.dto.MarketPlayerDTO;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketService marketService;
    private final UserRepository userRepository;

    public MarketController(MarketService marketService, UserRepository userRepository) {
        this.marketService = marketService;
        this.userRepository = userRepository;
    }

    @PostMapping("/initialize")
    public ResponseEntity<?> initializeMarket(
            @RequestParam Integer leagueId
    ) {
        try {
            marketService.initializeMarket(leagueId);
            return ResponseEntity.ok(Map.of("message", "Mercado inicializado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMarketPlayers(@RequestParam Integer leagueId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No autenticado"));
            }
            String username = auth.getName();
            List<MarketPlayerDTO> players = marketService.getAvailableMarketPlayersForUser(leagueId, username);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bid")
    public ResponseEntity<?> placeBid(
            @RequestParam Integer marketPlayerId,
            @RequestParam Long bidAmount
    ) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No autenticado"));
            }
            String username = auth.getName();
            marketService.placeBid(marketPlayerId, username, bidAmount);
            return ResponseEntity.ok(Map.of("message", "Puja realizada"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/finalize/{marketPlayerId}")
    public ResponseEntity<?> finalizeAuction(@PathVariable Integer marketPlayerId) {
        try {
            marketService.finalizeAuction(marketPlayerId);
            return ResponseEntity.ok(Map.of("message", "Subasta finalizada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshMarket(@RequestParam Integer leagueId) {
        try {
            marketService.refreshMarket(leagueId);
            return ResponseEntity.ok(Map.of("message", "Mercado refrescado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/finalize-expired")
    public ResponseEntity<?> finalizeExpiredAuctions() {
        try {
            marketService.finalizeExpiredAuctions();
            return ResponseEntity.ok(Map.of("message", "Subastas expiradas finalizadas"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/cancel-bid")
    public ResponseEntity<?> cancelBid(@RequestParam Integer marketPlayerId, Authentication auth) {
        try {
            String username = auth.getName();
            User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            marketService.cancelBid(marketPlayerId, user);
            return ResponseEntity.ok(Map.of("message", "Puja cancelada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
