package com.DraftLeague.models.Match;

import com.DraftLeague.models.Match.dto.MatchDTO;
import com.DraftLeague.models.Match.dto.UpcomingMatchDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/played")
    public ResponseEntity<Map<String, List<MatchDTO>>> getPlayedMatches() {
        return ResponseEntity.ok(matchService.getPlayedMatchesFromDB());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, List<UpcomingMatchDTO>>> getUpcomingMatches() {
        return ResponseEntity.ok(matchService.getUpcomingMatchesFromDB());
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importMatches() {
        try {
            String result = matchService.importMatchesFromJson();
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/{fixtureId}")
    public ResponseEntity<Match> getMatchByFixtureId(@PathVariable Integer fixtureId) {
        Match match = matchService.getMatchByFixtureId(fixtureId);
        if (match != null) {
            return ResponseEntity.ok(match);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{matchId}/teams")
    public ResponseEntity<List<String>> getTeamNamesByMatchId(@PathVariable Integer matchId) {
        try {
            List<String> teamNames = matchService.getNameTeamMatch(matchId);
            return ResponseEntity.ok(teamNames);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

