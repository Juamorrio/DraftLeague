package com.DraftLeague.services;

import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.MatchRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MatchAnalyticsService {

    private final MatchRepository matchRepository;

    private static final double K_FACTOR = 32.0;
    private static final double INITIAL_ELO = 1500.0;
    private static final int POISSON_GRID_SIZE = 6;

    @Data
    public static class MatchStats {
        private double teamElo;
        private double opponentElo;
        private double expectedTeamGoals;
        private double expectedOpponentGoals;
        private double winProb;
        private double drawProb;
        private double lossProb;
        private double teamCleanSheetProb;    
        private double opponentCleanSheetProb;
        private double teamAttack;
        private double teamDefense;
        private double opponentAttack;
        private double opponentDefense;
    }

    public MatchStats calculateMatchStats(Integer teamId, Integer opponentId, Integer round) {
        List<Match> historicalMatches = matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.FINISHED && m.getRound() < round)
                .sorted(Comparator.comparing(Match::getRound))
                .collect(Collectors.toList());

        Map<Integer, Double> eloRatings = calculateEloHistory(historicalMatches);
        
        MatchStats stats = new MatchStats();
        stats.setTeamElo(eloRatings.getOrDefault(teamId, INITIAL_ELO));
        stats.setOpponentElo(eloRatings.getOrDefault(opponentId, INITIAL_ELO));

        calculatePoissonStats(stats, teamId, opponentId, historicalMatches);

        return stats;
    }

    private Map<Integer, Double> calculateEloHistory(List<Match> matches) {
        Map<Integer, Double> ratings = new HashMap<>();

        for (Match match : matches) {
            double homeElo = ratings.getOrDefault(match.getHomeTeamId(), INITIAL_ELO);
            double awayElo = ratings.getOrDefault(match.getAwayTeamId(), INITIAL_ELO);

            double expectedHome = 1.0 / (1.0 + Math.pow(10, (awayElo - homeElo) / 400.0));
            double actualHome = (match.getHomeGoals() > match.getAwayGoals()) ? 1.0 : 
                               (match.getHomeGoals().equals(match.getAwayGoals())) ? 0.5 : 0.0;

            ratings.put(match.getHomeTeamId(), homeElo + K_FACTOR * (actualHome - expectedHome));
            ratings.put(match.getAwayTeamId(), awayElo + K_FACTOR * ((1.0 - actualHome) - (1.0 - expectedHome)));
        }
        return ratings;
    }

    private void calculatePoissonStats(MatchStats stats, Integer teamId, Integer opponentId, List<Match> history) {
        if (history.isEmpty()) {
            stats.setWinProb(0.33); stats.setDrawProb(0.33); stats.setLossProb(0.34);
            return;
        }

        double avgHomeGoals = history.stream().mapToInt(Match::getHomeGoals).average().orElse(1.5);
        double avgAwayGoals = history.stream().mapToInt(Match::getAwayGoals).average().orElse(1.2);

        stats.setTeamAttack(calculateStrength(teamId, history, true, avgHomeGoals));
        stats.setTeamDefense(calculateStrength(teamId, history, false, avgAwayGoals));
        stats.setOpponentAttack(calculateStrength(opponentId, history, true, avgHomeGoals));
        stats.setOpponentDefense(calculateStrength(opponentId, history, false, avgAwayGoals));

     
        double teamLambda = stats.getTeamAttack() * stats.getOpponentDefense() * avgHomeGoals;
        double opponentLambda = stats.getOpponentAttack() * stats.getTeamDefense() * avgAwayGoals;

        stats.setExpectedTeamGoals(teamLambda);
        stats.setExpectedOpponentGoals(opponentLambda);

        stats.setTeamCleanSheetProb(Math.exp(-opponentLambda));
        stats.setOpponentCleanSheetProb(Math.exp(-teamLambda));

        double win = 0, draw = 0, loss = 0;
        for (int i = 0; i < POISSON_GRID_SIZE; i++) {
            for (int j = 0; j < POISSON_GRID_SIZE; j++) {
                double prob = poisson(i, teamLambda) * poisson(j, opponentLambda);
                if (i > j) win += prob;
                else if (i == j) draw += prob;
                else loss += prob;
            }
        }
        stats.setWinProb(win);
        stats.setDrawProb(draw);
        stats.setLossProb(loss);
    }

    private double calculateStrength(Integer teamId, List<Match> history, boolean isAttack, double globalAvg) {
        List<Match> teamMatches = history.stream()
                .filter(m -> m.getHomeTeamId().equals(teamId) || m.getAwayTeamId().equals(teamId))
                .collect(Collectors.toList());

        if (teamMatches.isEmpty()) return 1.0;

        double teamAvg;
        if (isAttack) {
            teamAvg = teamMatches.stream()
                    .mapToDouble(m -> m.getHomeTeamId().equals(teamId) ? m.getHomeGoals() : m.getAwayGoals())
                    .average().orElse(globalAvg);
        } else {
            teamAvg = teamMatches.stream()
                    .mapToDouble(m -> m.getHomeTeamId().equals(teamId) ? m.getAwayGoals() : m.getHomeGoals())
                    .average().orElse(globalAvg);
        }

        return teamAvg / globalAvg;
    }

    private double poisson(int k, double lambda) {
        return (Math.pow(lambda, k) * Math.exp(-lambda)) / factorial(k);
    }

    private long factorial(int n) {
        long res = 1;
        for (int i = 2; i <= n; i++) res *= i;
        return res;
    }
}
