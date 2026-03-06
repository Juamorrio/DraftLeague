package com.DraftLeague.controllers;

import com.DraftLeague.dto.JornadaMatchesDTO;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.services.PlayerStatisticService;
import com.DraftLeague.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class PlayerStatisticController {

    private final PlayerStatisticService playerStatisticService;

    @PostMapping
    public ResponseEntity<PlayerStatistic> createStatistic(@RequestBody PlayerStatistic statistic) {
        try {
            PlayerStatistic saved = playerStatisticService.saveStatistic(statistic);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> createBulkStatistics(@RequestBody List<Map<String, Object>> statisticsData) {
        try {
            List<PlayerStatistic> saved = playerStatisticService.saveBulkFromJson(statisticsData);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Statistics saved successfully",
                "count", saved.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error saving statistics",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<PlayerStatistic>> getPlayerStatistics(@PathVariable String playerId) {
        try {
            List<PlayerStatistic> statistics = playerStatisticService.getPlayerStatistics(playerId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<PlayerStatistic>> getMatchStatistics(@PathVariable Integer matchId) {
        try {
            List<PlayerStatistic> statistics = playerStatisticService.getMatchStatistics(matchId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/player/{playerId}/match/{matchId}")
    public ResponseEntity<?> getPlayerMatchStatistic(
            @PathVariable String playerId,
            @PathVariable Integer matchId) {
        try {
            Object statistic = playerStatisticService.getPlayerMatchStatistic(playerId, matchId);
            if (statistic != null) {
                return ResponseEntity.ok(statistic);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/player/{playerId}/summary")
    public ResponseEntity<PlayerStatisticsSummaryDTO> getPlayerStatisticsSummary(@PathVariable String playerId) {
        try {
            PlayerStatisticsSummaryDTO summary = playerStatisticService.getPlayerStatisticsSummary(playerId);
            if (summary != null) {
                return ResponseEntity.ok(summary);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace(); 
            System.err.println("Error getting player statistics summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/player/{playerId}/matches")
    public ResponseEntity<List<JornadaMatchesDTO>> getPlayerMatchesSummary(@PathVariable String playerId) {
        try {
            List<JornadaMatchesDTO> matches = playerStatisticService.getPlayerMatchesSummary(playerId);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error getting player matches summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
