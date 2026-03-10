package com.DraftLeague.services;

import com.DraftLeague.models.Team.*;
import com.DraftLeague.models.Player.*;
import com.DraftLeague.models.Statistics.*;
import com.DraftLeague.models.Match.*;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import com.DraftLeague.repositories.TeamGameweekPointsRepository;
import com.DraftLeague.repositories.TeamPlayerGameweekPointsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FantasyPointsService {

    private static final Logger logger = LoggerFactory.getLogger(FantasyPointsService.class);

    private static final String CHIP_TRIPLE_CAP = "TRIPLE_CAP";
    private static final String CHIP_BENCH_BOOST = "BENCH_BOOST";

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

        String activeChip = team.getActiveChip();
        boolean isTripleCap = CHIP_TRIPLE_CAP.equals(activeChip);
        boolean isBenchBoost = CHIP_BENCH_BOOST.equals(activeChip);
        boolean isStatChip = activeChip != null && !isTripleCap && !isBenchBoost;

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

        List<TeamPlayerGameweekPoints> newSnapshots = new ArrayList<>();

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

            if (stat != null) {
                if (isStatChip) {
                    basePlayerPoints = stat.calculateFantasyPointsWithChip(activeChip);
                } else {
                    basePlayerPoints = stat.getTotalFantasyPoints() != null
                        ? stat.getTotalFantasyPoints() : 0;
                }
                finalPlayerPoints = basePlayerPoints;
                matchId = stat.getMatchId();
                minutesPlayed = stat.getMinutesPlayed() != null ? stat.getMinutesPlayed() : 0;

                if (pt.getIsCaptain() != null && pt.getIsCaptain()) {
                    finalPlayerPoints *= isTripleCap ? 3 : 2;
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
            newSnapshots.add(snapshot);

            boolean countForTotal = isBenchBoost || (pt.getLined() != null && pt.getLined());
            if (countForTotal) {
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

        tpgwPointsRepository.saveAll(newSnapshots);

        // Consume the active chip after calculation
        if (activeChip != null) {
            String used = team.getUsedChips();
            if (used == null || used.isBlank()) {
                team.setUsedChips(activeChip);
            } else {
                team.setUsedChips(used + "," + activeChip);
            }
            team.setActiveChip(null);
            teamRepository.save(team);
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
                logger.error("Error updating points for player {}: {}", playerId, e.getMessage(), e);
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
                logger.error("Error updating player points for match {}: {}", match.getId(), e.getMessage(), e);
            }
        }

        for (Team team : teams) {
            try {
                calculateTeamPointsForGameweek(team.getId(), gameweek);
                updateTeamTotalPoints(team.getId());
            } catch (Exception e) {
                logger.error("Error calculating points for team {}: {}", team.getId(), e.getMessage(), e);
            }
        }

        // Invalidate prediction cache so the next GET returns fresh predictions
        playerPredictionService.invalidateCache();
        // Pre-warm the cache for every player with enough data so that the
        // AI-Insights page responds instantly without on-demand computation.
        playerPredictionService.warmCacheForEligiblePlayers();
    }

    @Transactional
    public void updateAllPlayerPoints() {
        List<Player> players = playerRepository.findAll();
        for (Player player : players) {
            try {
                updatePlayerTotalPoints(player.getId());
            } catch (Exception e) {
                logger.error("Error updating player {}: {}", player.getId(), e.getMessage(), e);
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
