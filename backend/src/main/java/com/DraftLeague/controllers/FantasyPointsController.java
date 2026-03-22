package com.DraftLeague.controllers;
import com.DraftLeague.models.Team.TeamGameweekPoints;
import com.DraftLeague.models.Team.TeamPlayerGameweekPoints;

import com.DraftLeague.dto.*;
import com.DraftLeague.services.FantasyPointsService;
import com.DraftLeague.services.GameweekStateService;
import com.DraftLeague.models.Team.*;
import com.DraftLeague.models.League.*;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.repositories.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import com.DraftLeague.repositories.TeamGameweekPointsRepository;
import com.DraftLeague.repositories.TeamPlayerGameweekPointsRepository;

@RestController
@RequestMapping("/api/v1/fantasy-points")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FantasyPointsController {

    private final FantasyPointsService fantasyPointsService;
    private final TeamRepository teamRepository;
    private final TeamGameweekPointsRepository gwPointsRepository;
    private final LeagueRepository leagueRepository;
    private final PlayerRepository playerRepository;
    private final PlayerStatisticRepository statisticRepository;
    private final MatchRepository matchRepository;
    private final TeamPlayerGameweekPointsRepository tpgwPointsRepository;
    private final GameweekStateService gameweekStateService;

  
    /**
     * Public endpoint – returns the active gameweek and lock status so the frontend
     * can disable team-editing UI when a gameweek is being scored.
     * GET /api/v1/fantasy-points/gameweek/status
     */
    @GetMapping("/gameweek/status")
    public ResponseEntity<Map<String, Object>> getGameweekStatus() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("activeGameweek", gameweekStateService.getActiveGameweek());
        status.put("teamsLocked", gameweekStateService.isTeamsLocked());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/leagues/{leagueId}/gameweek/{gameweek}/ranking")
    public ResponseEntity<List<GameweekRankingDTO>> getGameweekRanking(
            @PathVariable Long leagueId,
            @PathVariable Integer gameweek) {

        League league = leagueRepository.findById(leagueId)
            .orElseThrow(() -> new RuntimeException("League not found"));

        List<Team> teams = teamRepository.findByLeague(league);
        List<GameweekRankingDTO> ranking = new ArrayList<>();

        for (Team team : teams) {
            TeamGameweekPoints gwPoints = gwPointsRepository
                .findByTeamAndGameweek(team, gameweek)
                .orElse(null);

            GameweekRankingDTO dto = new GameweekRankingDTO();
            dto.setTeamId(team.getId());
            dto.setUserId(team.getUser().getId());
            dto.setUserDisplayName(team.getUser().getDisplayName());
            dto.setGameweekPoints(gwPoints != null ? gwPoints.getPoints() : 0);
            dto.setTotalPoints(team.getTotalPoints() != null ? team.getTotalPoints() : 0);

            ranking.add(dto);
        }

        ranking.sort((a, b) -> b.getGameweekPoints().compareTo(a.getGameweekPoints()));

        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setPosition(i + 1);
        }

        return ResponseEntity.ok(ranking);
    }


    @GetMapping("/teams/{teamId}/history")
    public ResponseEntity<TeamPointsHistoryDTO> getTeamPointsHistory(
            @PathVariable Integer teamId) {

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        List<TeamGameweekPoints> history = gwPointsRepository.findByTeamOrderByGameweekAsc(team);

        TeamPointsHistoryDTO dto = new TeamPointsHistoryDTO();
        dto.setTeamId(team.getId());
        dto.setTeamName(team.getUser().getDisplayName() + "'s Team");
        dto.setCurrentTotalPoints(team.getTotalPoints());
        dto.setHistory(history.stream().map(gwp -> {
            GameweekPointDTO gw = new GameweekPointDTO();
            gw.setGameweek(gwp.getGameweek());
            gw.setPoints(gwp.getPoints());
            gw.setCaptainBonus(gwp.getCaptainBonus());
            return gw;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(dto);
    }


    @GetMapping("/teams/{teamId}/gameweek/{gameweek}/breakdown")
    public ResponseEntity<TeamGameweekPointsDTO> getPointsBreakdown(
            @PathVariable Integer teamId,
            @PathVariable Integer gameweek) {

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamGameweekPoints gwPoints = gwPointsRepository
            .findByTeamAndGameweek(team, gameweek)
            .orElse(null);
        if (gwPoints == null) {
            return ResponseEntity.notFound().build();
        }

        TeamGameweekPointsDTO dto = new TeamGameweekPointsDTO();
        dto.setTeamId(team.getId());
        dto.setTeamName(team.getUser().getDisplayName() + "'s Team");
        dto.setGameweek(gameweek);
        dto.setTotalPoints(gwPoints.getPoints());
        dto.setGoalkeeperPoints(gwPoints.getGoalkeeperPoints());
        dto.setDefenderPoints(gwPoints.getDefenderPoints());
        dto.setMidfielderPoints(gwPoints.getMidfielderPoints());
        dto.setForwardPoints(gwPoints.getForwardPoints());
        dto.setCaptainBonus(gwPoints.getCaptainBonus());
        dto.setCaptainId(gwPoints.getCaptainId());
        dto.setTopScorerId(gwPoints.getTopScorerId());
        dto.setTopScorerPoints(gwPoints.getTopScorerPoints());
        dto.setCalculatedAt(gwPoints.getCalculatedAt());
        dto.setAppliedChip(gwPoints.getAppliedChip());

        if (gwPoints.getCaptainId() != null) {
            playerRepository.findById(gwPoints.getCaptainId())
                .ifPresent(p -> dto.setCaptainName(p.getFullName()));
        }
        if (gwPoints.getTopScorerId() != null) {
            playerRepository.findById(gwPoints.getTopScorerId())
                .ifPresent(p -> dto.setTopScorerName(p.getFullName()));
        }

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/gameweek/{gameweek}/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> recalculateGameweek(@PathVariable Integer gameweek) {
        try {
            fantasyPointsService.recalculateGameweekPoints(gameweek);
            return ResponseEntity.ok(Map.of("message", "Gameweek " + gameweek + " recalculated successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to recalculate: " + e.getMessage()));
        }
    }


    @GetMapping("/teams/{teamId}/gameweek/{gameweek}/players")
    public ResponseEntity<List<PlayerGameweekPointsDTO>> getPlayerGameweekPoints(
            @PathVariable Integer teamId,
            @PathVariable Integer gameweek) {

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        List<TeamPlayerGameweekPoints> snapshots =
            tpgwPointsRepository.findByTeamAndGameweek(team, gameweek);

        if (snapshots.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<String> playerIds = snapshots.stream()
            .filter(s -> s.getIsInLineup())
            .map(TeamPlayerGameweekPoints::getPlayerId)
            .collect(Collectors.toList());
        Map<String, String> avatarMap = playerRepository.findAllById(playerIds).stream()
            .collect(Collectors.toMap(Player::getId, p -> p.getAvatarUrl() != null ? p.getAvatarUrl() : "", (a, b) -> a));

        List<PlayerGameweekPointsDTO> results = snapshots.stream()
            .filter(s -> s.getIsInLineup()) 
            .map(s -> PlayerGameweekPointsDTO.builder()
                .playerId(s.getPlayerId())
                .playerName(s.getPlayerName())
                .position(s.getPosition())
                .gameweekPoints(s.getPoints()) 
                .matchId(s.getMatchId())
                .played(s.getMinutesPlayed() > 0)
                .isCaptain(s.getIsCaptain())
                .avatarUrl(avatarMap.getOrDefault(s.getPlayerId(), null))
                .build())
            .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @PostMapping("/players/update-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateAllPlayerPoints() {
        try {
            fantasyPointsService.updateAllPlayerPoints();
            return ResponseEntity.ok(Map.of(
                "message", "Update request triggered for all players"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update player points: " + e.getMessage()));
        }
    }
}
