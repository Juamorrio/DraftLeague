package com.DraftLeague.controllers;

import com.DraftLeague.dto.PlayerPredictionDTO;
import com.DraftLeague.dto.TeamPredictionDTO;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.services.FantasyPointsService;
import com.DraftLeague.services.PlayerPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MLController {

    private final PlayerPredictionService playerPredictionService;
    private final FantasyPointsService fantasyPointsService;
    private final MatchRepository matchRepository;

    /**
     * GET /api/ml/predict/player/{playerId}
     * Returns predicted fantasy points for a single player's next match.
     */
    @GetMapping("/predict/player/{playerId}")
    public ResponseEntity<PlayerPredictionDTO> predictPlayer(@PathVariable String playerId) {
        PlayerPredictionDTO prediction = playerPredictionService.predictForPlayer(playerId);
        return ResponseEntity.ok(prediction);
    }

    /**
     * GET /api/ml/predict/team/{teamId}
     * Returns predicted fantasy points aggregated for a full team.
     */
    @GetMapping("/predict/team/{teamId}")
    public ResponseEntity<TeamPredictionDTO> predictTeam(@PathVariable Integer teamId) {
        TeamPredictionDTO prediction = playerPredictionService.predictForTeam(teamId);
        return ResponseEntity.ok(prediction);
    }

    /**
     * GET /api/ml/next-round
     * Returns all UPCOMING matches for the next (lowest) round number.
     */
    @GetMapping("/next-round")
    public ResponseEntity<Map<String, Object>> getNextRound() {
        List<Match> upcoming = matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING);

        if (upcoming.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "round", 0,
                    "matches", List.of()
            ));
        }

        int nextRound = upcoming.get(0).getRound();
        List<Match> nextRoundMatches = upcoming.stream()
                .filter(m -> nextRound == m.getRound())
                .collect(Collectors.toList());

        List<Map<String, Object>> matchSummaries = nextRoundMatches.stream()
                .map(m -> Map.<String, Object>of(
                        "id",       m.getId(),
                        "round",    m.getRound(),
                        "homeClub", m.getHomeClub(),
                        "awayClub", m.getAwayClub(),
                        "date",     m.getMatchDate() != null ? m.getMatchDate() : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "round",   nextRound,
                "matches", matchSummaries
        ));
    }

    /**
     * POST /api/ml/recalculate-fantasy-points
     * Triggers a full recalculation of all player total points and invalidates
     * the prediction cache so the next GET returns fresh predictions.
     */
    @PostMapping("/recalculate-fantasy-points")
    public ResponseEntity<Map<String, String>> recalculateFantasyPoints() {
        fantasyPointsService.updateAllPlayerPoints();
        playerPredictionService.invalidateCache();
        return ResponseEntity.ok(Map.of("status", "recalculated"));
    }
}
