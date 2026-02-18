package com.DraftLeague.services;

import com.DraftLeague.models.Team.*;
import com.DraftLeague.models.Player.*;
import com.DraftLeague.models.Statistics.*;
import com.DraftLeague.models.Match.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FantasyPointsService {

    private final TeamRepository teamRepository;
    private final TeamGameweekPointsRepository gwPointsRepository;
    private final PlayerStatisticRepository statisticRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final TeamPlayerGameweekPointsRepository tpgwPointsRepository;

    /**
     * Calcula y guarda los puntos de un equipo para una jornada específica
     */
    @Transactional
    public TeamGameweekPoints calculateTeamPointsForGameweek(Integer teamId, Integer gameweek) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        // Buscar o crear registro de puntos
        TeamGameweekPoints gwPoints = gwPointsRepository
            .findByTeamAndGameweek(team, gameweek)
            .orElse(new TeamGameweekPoints());

        gwPoints.setTeam(team);
        gwPoints.setGameweek(gameweek);

        // Obtener partidos de la jornada
        List<Match> matches = matchRepository.findByRound(gameweek);
        Set<Integer> matchIds = matches.stream()
            .map(Match::getId)
            .collect(Collectors.toSet());

        int totalPoints = 0;
        int gkPoints = 0, defPoints = 0, midPoints = 0, fwdPoints = 0;
        int captainBonus = 0;
        String captainId = null;
        String topScorerId = null;
        int topScorerPoints = 0;

        // ⭐ NUEVO: Limpiar snapshots antiguos de esta jornada (si recalculamos)
        List<TeamPlayerGameweekPoints> existingSnapshots =
            tpgwPointsRepository.findByTeamAndGameweek(team, gameweek);
        if (!existingSnapshots.isEmpty()) {
            tpgwPointsRepository.deleteAll(existingSnapshots);
        }

        // Iterar sobre jugadores del equipo
        for (PlayerTeam pt : team.getPlayerTeams()) {
            Player player = pt.getPlayer();

            // Buscar estadística del jugador en algún partido de la jornada
            PlayerStatistic stat = null;
            List<PlayerStatistic> playerStats = statisticRepository.findByPlayerId(player.getId());
            for (PlayerStatistic s : playerStats) {
                if (matchIds.contains(s.getMatchId())) {
                    stat = s;
                    break;
                }
            }

            int basePlayerPoints = 0;
            int finalPlayerPoints = 0;
            Integer matchId = null;
            int minutesPlayed = 0;
            boolean played = false;

            if (stat != null) {
                basePlayerPoints = stat.getTotalFantasyPoints() != null
                    ? stat.getTotalFantasyPoints() : 0;
                finalPlayerPoints = basePlayerPoints;
                matchId = stat.getMatchId();
                minutesPlayed = stat.getMinutesPlayed() != null ? stat.getMinutesPlayed() : 0;
                played = true;

                // Aplicar multiplicador de CAPITÁN (2x)
                if (pt.getIsCaptain() != null && pt.getIsCaptain()) {
                    finalPlayerPoints *= 2;
                    captainBonus = basePlayerPoints;
                    captainId = player.getId();
                }
            }

            // ⭐ NUEVO: Guardar snapshot inmutable (TODOS los jugadores del equipo, no solo titulares)
            TeamPlayerGameweekPoints snapshot = new TeamPlayerGameweekPoints();
            snapshot.setTeam(team);
            snapshot.setPlayerId(player.getId());
            snapshot.setGameweek(gameweek);
            snapshot.setPlayerName(player.getFullName());
            snapshot.setPosition(player.getPosition().toString());
            snapshot.setBasePoints(basePlayerPoints);
            snapshot.setPoints(finalPlayerPoints);
            snapshot.setMinutesPlayed(minutesPlayed);
            snapshot.setMatchId(matchId);
            snapshot.setIsInLineup(pt.getLined() != null && pt.getLined());
            snapshot.setIsCaptain(pt.getIsCaptain() != null && pt.getIsCaptain());
            snapshot.setIsBenched(pt.getLined() == null || !pt.getLined());

            tpgwPointsRepository.save(snapshot);

            // Solo sumar puntos de TITULARES al total del equipo
            if (pt.getLined() != null && pt.getLined()) {
                totalPoints += finalPlayerPoints;

                // Desglosar por posición
                Position position = player.getPosition();
                if (Position.POR.equals(position)) {
                    gkPoints += finalPlayerPoints;
                } else if (Position.DEF.equals(position)) {
                    defPoints += finalPlayerPoints;
                } else if (Position.MID.equals(position)) {
                    midPoints += finalPlayerPoints;
                } else if (Position.DEL.equals(position)) {
                    fwdPoints += finalPlayerPoints;
                }

                // Tracking del top scorer
                if (finalPlayerPoints > topScorerPoints) {
                    topScorerId = player.getId();
                    topScorerPoints = finalPlayerPoints;
                }
            }
        }

        // Guardar resultados agregados
        gwPoints.setPoints(totalPoints);
        gwPoints.setGoalkeeperPoints(gkPoints);
        gwPoints.setDefenderPoints(defPoints);
        gwPoints.setMidfielderPoints(midPoints);
        gwPoints.setForwardPoints(fwdPoints);
        gwPoints.setCaptainId(captainId);
        gwPoints.setCaptainBonus(captainBonus);
        gwPoints.setTopScorerId(topScorerId);
        gwPoints.setTopScorerPoints(topScorerPoints);
        gwPoints.setCalculatedAt(new Date());
        gwPoints.setBenchPoints(0);

        return gwPointsRepository.save(gwPoints);
    }

    /**
     * Actualiza Team.totalPoints sumando todos los gameweeks
     */
    @Transactional
    public void updateTeamTotalPoints(Integer teamId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        List<TeamGameweekPoints> history = gwPointsRepository.findByTeamOrderByGameweekAsc(team);

        int total = history.stream()
            .mapToInt(TeamGameweekPoints::getPoints)
            .sum();

        team.setTotalPoints(total);
        teamRepository.save(team);
    }

    /**
     * Actualiza Player.totalPoints sumando todas sus estadísticas
     */
    @Transactional
    public void updatePlayerTotalPoints(String playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new RuntimeException("Player not found"));

        List<PlayerStatistic> stats = statisticRepository.findByPlayerId(playerId);

        int total = stats.stream()
            .mapToInt(stat -> stat.getTotalFantasyPoints() != null ? stat.getTotalFantasyPoints() : 0)
            .sum();

        player.setTotalPoints(total);
        playerRepository.save(player);
    }

    /**
     * Actualiza los totalPoints de todos los jugadores únicos en un partido
     */
    @Transactional
    public void updatePlayerPointsForMatch(Integer matchId) {
        List<PlayerStatistic> stats = statisticRepository.findByMatchId(matchId);

        // Obtener IDs únicos de jugadores
        Set<String> playerIds = stats.stream()
            .map(PlayerStatistic::getPlayerId)
            .collect(Collectors.toSet());

        // Actualizar totalPoints de cada jugador
        for (String playerId : playerIds) {
            try {
                updatePlayerTotalPoints(playerId);
            } catch (Exception e) {
                System.err.println("Error updating points for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Recalcula puntos para TODOS los equipos en una jornada
     */
    @Transactional
    public void recalculateGameweekPoints(Integer gameweek) {
        List<Team> teams = teamRepository.findAll();

        // Primero actualizar puntos de jugadores
        List<Match> matches = matchRepository.findByRound(gameweek);
        for (Match match : matches) {
            try {
                updatePlayerPointsForMatch(match.getId());
            } catch (Exception e) {
                System.err.println("Error updating player points for match " + match.getId() + ": " + e.getMessage());
            }
        }

        // Luego actualizar puntos de equipos
        for (Team team : teams) {
            try {
                calculateTeamPointsForGameweek(team.getId(), gameweek);
                updateTeamTotalPoints(team.getId());
            } catch (Exception e) {
                // Log error pero continuar con otros equipos
                System.err.println("Error calculating points for team " + team.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Trigger para actualizar puntos cuando se guardan estadísticas
     */
    @Transactional
    public void triggerPointsUpdateForMatch(Integer matchId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found"));

        Integer gameweek = match.getRound();
        if (gameweek != null) {
            // Actualizar puntos de jugadores primero
            updatePlayerPointsForMatch(matchId);
            // Luego recalcular puntos de equipos
            recalculateGameweekPoints(gameweek);
        }
    }
}
