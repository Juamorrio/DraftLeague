package com.DraftLeague.controllers;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.Gameweek.GameweekState;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.services.PlayerImportService;
import com.DraftLeague.services.UserService;
import com.DraftLeague.services.GameweekStateService;
import com.DraftLeague.services.FantasyPointsService;
import com.DraftLeague.services.MarketValueUpdateService;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.MarketService;
import com.DraftLeague.services.MatchService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final LeagueRepository leagueRepository;
    private final PlayerRepository playerRepository;
    private final MarketService marketService;
    private final PlayerImportService importService;
    private final UserService userService;
    private final MatchService matchService;
    private final GameweekStateService gameweekStateService;
    private final FantasyPointsService fantasyPointsService;
    private final MarketValueUpdateService marketValueUpdateService;

    public AdminController(UserRepository userRepository, 
                          LeagueRepository leagueRepository,
                          PlayerRepository playerRepository,
                          MarketService marketService,
                          PlayerImportService importService,
                          UserService userService,
                          MatchService matchService,
                          GameweekStateService gameweekStateService,
                          FantasyPointsService fantasyPointsService,
                          MarketValueUpdateService marketValueUpdateService) {
        this.userRepository = userRepository;
        this.leagueRepository = leagueRepository;
        this.playerRepository = playerRepository;
        this.marketService = marketService;
        this.importService = importService;
        this.userService = userService;
        this.matchService = matchService;
        this.gameweekStateService = gameweekStateService;
        this.fantasyPointsService = fantasyPointsService;
        this.marketValueUpdateService = marketValueUpdateService;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String username = auth.getName();
        User user = userRepository.findUserByUsername(username).orElse(null);
        return user != null && "ADMIN".equals(user.getRole());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        long totalUsers = userRepository.count();
        long totalLeagues = leagueRepository.count();
        long totalPlayers = playerRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalLeagues", totalLeagues);
        stats.put("totalPlayers", totalPlayers);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Integer id, 
                                           @RequestBody Map<String, String> body,
                                           Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        String newRole = body.get("role");
        if (!"USER".equals(newRole) && !"ADMIN".equals(newRole)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol invÃƒÂ¡lido"));
        }

        user.setRole(newRole);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Rol actualizado", "user", user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id, Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado"));
    }

    @DeleteMapping("/leagues/{id}")
    public ResponseEntity<?> deleteLeague(@PathVariable Long id, Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        leagueRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Liga eliminada"));
    }

    @PostMapping("/market/{leagueId}/refresh")
    public ResponseEntity<?> refreshMarket(@PathVariable Integer leagueId, Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        try {
            marketService.finalizeExpiredAuctions();
            marketService.refreshMarket(leagueId);
            return ResponseEntity.ok(Map.of("message", "Mercado refrescado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al refrescar mercado: " + e.getMessage()));
        }
    }

    @GetMapping("/leagues")
    public ResponseEntity<?> getAllLeagues(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        List<League> leagues = leagueRepository.findAll();
        return ResponseEntity.ok(leagues);
    }

    @PostMapping("/import-players")
    public ResponseEntity<?> importPlayers(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        try {
            int count = importService.importFromJsonResource();
            return ResponseEntity.ok(Map.of("message", "Se importaron " + count + " jugadores."));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("===== ERROR DETALLADO AL IMPORTAR JUGADORES =====");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("Tipo: " + e.getClass().getName());
            
            Throwable cause = e.getCause();
            if (cause != null) {
                System.err.println("Causa raÃƒÂ­z: " + cause.getMessage());
                System.err.println("Tipo causa: " + cause.getClass().getName());
            }
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error al importar jugadores: " + e.getMessage(),
                "type", e.getClass().getSimpleName()
            ));
        }
    }

    @PostMapping("/sync-matches")
    public ResponseEntity<?> syncMatches(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        try {
            String output = matchService.syncMatches();
            return ResponseEntity.ok(Map.of(
                "message", "Partidos sincronizados correctamente.",
                "output", output
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al sincronizar partidos: " + e.getMessage()));
        }
    }

    @PostMapping("/sync-players")
    public ResponseEntity<?> syncPlayers(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }

        try {
            String output = importService.syncPlayers();
            return ResponseEntity.ok(Map.of(
                "message", "Jugadores sincronizados correctamente.",
                "output", output
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al sincronizar jugadores: " + e.getMessage()));
        }
    }

    /**
     * Manually triggers a full market value recalculation for all players.
     * POST /api/v1/admin/market/recalculate-prices
     */
    @PostMapping("/market/recalculate-prices")
    public ResponseEntity<?> recalculateMarketPrices(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            Map<String, Integer> result = marketValueUpdateService.recalculateAllMarketValues();
            return ResponseEntity.ok(Map.of(
                "message", "Precios de mercado recalculados correctamente.",
                "updatedCount", result.getOrDefault("updatedCount", 0),
                "skippedCount", result.getOrDefault("skippedCount", 0),
                "errorCount",   result.getOrDefault("errorCount",   0)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error al recalcular precios: " + e.getMessage()));
        }
    }

    /**
     * Manually triggers a market value recalculation for a single player.
     * POST /api/v1/admin/market/recalculate-prices/{playerId}
     */
    @PostMapping("/market/recalculate-prices/{playerId}")
    public ResponseEntity<?> recalculateMarketPriceForPlayer(
            @PathVariable String playerId, Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            com.DraftLeague.models.Player.Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Jugador no encontrado: " + playerId));
            int oldValue = player.getMarketValue();
            boolean changed = marketValueUpdateService.recalculateForPlayer(player);
            int newValue = playerRepository.findById(playerId)
                    .map(com.DraftLeague.models.Player.Player::getMarketValue)
                    .orElse(oldValue);
            return ResponseEntity.ok(Map.of(
                "playerId",  playerId,
                "oldValue",  oldValue,
                "newValue",  newValue,
                "changed",   changed
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    // ─── Gameweek State Management ────────────────────────────────────────────────

    /**
     * Returns the current global gameweek state (active gameweek and lock status).
     * GET /api/v1/admin/gameweek/status
     */
    @GetMapping("/gameweek/status")
    public ResponseEntity<?> getGameweekStatus(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        GameweekState state = gameweekStateService.getState();
        Map<String, Object> response = new HashMap<>();
        response.put("activeGameweek", state.getActiveGameweek());
        response.put("teamsLocked", state.getTeamsLocked());
        response.put("lockedAt", state.getLockedAt());
        response.put("unlockedAt", state.getUnlockedAt());
        return ResponseEntity.ok(response);
    }

    /**
     * Activates a gameweek for scoring and locks team modifications.
     * POST /api/v1/admin/gameweek/activate
     * Body: { "gameweek": 5 }
     */
    @PostMapping("/gameweek/activate")
    public ResponseEntity<?> activateGameweek(@RequestBody Map<String, Integer> body,
                                              Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        Integer gameweek = body.get("gameweek");
        if (gameweek == null || gameweek < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "Jornada inv\u00e1lida"));
        }
        try {
            GameweekState state = gameweekStateService.activateGameweek(gameweek);
            return ResponseEntity.ok(Map.of(
                "message", "Jornada " + gameweek + " activada. Equipos bloqueados.",
                "activeGameweek", state.getActiveGameweek(),
                "teamsLocked", state.getTeamsLocked()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Unlocks team modifications after a gameweek scoring round.
     * POST /api/v1/admin/gameweek/unlock
     */
    @PostMapping("/gameweek/unlock")
    public ResponseEntity<?> unlockTeams(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            GameweekState state = gameweekStateService.unlockTeams();
            return ResponseEntity.ok(Map.of(
                "message", "Equipos desbloqueados. Los usuarios pueden modificar sus equipos.",
                "teamsLocked", state.getTeamsLocked()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Calculates fantasy points for all teams for the active gameweek.
     * Can be called multiple times as match data arrives.
     * POST /api/v1/admin/gameweek/calculate-points
     */
    @PostMapping("/gameweek/calculate-points")
    public ResponseEntity<?> calculateGameweekPoints(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        Integer activeGameweek = gameweekStateService.getActiveGameweek();
        if (activeGameweek == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "No hay ninguna jornada activa. Activa una jornada primero."));
        }
        try {
            // Step 1: fetch stats from API-Football and save to DB
            importService.syncGameweekStats(activeGameweek);
            // Step 2: calculate fantasy points for all teams
            fantasyPointsService.recalculateGameweekPoints(activeGameweek);
            // Step 3: recalculate market values and record per-gameweek history
            Map<String, Integer> mvResult = marketValueUpdateService.recalculateAllMarketValuesForGameweek(activeGameweek);
            return ResponseEntity.ok(Map.of(
                "message", "Estad\u00edsticas obtenidas, puntos y valores de mercado de la jornada " + activeGameweek + " calculados correctamente.",
                "gameweek", activeGameweek,
                "marketValueUpdated", mvResult.getOrDefault("updatedCount", 0),
                "marketValueSkipped", mvResult.getOrDefault("skippedCount", 0),
                "marketValueErrors",  mvResult.getOrDefault("errorCount", 0)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al calcular puntos: " + e.getMessage()));
        }
    }
}
