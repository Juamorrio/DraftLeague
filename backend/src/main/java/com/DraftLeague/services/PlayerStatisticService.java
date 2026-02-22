package com.DraftLeague.services;
import com.DraftLeague.models.Statistics.GoalkeeperStatistic;
import com.DraftLeague.models.Statistics.DefenderStatistic;
import com.DraftLeague.models.Statistics.MidfielderStatistic;
import com.DraftLeague.models.Statistics.ForwardStatistic;
import com.DraftLeague.services.PlayerStatisticFactory;

import com.DraftLeague.dto.JornadaMatchesDTO;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.dto.*;
import com.DraftLeague.dto.PlayerMatchSummaryDTO;
import com.DraftLeague.dto.PlayerStatisticsSummaryDTO;
import com.DraftLeague.services.FantasyPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import com.DraftLeague.services.PlayerStatisticService;

@Service
@RequiredArgsConstructor
public class PlayerStatisticService {

    private final PlayerStatisticRepository playerStatisticRepository;
    private final PlayerStatisticFactory playerStatisticFactory;
    private final FantasyPointsService fantasyPointsService;
    private final MatchRepository matchRepository;

    @Transactional
    public PlayerStatistic saveStatistic(PlayerStatistic statistic) {
        return playerStatisticRepository.save(statistic);
    }

    @Transactional
    public List<PlayerStatistic> saveStatistics(List<PlayerStatistic> statistics) {
        return playerStatisticRepository.saveAll(statistics);
    }

