package com.DraftLeague.models.Player;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.DraftLeague.models.user.User;
import com.DraftLeague.models.user.UserRepository;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {
    
    private final PlayerService playerService;
    private final UserRepository userRepository;

    public PlayerController(PlayerService playerService, UserRepository userRepository) {
        this.playerService = playerService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers(
            @RequestParam(required = false) Integer leagueId,
            @RequestParam(required = false) Boolean onlyOwned) {
        
        if (leagueId != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                User user = userRepository.findUserByUsername(username).orElse(null);
                
                if (user != null) {
                    if (Boolean.TRUE.equals(onlyOwned)) {
                        return ResponseEntity.ok(playerService.getPlayersByUserAndLeague(user, leagueId));
                    } else {
                        return ResponseEntity.ok(playerService.getAvailablePlayersForUserInLeague(user, leagueId));
                    }
                }
            }
        }
        
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
        Player createdPlayer = playerService.createPlayer(player);
        return ResponseEntity.ok(createdPlayer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayerById(@PathVariable String id) {
        Player player = playerService.getPlayerById(id);
        return ResponseEntity.ok(player);
    }

    //@PutMapping("/{id}")
    //public ResponseEntity<Player> updatePlayer(@PathVariable Long id, @RequestBody Player player) {
        //Player updatedPlayer = playerService.updatePlayer(id, player);
        //return ResponseEntity.ok(updatedPlayer);
    //}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/purchase")
    public ResponseEntity<?> purchasePlayer(
            @RequestParam String playerId,
            @RequestParam Integer leagueId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }
            
            String username = auth.getName();
            User user = userRepository.findUserByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
            
            playerService.purchasePlayer(playerId, leagueId, user);
            return ResponseEntity.ok().body(Map.of("message", "Jugador comprado exitosamente"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al comprar jugador: " + e.getMessage()));
        }
    }

    @GetMapping("/load-image-team-player")
    public ResponseEntity<?> loadImageTeamPlayer(
            @RequestParam String teamId) {
        try {
            byte[] imageBytes = playerService.fetchPlayerTeamImage(teamId);
            return ResponseEntity.ok(Map.of("imageBytes", imageBytes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cargar imagen: " + e.getMessage()));
        }
    }

}