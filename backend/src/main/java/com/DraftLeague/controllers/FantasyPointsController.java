package com.DraftLeague.controllers;

import com.DraftLeague.dto.*;
import com.DraftLeague.services.FantasyPointsService;
import com.DraftLeague.models.Team.*;
import com.DraftLeague.models.League.*;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerRepository;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.models.Statistics.PlayerStatisticRepository;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * GET /api/v1/fantasy-points/leagues/{leagueId}/gameweek/{gameweek}/ranking
     * Ranking de una jornada específica
     */
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

        // Ordenar por puntos de jornada DESC
        ranking.sort((a, b) -> b.getGameweekPoints().compareTo(a.getGameweekPoints()));

        // Asignar posiciones
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setPosition(i + 1);
        }

        return ResponseEntity.ok(ranking);
    }

    /**
     * GET /api/v1/fantasy-points/teams/{teamId}/history
     * Histórico de puntos de un equipo
     */
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

    /**
     * GET /api/v1/fantasy-points/teams/{teamId}/gameweek/{gameweek}/breakdown
     * Desglose detallado de puntos por posición
     */
    @GetMapping("/teams/{teamId}/gameweek/{gameweek}/breakdown")
    public ResponseEntity<TeamGameweekPointsDTO> getPointsBreakdown(
            @PathVariable Integer teamId,
            @PathVariable Integer gameweek) {

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamGameweekPoints gwPoints = gwPointsRepository
            .findByTeamAndGameweek(team, gameweek)
            .orElseThrow(() -> new RuntimeException("No points found for this gameweek"));

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

        // Nombres de capitán y top scorer
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

    /**
     * POST /api/v1/fantasy-points/gameweek/{gameweek}/recalculate
     * Recalcular puntos (solo admin)
     */
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

    /**
     * GET /api/v1/fantasy-points/teams/{teamId}/gameweek/{gameweek}/players
     * Puntos individuales por jugador en una jornada específica (DATOS INMUTABLES)
     */
    @GetMapping("/teams/{teamId}/gameweek/{gameweek}/players")
    public ResponseEntity<List<PlayerGameweekPointsDTO>> getPlayerGameweekPoints(
            @PathVariable Integer teamId,
            @PathVariable Integer gameweek) {

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        // ⭐ CAMBIO: Leer desde snapshots guardados en lugar de calcular dinámicamente
        List<TeamPlayerGameweekPoints> snapshots =
            tpgwPointsRepository.findByTeamAndGameweek(team, gameweek);

        if (snapshots.isEmpty()) {
            // Si no hay snapshots, la jornada no se ha calculado aún
            return ResponseEntity.ok(new ArrayList<>());
        }

        // Pre-cargar avatares de los jugadores
        List<String> playerIds = snapshots.stream()
            .filter(s -> s.getIsInLineup())
            .map(TeamPlayerGameweekPoints::getPlayerId)
            .collect(Collectors.toList());
        Map<String, String> avatarMap = playerRepository.findAllById(playerIds).stream()
            .collect(Collectors.toMap(Player::getId, p -> p.getAvatarUrl() != null ? p.getAvatarUrl() : "", (a, b) -> a));

        List<PlayerGameweekPointsDTO> results = snapshots.stream()
            .filter(s -> s.getIsInLineup()) // Solo titulares
            .map(s -> PlayerGameweekPointsDTO.builder()
                .playerId(s.getPlayerId())
                .playerName(s.getPlayerName())
                .position(s.getPosition())
                .gameweekPoints(s.getPoints()) // Ya incluye multiplicador de capitán
                .matchId(s.getMatchId())
                .played(s.getMinutesPlayed() > 0)
                .isCaptain(s.getIsCaptain())
                .avatarUrl(avatarMap.getOrDefault(s.getPlayerId(), null))
                .build())
            .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/v1/fantasy-points/players/update-all
     * Actualizar totalPoints de TODOS los jugadores (solo admin)
     */
    @PostMapping("/players/update-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateAllPlayerPoints() {
        try {
            List<Player> players = playerRepository.findAll();
            int updated = 0;

            for (Player player : players) {
                try {
                    fantasyPointsService.updatePlayerTotalPoints(player.getId());
                    updated++;
                } catch (Exception e) {
                    System.err.println("Error updating player " + player.getId() + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "Updated total points for " + updated + " players",
                "total", String.valueOf(players.size()),
                "updated", String.valueOf(updated)
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update player points: " + e.getMessage()));
        }
    }
}
