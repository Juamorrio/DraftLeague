package com.DraftLeague.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.DraftLeague.dto.CreateLeagueRequest;
import com.DraftLeague.models.League.League;
import com.DraftLeague.services.LeagueService;

@RestController
@RequestMapping("/api/v1/leagues")
public class LeagueController {

    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @PostMapping
    public ResponseEntity<League> createLeague(@Valid @RequestBody CreateLeagueRequest request) {
        League createdLeague = leagueService.createLeague(request);
        return ResponseEntity.ok(createdLeague);
    }

    @GetMapping("/{id}")
    public ResponseEntity<League> getLeagueById(@PathVariable Long id) {
        League league = leagueService.getLeagueById(id);
        return ResponseEntity.ok(league);
    }

    @GetMapping
    public ResponseEntity<List<League>> getAllLeagues() {
        List<League> leagues = leagueService.getAllLeagues();
        return ResponseEntity.ok(leagues);
    }

    @PutMapping("/{id}")
    public ResponseEntity<League> updateLeague(@PathVariable Long id, @RequestBody League league) {
        League updatedLeague = leagueService.updateLeague(id, league);
        return ResponseEntity.ok(updatedLeague);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeague(@PathVariable Long id) {
        leagueService.deleteLeague(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{leagueId}/ranking")
    public ResponseEntity<List<Map<String,Object>>> getRanking(@PathVariable Long leagueId) {
        return ResponseEntity.ok(leagueService.getRanking(leagueId));
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinLeague(@RequestBody Map<String,String> body) {
        try {
            String code = body.get("code");
            League league = leagueService.joinLeagueByCode(code);
            return ResponseEntity.ok(Map.of(
                "id", league.getId(),
                "code", league.getCode(),
                "name", league.getName()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
}
