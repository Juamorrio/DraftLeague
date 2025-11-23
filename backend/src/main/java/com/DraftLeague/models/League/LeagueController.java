package com.DraftLeague.models.League;

import java.util.List;

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
import com.DraftLeague.models.League.dto.CreateLeagueRequest;

@RestController
@RequestMapping({"/api/v1/leagues","/api/v1"})
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

    @GetMapping("/users/{userId}/leagues")
    public ResponseEntity<List<League>> getLeaguesByUserId(@PathVariable Integer userId) {
        List<League> leagues = leagueService.getLeaguesByUserId(userId);
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
    
    @GetMapping("/leagues/{leagueId}/ranking")
    public ResponseEntity<List<java.util.Map<String,Object>>> getRanking(@PathVariable Long leagueId) {
        return ResponseEntity.ok(leagueService.getRanking(leagueId));
    }

    @PostMapping("/leagues/join")
    public ResponseEntity<?> joinLeague(@RequestBody java.util.Map<String,String> body) {
        try {
            String code = body.get("code");
            League league = leagueService.joinLeagueByCode(code);
            return ResponseEntity.ok(java.util.Map.of(
                "id", league.getId(),
                "code", league.getCode(),
                "name", league.getName()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
}
