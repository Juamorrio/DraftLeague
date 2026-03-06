package com.DraftLeague.services;

import com.DraftLeague.dto.PlayerPredictionDTO;
import com.DraftLeague.dto.TeamPredictionDTO;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import com.DraftLeague.repositories.PlayerTeamRepository;
import com.DraftLeague.repositories.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerPredictionService {

    private final PlayerStatisticRepository statisticRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final TeamRepository teamRepository;

    private static final int MIN_STATS_FOR_PREDICTION = 2;

    private final Map<String, PlayerPredictionDTO> predictionCache = new ConcurrentHashMap<>();

    /**
     * Returns the cached prediction for a player, computing it on first access.
     */
    public PlayerPredictionDTO predictForPlayer(String playerId) {
        return predictionCache.computeIfAbsent(playerId, this::buildPlayerPrediction);
    }

    /**
     * Pre-warms the prediction cache for every player that has at least
     * MIN_STATS_FOR_PREDICTION recorded match statistics.
     * Called automatically after gameweek points are recalculated so that
     * the AI-Insights page gets instant cache hits on the next request.
     */
    public void warmCacheForEligiblePlayers() {
        List<com.DraftLeague.models.Player.Player> allPlayers = playerRepository.findAll();
        int warmed = 0;
        for (com.DraftLeague.models.Player.Player player : allPlayers) {
            try {
                long statCount = statisticRepository
                        .findByPlayerIdOrderByMatchIdDesc(player.getId())
                        .stream()
                        .limit(MIN_STATS_FOR_PREDICTION)
                        .count();
                if (statCount >= MIN_STATS_FOR_PREDICTION) {
                    predictionCache.put(player.getId(), buildPlayerPrediction(player.getId()));
                    warmed++;
                }
            } catch (Exception e) {
                System.err.println("[PredictionCache] Could not pre-warm player "
                        + player.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("[PredictionCache] Pre-warmed " + warmed
                + " / " + allPlayers.size() + " player predictions after gameweek recalculation.");
    }

    @Transactional(readOnly = true)
    protected PlayerPredictionDTO buildPlayerPrediction(String playerId) {
        Player player = playerRepository.findById(playerId).orElse(null);

        // Load last 5 stats ordered by most recent match first
        List<PlayerStatistic> stats = statisticRepository
                .findByPlayerIdOrderByMatchIdDesc(playerId)
                .stream().limit(5).collect(Collectors.toList());

        // Resolve next match context using the player's real club
        Integer nextMatchId = null;
        Integer round = null;
        Boolean isHomeTeam = null;
        String opponent = null;

        if (player != null) {
            Optional<Match> nextMatch = matchRepository
                    .findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)
                    .stream()
                    .filter(m -> player.getClubId().equals(m.getHomeTeamId())
                              || player.getClubId().equals(m.getAwayTeamId()))
                    .findFirst();

            if (nextMatch.isPresent()) {
                Match m = nextMatch.get();
                nextMatchId = m.getId();
                round = m.getRound();
                isHomeTeam = player.getClubId().equals(m.getHomeTeamId());
                opponent = Boolean.TRUE.equals(isHomeTeam) ? m.getAwayClub() : m.getHomeClub();
            }
        }

        String playerName = player != null ? player.getFullName() : playerId;
        String position   = player != null ? player.getPosition().name() : "UNKNOWN";
        String playerType = stats.isEmpty() ? "UNKNOWN" : stats.get(0).getPlayerType().name();

        // Not enough history to produce a meaningful prediction
        if (stats.size() < MIN_STATS_FOR_PREDICTION) {
            return PlayerPredictionDTO.builder()
                    .playerId(playerId)
                    .playerName(playerName)
                    .position(position)
                    .playerType(playerType)
                    .predictedPoints(0.0)
                    .confidenceInterval(List.of(0, 0))
                    .featuresImportance(Collections.emptyMap())
                    .nextMatchId(nextMatchId)
                    .round(round)
                    .isHomeTeam(isHomeTeam)
                    .opponent(opponent)
                    .build();
        }

        // Exponential weights: index 0 (most recent) = 5, ..., index 4 = 1
        int[] weights = {5, 4, 3, 2, 1};
        int sumWeights = 0;
        double wPoints = 0, wRating = 0, wGoals = 0, wAssists = 0, wMinutes = 0;

        for (int i = 0; i < stats.size(); i++) {
            int w = weights[i];
            sumWeights += w;
            PlayerStatistic s = stats.get(i);

            // calculateFantasyPoints() recomputes from raw stats — always accurate
            wPoints  += s.calculateFantasyPoints() * w;
            wRating  += (s.getRating()       != null ? s.getRating()       : 0.0) * w;
            wGoals   += (s.getGoals()        != null ? s.getGoals()        : 0)   * w;
            wAssists += (s.getAssists()      != null ? s.getAssists()      : 0)   * w;
            wMinutes += (s.getMinutesPlayed()!= null ? s.getMinutesPlayed(): 0)   * w;
        }

        double predictedPoints = wPoints  / sumWeights;
        double avgRating        = wRating  / sumWeights;
        double avgGoals         = wGoals   / sumWeights;
        double avgAssists       = wAssists / sumWeights;
        double avgMinutes       = wMinutes / sumWeights;

        int confidenceLow  = Math.max(0, (int) Math.floor(predictedPoints * 0.7));
        int confidenceHigh = (int) Math.ceil(predictedPoints * 1.3);

        // Feature-importance map: values in [0, 1] so the frontend bar renders
        // importance * 100 as a percentage width.
        Map<String, Double> features = new LinkedHashMap<>();
        double ratingImp  = Math.min(avgRating  / 10.0, 1.0);
        double minutesImp = Math.min(avgMinutes / 90.0, 1.0);
        double goalsImp   = Math.min(avgGoals   /  3.0, 1.0);
        double assistsImp = Math.min(avgAssists /  3.0, 1.0);
        double formImp    = Math.min(predictedPoints / 15.0, 1.0);
        double matchesImp = stats.size() / 5.0;

        if (ratingImp  > 0) features.put("avgRatingLast5",  round2(ratingImp));
        if (minutesImp > 0) features.put("avgMinutesLast5", round2(minutesImp));
        if (goalsImp   > 0) features.put("avgGoalsLast5",   round2(goalsImp));
        if (assistsImp > 0) features.put("avgAssistsLast5", round2(assistsImp));
        if (formImp    > 0) features.put("recentFormPoints",round2(formImp));
        features.put("matchesPlayed", round2(matchesImp));

        return PlayerPredictionDTO.builder()
                .playerId(playerId)
                .playerName(playerName)
                .position(position)
                .playerType(playerType)
                .predictedPoints(round2(predictedPoints))
                .confidenceInterval(List.of(confidenceLow, confidenceHigh))
                .featuresImportance(features)
                .nextMatchId(nextMatchId)
                .round(round)
                .isHomeTeam(isHomeTeam)
                .opponent(opponent)
                .build();
    }

    /**
     * Builds a team prediction by aggregating predictions for all lined-up players.
     * The captain's points are doubled.
     */
    @Transactional(readOnly = true)
    public TeamPredictionDTO predictForTeam(Integer teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));

        List<PlayerTeam> lineup = playerTeamRepository.findByTeam(team).stream()
                .filter(pt -> Boolean.TRUE.equals(pt.getLined()))
                .collect(Collectors.toList());

        List<TeamPredictionDTO.PlayerPrediction> playerPredictions = new ArrayList<>();
        double total = 0;
        Integer nextMatchId = null;
        Integer round = null;
        Boolean isHomeTeam = null;
        String opponent = null;

        for (PlayerTeam pt : lineup) {
            PlayerPredictionDTO pred = predictForPlayer(pt.getPlayer().getId());
            double pts = pred.getPredictedPoints() != null ? pred.getPredictedPoints() : 0.0;
            boolean captain = Boolean.TRUE.equals(pt.getIsCaptain());
            double effectivePts = captain ? pts * 2 : pts;
            total += effectivePts;

            playerPredictions.add(TeamPredictionDTO.PlayerPrediction.builder()
                    .playerId(pred.getPlayerId())
                    .playerName(pred.getPlayerName())
                    .position(pred.getPosition())
                    .predictedPoints(round2(effectivePts))
                    .build());

            if (nextMatchId == null && pred.getNextMatchId() != null) {
                nextMatchId = pred.getNextMatchId();
                round       = pred.getRound();
                isHomeTeam  = pred.getIsHomeTeam();
                opponent    = pred.getOpponent();
            }
        }

        int low  = Math.max(0, (int) Math.floor(total * 0.7));
        int high = (int) Math.ceil(total * 1.3);

        String teamName = (team.getUser() != null && team.getUser().getUsername() != null)
                ? team.getUser().getUsername() + "'s Team"
                : "Team " + teamId;

        return TeamPredictionDTO.builder()
                .teamId(teamId)
                .teamName(teamName)
                .totalPredictedPoints(round2(total))
                .confidenceInterval(List.of(low, high))
                .players(playerPredictions)
                .nextMatchId(nextMatchId)
                .round(round)
                .isHomeTeam(isHomeTeam)
                .opponent(opponent)
                .build();
    }

    /**
     * Clears the prediction cache so the next request recomputes fresh predictions.
     * Called automatically after fantasy points are recalculated.
     */
    public void invalidateCache() {
        predictionCache.clear();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