    @Transactional
    public List<PlayerStatistic> saveBulkFromJson(List<Map<String, Object>> jsonData) {
        List<PlayerStatistic> statistics = new ArrayList<>();

        for (Map<String, Object> data : jsonData) {
            String playerType = (String) data.get("playerType");
            PlayerStatistic statistic = playerStatisticFactory.createStatistic(playerType);

            mapJsonToStatistic(data, statistic);
            statistics.add(statistic);
        }

        statistics = playerStatisticRepository.saveAll(statistics);

        for (PlayerStatistic stat : statistics) {
            int points = stat.calculateFantasyPoints();
            stat.setTotalFantasyPoints(points);
        }

        statistics = playerStatisticRepository.saveAll(statistics);

        if (!statistics.isEmpty()) {
            java.util.Set<Integer> matchIds = new java.util.HashSet<>();
            for (PlayerStatistic stat : statistics) {
                if (stat.getMatchId() != null) {
                    matchIds.add(stat.getMatchId());
                }
            }
            for (Integer matchId : matchIds) {
                try {
                    fantasyPointsService.triggerPointsUpdateForMatch(matchId);
                } catch (Exception e) {
                    System.err.println("Error triggering fantasy points update for match " + matchId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return statistics;
    }

    private void mapJsonToStatistic(Map<String, Object> data, PlayerStatistic statistic) {
        statistic.setPlayerId(getString(data, "playerId"));
        statistic.setIsHomeTeam(getBoolean(data, "isHomeTeam"));
        statistic.setRole((String) data.get("role"));
        statistic.setRating(getDouble(data, "rating"));
        statistic.setMinutesPlayed(getInteger(data, "minutesPlayed"));
        statistic.setGoals(getInteger(data, "goals"));
        statistic.setAssists(getInteger(data, "assists"));
        statistic.setTotalShots(getInteger(data, "totalShots"));
        statistic.setShotsOnTarget(getInteger(data, "shotsOnTarget"));
        statistic.setAccuratePasses(getInteger(data, "accuratePasses"));
        statistic.setTotalPasses(getInteger(data, "totalPasses"));
        statistic.setChancesCreated(getInteger(data, "chancesCreated"));
        statistic.setSuccessfulDribbles(getInteger(data, "successfulDribbles"));
        statistic.setTotalDribbles(getInteger(data, "totalDribbles"));
        statistic.setDribbledPast(getInteger(data, "dribbledPast"));
        statistic.setOffsides(getInteger(data, "offsides"));
        statistic.setAccurateCrosses(getInteger(data, "accurateCrosses"));
        statistic.setTotalCrosses(getInteger(data, "totalCrosses"));
        statistic.setTackles(getInteger(data, "tackles"));
        statistic.setBlocks(getInteger(data, "blocks"));
        statistic.setInterceptions(getInteger(data, "interceptions"));
        statistic.setDuelsWon(getInteger(data, "duelsWon"));
        statistic.setDuelsLost(getInteger(data, "duelsLost"));
        statistic.setWasFouled(getInteger(data, "wasFouled"));
        statistic.setFoulsCommitted(getInteger(data, "foulsCommitted"));
        statistic.setYellowCards(getInteger(data, "yellowCards"));
        statistic.setRedCards(getInteger(data, "redCards"));
        statistic.setPenaltiesWon(getInteger(data, "penaltiesWon"));
        statistic.setPenaltyScored(getInteger(data, "penaltyScored"));
        statistic.setPenaltyMissed(getInteger(data, "penaltyMissed"));
        statistic.setIsSubstitute(getBoolean(data, "isSubstitute"));
        statistic.setIsCaptain(getBoolean(data, "isCaptain"));
        statistic.setShirtNumber(getInteger(data, "shirtNumber"));
        statistic.setPenaltyCommitted(getInteger(data, "penaltyCommitted"));

        // Resolver fixtureId de API-Football a match_id interno de la BD
        Integer fixtureId = getInteger(data, "fixtureId");
        if (fixtureId != null) {
            Match match = matchRepository.findByApiFootballFixtureId(fixtureId).orElse(null);
            if (match != null) {
                statistic.setMatchId(match.getId());
            }
        } else {
            statistic.setMatchId(getInteger(data, "matchId"));
        }

        // Mapear campos especificos por tipo de jugador
        if (statistic instanceof GoalkeeperStatistic goalkeeper) {
            goalkeeper.setSaves(getInteger(data, "saves"));
            goalkeeper.setGoalsConceded(getInteger(data, "goalsConceded"));
            statistic.setSaves(getInteger(data, "saves"));
            statistic.setGoalsConceded(getInteger(data, "goalsConceded"));
            statistic.setPenaltiesSaved(getInteger(data, "penaltiesSaved"));
            statistic.setCleanSheet(getBoolean(data, "cleanSheet"));
        } else if (statistic instanceof DefenderStatistic defender) {
            defender.setPenaltiesWon(getInteger(data, "penaltiesWon"));
            defender.setSuccessfulDribbles(getInteger(data, "successfulDribbles"));
            defender.setTotalDribbles(getInteger(data, "totalDribbles"));
        } else if (statistic instanceof MidfielderStatistic midfielder) {
            midfielder.setSuccessfulDribbles(getInteger(data, "successfulDribbles"));
            midfielder.setTotalDribbles(getInteger(data, "totalDribbles"));
            midfielder.setPenaltiesWon(getInteger(data, "penaltiesWon"));
        } else if (statistic instanceof ForwardStatistic forward) {
            forward.setSuccessfulDribbles(getInteger(data, "successfulDribbles"));
            forward.setTotalDribbles(getInteger(data, "totalDribbles"));
            forward.setPenaltiesWon(getInteger(data, "penaltiesWon"));
            forward.setOffsides(getInteger(data, "offsides"));
        }
    }

    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private Double getDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return null;
    }

    private Boolean getBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        return value.toString();
    }

    public List<PlayerStatistic> getPlayerStatistics(String playerId) {
        return playerStatisticRepository.findByPlayerId(playerId);
    }

    public List<PlayerStatistic> getMatchStatistics(Integer matchId) {
        return playerStatisticRepository.findByMatchId(matchId);
    }

    public Object getPlayerMatchStatistic(String playerId, Integer matchId) {
        try {
            Map<String, Object> data = playerStatisticRepository.findPlayerMatchStatisticData(playerId, matchId);

            if (data == null || data.isEmpty()) {
                return null;
            }

            String playerTypeStr = (String) data.get("player_type");
            if (playerTypeStr == null) {
                return null;
            }

            PlayerStatistic.PlayerType playerType = PlayerStatistic.PlayerType.valueOf(playerTypeStr);

            return switch (playerType) {
                case GOALKEEPER -> mapToGoalkeeperDTO(data);
                case DEFENDER -> mapToDefenderDTO(data);
                case MIDFIELDER -> mapToMidfielderDTO(data);
                case FORWARD -> mapToForwardDTO(data);
            };
        } catch (Exception e) {
            System.err.println("Error in getPlayerMatchStatistic: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error getting player match statistic", e);
        }
    }

    private GoalkeeperStatisticDTO mapToGoalkeeperDTO(Map<String, Object> data) {
        GoalkeeperStatisticDTO dto = new GoalkeeperStatisticDTO();
        mapBaseFields(dto, data);
        dto.setSaves(getIntegerValue(data, "gk_saves"));
        dto.setGoalsConceded(getIntegerValue(data, "gk_goals_conceded"));
        return dto;
    }

    private DefenderStatisticDTO mapToDefenderDTO(Map<String, Object> data) {
        DefenderStatisticDTO dto = new DefenderStatisticDTO();
        mapBaseFields(dto, data);
        dto.setPenaltiesWon(getIntegerValue(data, "def_penalties_won"));
        dto.setSuccessfulDribbles(getIntegerValue(data, "def_successful_dribbles"));
        dto.setTotalDribbles(getIntegerValue(data, "def_total_dribbles"));
        return dto;
    }

    private MidfielderStatisticDTO mapToMidfielderDTO(Map<String, Object> data) {
        MidfielderStatisticDTO dto = new MidfielderStatisticDTO();
        mapBaseFields(dto, data);
        dto.setSuccessfulDribbles(getIntegerValue(data, "mid_successful_dribbles"));
        dto.setTotalDribbles(getIntegerValue(data, "mid_total_dribbles"));
        dto.setPenaltiesWon(getIntegerValue(data, "mid_penalties_won"));
        return dto;
    }

    private ForwardStatisticDTO mapToForwardDTO(Map<String, Object> data) {
        ForwardStatisticDTO dto = new ForwardStatisticDTO();
        mapBaseFields(dto, data);
        dto.setSuccessfulDribbles(getIntegerValue(data, "fwd_successful_dribbles"));
        dto.setTotalDribbles(getIntegerValue(data, "fwd_total_dribbles"));
        dto.setPenaltiesWon(getIntegerValue(data, "fwd_penalties_won"));
        dto.setOffsides(getIntegerValue(data, "fwd_offsides"));
        return dto;
    }

    private void mapBaseFields(BasePlayerStatisticDTO dto, Map<String, Object> data) {
        dto.setId(getIntegerValue(data, "id"));
        dto.setPlayerId(getIntegerValue(data, "player_id"));
        dto.setMatchId(getIntegerValue(data, "match_id"));
        dto.setIsHomeTeam(getBooleanValue(data, "is_home_team"));

        String playerTypeStr = (String) data.get("player_type");
        if (playerTypeStr != null) {
            dto.setPlayerType(PlayerStatistic.PlayerType.valueOf(playerTypeStr));
        }
        dto.setRole((String) data.get("role"));

        dto.setRating(getDoubleOrNull(data, "rating"));
        dto.setMinutesPlayed(getIntegerValue(data, "minutes_played"));
        dto.setTotalFantasyPoints(getIntegerValue(data, "total_fantasy_points"));

        dto.setGoals(getIntegerValue(data, "goals"));
        dto.setAssists(getIntegerValue(data, "assists"));
        dto.setTotalShots(getIntegerValue(data, "total_shots"));
        dto.setShotsOnTarget(getIntegerValue(data, "shots_on_target"));
        dto.setChancesCreated(getIntegerValue(data, "chances_created"));
        dto.setSuccessfulDribbles(getIntegerValue(data, "successful_dribbles"));
        dto.setTotalDribbles(getIntegerValue(data, "total_dribbles"));
        dto.setDribbledPast(getIntegerValue(data, "dribbled_past"));
        dto.setOffsides(getIntegerValue(data, "offsides"));

        Integer accuratePasses = getIntegerValue(data, "accurate_passes");
        Integer totalPasses = getIntegerValue(data, "total_passes");
        dto.setAccuratePasses(accuratePasses);
        dto.setTotalPasses(totalPasses);
        if (totalPasses != null && totalPasses > 0 && accuratePasses != null) {
            double accuracy = Math.round((double) accuratePasses / totalPasses * 10000.0) / 100.0;
            dto.setPassAccuracy(accuracy);
        }
        dto.setAccurateCrosses(getIntegerValue(data, "accurate_crosses"));
        dto.setTotalCrosses(getIntegerValue(data, "total_crosses"));

        dto.setTackles(getIntegerValue(data, "tackles"));
        dto.setBlocks(getIntegerValue(data, "blocks"));
        dto.setInterceptions(getIntegerValue(data, "interceptions"));

        dto.setDuelsWon(getIntegerValue(data, "duels_won"));
        dto.setDuelsLost(getIntegerValue(data, "duels_lost"));

        dto.setWasFouled(getIntegerValue(data, "was_fouled"));
        dto.setFoulsCommitted(getIntegerValue(data, "fouls_committed"));
        dto.setYellowCards(getIntegerValue(data, "yellow_cards"));
        dto.setRedCards(getIntegerValue(data, "red_cards"));

        dto.setPenaltiesWon(getIntegerValue(data, "penalties_won"));
        dto.setPenaltyScored(getIntegerValue(data, "penalty_scored"));
        dto.setPenaltyMissed(getIntegerValue(data, "penalty_missed"));
        dto.setIsSubstitute(getBooleanValue(data, "is_substitute"));
        dto.setIsCaptain(getBooleanValue(data, "is_captain"));
        dto.setShirtNumber(getIntegerValue(data, "shirt_number"));
        dto.setPenaltyCommitted(getIntegerValue(data, "penalty_committed"));
    }

    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private Double getDoubleOrNull(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return null;
    }

    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() == 1;
        return Boolean.parseBoolean(value.toString());
    }

    public PlayerStatisticsSummaryDTO getPlayerStatisticsSummary(String playerId) {
        try {
            Map<String, Object> data = playerStatisticRepository.getPlayerStatisticsSummaryData(playerId);

            if (data == null || data.isEmpty()) {
                return null;
            }

            PlayerStatisticsSummaryDTO summary = new PlayerStatisticsSummaryDTO();
            summary.setPlayerId(playerId);

            Number matchesPlayed = (Number) data.get("matches_played");
            String playerType = (String) data.get("player_type");

            if (matchesPlayed == null || matchesPlayed.intValue() == 0) {
                return null;
            }

            int matches = matchesPlayed.intValue();
            summary.setMatchesPlayed(matches);
            summary.setPlayerType(playerType);

            int totalGoals = getIntValue(data, "total_goals");
            int totalAssists = getIntValue(data, "total_assists");
            int totalMinutesPlayed = getIntValue(data, "total_minutes_played");
            int totalFantasyPoints = getIntValue(data, "total_fantasy_points");
            int totalShots = getIntValue(data, "total_shots");
            int totalAccuratePasses = getIntValue(data, "total_accurate_passes");
            int totalPassesVal = getIntValue(data, "total_passes");
            int totalChancesCreated = getIntValue(data, "total_chances_created");
            int totalTackles = getIntValue(data, "total_tackles");
            int totalInterceptions = getIntValue(data, "total_interceptions");
            int totalDuelsWon = getIntValue(data, "total_duels_won");
            int totalDuelsLost = getIntValue(data, "total_duels_lost");
            int totalYellowCards = getIntValue(data, "total_yellow_cards");
            int totalRedCards = getIntValue(data, "total_red_cards");

            summary.setTotalGoals(totalGoals);
            summary.setTotalAssists(totalAssists);
            summary.setTotalMinutesPlayed(totalMinutesPlayed);
            summary.setTotalFantasyPoints(totalFantasyPoints);
            summary.setTotalShots(totalShots);
            summary.setTotalAccuratePasses(totalAccuratePasses);
            summary.setTotalPasses(totalPassesVal);
            summary.setTotalChancesCreated(totalChancesCreated);
            summary.setTotalTackles(totalTackles);
            summary.setTotalInterceptions(totalInterceptions);
            summary.setTotalDuelsWon(totalDuelsWon);
            summary.setTotalDuelsLost(totalDuelsLost);
            summary.setTotalYellowCards(totalYellowCards);
            summary.setTotalRedCards(totalRedCards);
            summary.setTotalPenaltyCommitted(getIntValue(data, "total_penalty_committed"));
            summary.setTimesCaptain(getIntValue(data, "times_captain"));

            summary.setAvgRating(getDoubleValue(data, "avg_rating"));
            summary.setAvgMinutesPlayed(Math.round((double) totalMinutesPlayed / matches * 100.0) / 100.0);
            summary.setAvgGoals(Math.round((double) totalGoals / matches * 100.0) / 100.0);
            summary.setAvgAssists(Math.round((double) totalAssists / matches * 100.0) / 100.0);
            summary.setAvgShots(Math.round((double) totalShots / matches * 100.0) / 100.0);
            summary.setAvgPassAccuracy(totalPassesVal > 0 ? Math.round((double) totalAccuratePasses / totalPassesVal * 10000.0) / 100.0 : 0.0);
            summary.setAvgDuelsWon(Math.round((double) totalDuelsWon / matches * 100.0) / 100.0);
            summary.setAvgFantasyPoints(Math.round((double) totalFantasyPoints / matches * 100.0) / 100.0);

            return summary;
        } catch (Exception e) {
            System.err.println("Error in getPlayerStatisticsSummary: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error calculating player statistics summary", e);
        }
    }

    private int getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return Math.round(((Number) value).doubleValue() * 100.0) / 100.0;
        }
        return 0.0;
    }

    public List<JornadaMatchesDTO> getPlayerMatchesSummary(String playerId) {
        try {
            List<Map<String, Object>> results = playerStatisticRepository.getPlayerMatchesSummary(playerId);

            List<PlayerStatistic> playerStats = playerStatisticRepository.findByPlayerId(playerId);
            Map<Integer, PlayerStatistic> statsByMatchId = new java.util.HashMap<>();
            for (PlayerStatistic stat : playerStats) {
                statsByMatchId.put(stat.getMatchId(), stat);
            }

            List<PlayerMatchSummaryDTO> summaries = new ArrayList<>();

            for (Map<String, Object> data : results) {
                PlayerMatchSummaryDTO dto = new PlayerMatchSummaryDTO();

                dto.setMatchId(getIntegerValue(data, "matchId"));
                dto.setRound(getIntegerValue(data, "round"));
                dto.setHomeTeam((String) data.get("homeTeam"));
                dto.setAwayTeam((String) data.get("awayTeam"));
                dto.setOpponent((String) data.get("opponent"));
                dto.setIsHomeTeam(getBooleanValue(data, "isHomeTeam"));

                dto.setGoalsScored(getIntegerValue(data, "goalsScored"));
                dto.setGoalsConceded(getIntegerValue(data, "goalsConceded"));

                dto.setMinutesPlayed(getIntegerValue(data, "minutesPlayed"));
                dto.setRating(getDoubleOrNull(data, "rating"));
                dto.setFantasyPoints(getIntegerValue(data, "fantasyPoints"));

                dto.setGoals(getIntegerValue(data, "goals"));
                dto.setAssists(getIntegerValue(data, "assists"));
                dto.setTotalShots(getIntegerValue(data, "totalShots"));
                dto.setShotsOnTarget(getIntegerValue(data, "shotsOnTarget"));
                dto.setChancesCreated(getIntegerValue(data, "chancesCreated"));

                Integer totalPassesDTO = getIntegerValue(data, "totalPasses");
                Integer accuratePassesDTO = getIntegerValue(data, "accuratePasses");
                dto.setTotalPasses(totalPassesDTO);
                dto.setAccuratePasses(accuratePassesDTO);
                if (totalPassesDTO != null && totalPassesDTO > 0 && accuratePassesDTO != null) {
                    double accuracy = Math.round((double) accuratePassesDTO / totalPassesDTO * 10000.0) / 100.0;
                    dto.setPassAccuracy(accuracy);
                }

                dto.setTackles(getIntegerValue(data, "tackles"));
                dto.setInterceptions(getIntegerValue(data, "interceptions"));
                dto.setBlocks(getIntegerValue(data, "blocks"));

                dto.setDuelsWon(getIntegerValue(data, "duelsWon"));
                dto.setDuelsLost(getIntegerValue(data, "duelsLost"));

                dto.setYellowCards(getIntegerValue(data, "yellowCards"));
                dto.setRedCards(getIntegerValue(data, "redCards"));

                dto.setSaves(getIntegerValue(data, "saves"));
                dto.setGoalsAllowed(getIntegerValue(data, "goalsAllowed"));

                dto.setIsCaptain(getBooleanValue(data, "isCaptain"));
                dto.setShirtNumber(getIntegerValue(data, "shirtNumber"));
                dto.setPenaltyCommitted(getIntegerValue(data, "penaltyCommitted"));

                Integer matchId = dto.getMatchId();
                if (matchId != null && statsByMatchId.containsKey(matchId)) {
                    PlayerStatistic stat = statsByMatchId.get(matchId);
                    dto.setPointsBreakdown(stat.calculateFantasyPointsBreakdown());
                }

                summaries.add(dto);
            }

            Map<Integer, List<PlayerMatchSummaryDTO>> matchesByRound = summaries.stream()
                .filter(d -> d.getRound() != null)
                .collect(Collectors.groupingBy(PlayerMatchSummaryDTO::getRound));

            return matchesByRound.entrySet().stream()
                .map(entry -> new JornadaMatchesDTO(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.getJornada().compareTo(a.getJornada()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in getPlayerMatchesSummary: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error getting player matches summary", e);
        }
    }
}
