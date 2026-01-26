package com.DraftLeague.models.Market;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
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
            List<MarketPlayer> players = marketService.getAvailableMarketPlayers(leagueId);
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
}
