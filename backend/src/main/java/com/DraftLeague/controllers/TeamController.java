package com.DraftLeague.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.DraftLeague.dto.UpdateTeamPlayersRequest;

import jakarta.validation.Valid;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.League.League;
import com.DraftLeague.services.TeamService;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {
    
    private final TeamService teamService;

    @Autowired
    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/{id}")
    public Team getTeamById(@PathVariable Integer id) {
        return teamService.getTeamById(id);
    }

    @GetMapping("/league/{leagueId}/{userId}")
    public ResponseEntity<Team> getTeamByLeague(@PathVariable Integer leagueId, @PathVariable Integer userId) {
        try {
            Team team = teamService.getTeamByUserAndLeague(leagueId, userId);
            return ResponseEntity.ok(team);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Team createTeam(@RequestBody Team team) {
        return teamService.postTeam(team);
    }

    @PostMapping("/{id}/update")
    public Team updateTeam(@PathVariable Integer id, @RequestBody Team team) {
        return teamService.updateTeam(team, id);
    }

    @PutMapping("/league/{leagueId}/{userId}/players")
    public ResponseEntity<?> updateTeamPlayers(
            @PathVariable Integer leagueId, 
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateTeamPlayersRequest request) {
        try {
            Team updatedTeam = teamService.updateTeamPlayers(leagueId, userId, request);
            return ResponseEntity.ok(updatedTeam);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-team/{leagueId}/{userId}")
    public ResponseEntity<Team> getMyTeam(@PathVariable Integer leagueId, @PathVariable Integer userId) {
        try {
            Team team = teamService.getTeamByUserAndLeague(leagueId, userId);
            return ResponseEntity.ok(team);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/league/{leagueId}/buyout")
    public ResponseEntity<?> buyoutPlayer(
            @PathVariable Integer leagueId,
            @RequestBody java.util.Map<String,Object> body) {
        try {
            Integer sellerUserId = (Integer) body.get("sellerUserId");
            String playerId = (String) body.get("playerId");
            if (sellerUserId == null || playerId == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Par\u00e1metros inv\u00e1lidos"));
            }
            Team buyerTeam = teamService.buyoutPlayer(leagueId, sellerUserId, playerId);
            return ResponseEntity.ok(java.util.Map.of(
                "teamId", buyerTeam.getId(),
                "budget", buyerTeam.getBudget()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/league/{leagueId}/sell-player")
    public ResponseEntity<?> sellPlayer(
            @PathVariable Integer leagueId,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String playerId = body.get("playerId");
            if (playerId == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "playerId requerido"));
            }
            java.util.Map<String, Object> result = teamService.sellPlayer(leagueId, playerId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/league/{leagueId}/{userId}/wildcard")
    public ResponseEntity<?> useWildcard(
            @PathVariable Integer leagueId,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateTeamPlayersRequest request) {
        try {
            Team updatedTeam = teamService.useWildcard(leagueId, userId, request);
            return ResponseEntity.ok(java.util.Map.of(
                "teamId", updatedTeam.getId(),
                "wildcardUsed", true
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/league/{leagueId}/{userId}/chip")
    public ResponseEntity<?> activateChip(
            @PathVariable Integer leagueId,
            @PathVariable Integer userId,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String chip = body.get("chip");
            if (chip == null || chip.isBlank()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "chip requerido"));
            }
            Team updatedTeam = teamService.activateChip(leagueId, userId, chip);
            return ResponseEntity.ok(java.util.Map.of(
                "activeChip", updatedTeam.getActiveChip() != null ? updatedTeam.getActiveChip() : "",
                "usedChips", updatedTeam.getUsedChips() != null ? updatedTeam.getUsedChips() : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/league/{leagueId}/{userId}/chip")
    public ResponseEntity<?> cancelChip(
            @PathVariable Integer leagueId,
            @PathVariable Integer userId) {
        try {
            Team updatedTeam = teamService.cancelChip(leagueId, userId);
            return ResponseEntity.ok(java.util.Map.of(
                "activeChip", "",
                "usedChips", updatedTeam.getUsedChips() != null ? updatedTeam.getUsedChips() : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
