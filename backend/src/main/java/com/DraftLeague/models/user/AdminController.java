package com.DraftLeague.models.user;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.League.LeagueRepository;
import com.DraftLeague.models.Player.PlayerImportService;
import com.DraftLeague.models.Player.PlayerRepository;
import com.DraftLeague.models.Market.MarketService;
import com.DraftLeague.models.Match.MatchService;

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

    public AdminController(UserRepository userRepository, 
                          LeagueRepository leagueRepository,
                          PlayerRepository playerRepository,
                          MarketService marketService,
                          PlayerImportService importService,
                          UserService userService,
                          MatchService matchService) {
        this.userRepository = userRepository;
        this.leagueRepository = leagueRepository;
        this.playerRepository = playerRepository;
        this.marketService = marketService;
        this.importService = importService;
        this.userService = userService;
        this.matchService = matchService;
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
            return ResponseEntity.badRequest().body(Map.of("error", "Rol inválido"));
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
                System.err.println("Causa raíz: " + cause.getMessage());
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
            return ResponseEntity.ok(Map.of("message", "Partidos sincronizados exitosamente", "output", output));
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
            return ResponseEntity.ok(Map.of("message", "Jugadores sincronizados exitosamente", "output", output));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al sincronizar jugadores: " + e.getMessage()));
        }
    }
}
