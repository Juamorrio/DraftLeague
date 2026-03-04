package com.DraftLeague.services;
import com.DraftLeague.models.Team.TeamGameweekPoints;
import com.DraftLeague.models.Team.TeamPlayerGameweekPoints;

import com.DraftLeague.models.Team.*;
import com.DraftLeague.models.Player.*;
import com.DraftLeague.models.Statistics.*;
import com.DraftLeague.models.Match.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import com.DraftLeague.repositories.TeamGameweekPointsRepository;
import com.DraftLeague.repositories.TeamPlayerGameweekPointsRepository;

@Service
@RequiredArgsConstructor
public class FantasyPointsService {

    private final TeamRepository teamRepository;
    private final TeamGameweekPointsRepository gwPointsRepository;
    private final PlayerStatisticRepository statisticRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final TeamPlayerGameweekPointsRepository tpgwPointsRepository;
    private final PlayerPredictionService playerPredictionService;

 
    @Transactional
    public TeamGameweekPoints calculateTeamPointsForGameweek(Integer teamId, Integer gameweek) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamGameweekPoints gwPoints = gwPointsRepository
            .findByTeamAndGameweek(team, gameweek)
            .orElse(new TeamGameweekPoints());

        gwPoints.setTeam(team);
        gwPoints.setGameweek(gameweek);

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

        List<TeamPlayerGameweekPoints> existingSnapshots =
            tpgwPointsRepository.findByTeamAndGameweek(team, gameweek);
        if (!existingSnapshots.isEmpty()) {
            tpgwPointsRepository.deleteAll(existingSnapshots);
        }

        for (PlayerTeam pt : team.getPlayerTeams()) {
            Player player = pt.getPlayer();

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

                if (pt.getIsCaptain() != null && pt.getIsCaptain()) {
                    finalPlayerPoints *= 2;
                    captainBonus = basePlayerPoints;
                    captainId = player.getId();
                }
            }

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

            if (pt.getLined() != null && pt.getLined()) {
                totalPoints += finalPlayerPoints;

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

                if (finalPlayerPoints > topScorerPoints) {
                    topScorerId = player.getId();
                    topScorerPoints = finalPlayerPoints;
                }
            }
        }

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

    @Transactional
    public void updatePlayerPointsForMatch(Integer matchId) {
        List<PlayerStatistic> stats = statisticRepository.findByMatchId(matchId);

        Set<String> playerIds = stats.stream()
            .map(PlayerStatistic::getPlayerId)
            .collect(Collectors.toSet());

        for (String playerId : playerIds) {
            try {
                updatePlayerTotalPoints(playerId);
            } catch (Exception e) {
                System.err.println("Error updating points for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public void recalculateGameweekPoints(Integer gameweek) {
        List<Team> teams = teamRepository.findAll();

        List<Match> matches = matchRepository.findByRound(gameweek);
        for (Match match : matches) {
            try {
                updatePlayerPointsForMatch(match.getId());
            } catch (Exception e) {
                System.err.println("Error updating player points for match " + match.getId() + ": " + e.getMessage());
            }
        }

        for (Team team : teams) {
            try {
                calculateTeamPointsForGameweek(team.getId(), gameweek);
                updateTeamTotalPoints(team.getId());
            } catch (Exception e) {
                System.err.println("Error calculating points for team " + team.getId() + ": " + e.getMessage());
            }
        }

        // Invalidate prediction cache so the next GET returns fresh predictions
        playerPredictionService.invalidateCache();
    }

    @Transactional
    public void updateAllPlayerPoints() {
        List<Player> players = playerRepository.findAll();
        for (Player player : players) {
            try {
                updatePlayerTotalPoints(player.getId());
            } catch (Exception e) {
                System.err.println("Error updating player " + player.getId() + ": " + e.getMessage());
            }
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerPointsUpdateForMatch(Integer matchId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found"));

        Integer gameweek = match.getRound();
        if (gameweek != null) {
            updatePlayerPointsForMatch(matchId);
            recalculateGameweekPoints(gameweek);
        }
    }
}
