package com.DraftLeague.models.Team;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.DraftLeague.models.Team.dto.UpdateTeamPlayersRequest;

import jakarta.validation.Valid;

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
}