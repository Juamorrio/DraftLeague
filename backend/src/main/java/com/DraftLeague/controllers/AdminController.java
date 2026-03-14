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
import com.DraftLeague.services.LeagueService;
import com.DraftLeague.services.PlayerStatisticsService;

import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

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
    private final LeagueService leagueService;
    private final PlayerStatisticsService playerStatisticsService;

    public AdminController(UserRepository userRepository,
                          LeagueRepository leagueRepository,
                          PlayerRepository playerRepository,
                          MarketService marketService,
                          PlayerImportService importService,
                          UserService userService,
                          MatchService matchService,
                          GameweekStateService gameweekStateService,
                          FantasyPointsService fantasyPointsService,
                          MarketValueUpdateService marketValueUpdateService,
                          LeagueService leagueService,
                          PlayerStatisticsService playerStatisticsService) {
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
        this.leagueService = leagueService;
        this.playerStatisticsService = playerStatisticsService;
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
            return ResponseEntity.badRequest().body(Map.of("error", "Rol inv\u00e1lido"));
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

        leagueService.deleteLeague(id);
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

        List<Map<String, Object>> result = leagueRepository.findAll().stream()
            .map(l -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", l.getId());
                m.put("name", l.getName());
                m.put("code", l.getCode());
                m.put("maxTeams", l.getMaxTeams());
                m.put("initialBudget", l.getInitialBudget());
                return m;
            })
            .toList();
        return ResponseEntity.ok(result);
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
            log.error("Error importing players", e);
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
            int statsSynced = importService.syncGameweekStats(activeGameweek);

            // Step 2: calculate fantasy points for all teams
            fantasyPointsService.recalculateGameweekPoints(activeGameweek);

            // Step 3: recalculate market values and record per-gameweek history
            Map<String, Integer> mvResult = marketValueUpdateService.recalculateAllMarketValuesForGameweek(activeGameweek);

            // Build response – include a warning when the API returned no stats so the
            // admin knows why market values and fantasy points may not have changed.
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("gameweek",            activeGameweek);
            body.put("statsPlayersSynced",  statsSynced);
            body.put("marketValueUpdated",  mvResult.getOrDefault("updatedCount", 0));
            body.put("marketValueSkipped",  mvResult.getOrDefault("skippedCount", 0));
            body.put("marketValueErrors",   mvResult.getOrDefault("errorCount", 0));

            if (statsSynced == 0) {
                body.put("warning",
                    "La API-Football no devolvió estadísticas para la jornada " + activeGameweek +
                    ". Verifica que la jornada esté finalizada, que el fixture ID sea correcto " +
                    "y que no hayas excedido el límite de tu plan. Los valores de mercado se " +
                    "recalcularon usando únicamente los datos ya existentes en la base de datos.");
                body.put("message", "Jornada " + activeGameweek +
                    " procesada con advertencia: 0 estadísticas obtenidas del API.");
            } else {
                body.put("message", "Estadísticas (" + statsSynced +
                    " registros), puntos y valores de mercado de la jornada " +
                    activeGameweek + " calculados correctamente.");
            }

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al calcular puntos: " + e.getMessage()));
        }
    }

    @PostMapping("/backfill-clean-sheets")
    public ResponseEntity<?> backfillCleanSheets(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            int updated = playerStatisticsService.recalculateCleanSheets();
            return ResponseEntity.ok(Map.of(
                "message", "Clean sheets retroactively calculated for " + updated + " statistics.",
                "updated", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al backfill clean sheets: " + e.getMessage()));
        }
    }
}
