package com.DraftLeague.models.Match;

import com.DraftLeague.models.Match.dto.MatchDTO;
import com.DraftLeague.models.Match.dto.UpcomingMatchDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.ok(matchService.getPlayedMatches());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, List<UpcomingMatchDTO>>> getUpcomingMatches() {
        return ResponseEntity.ok(matchService.getUpcomingMatches());
    }
}
