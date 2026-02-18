package com.DraftLeague.models.Statistics;

import com.DraftLeague.dto.JornadaMatchesDTO;
import com.DraftLeague.models.Statistics.dto.*;
import com.DraftLeague.models.Statistics.dto.PlayerMatchSummaryDTO;
import com.DraftLeague.models.Statistics.dto.PlayerStatisticsSummaryDTO;
import com.DraftLeague.services.FantasyPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerStatisticService {

    private final PlayerStatisticRepository playerStatisticRepository;
    private final PlayerStatisticFactory playerStatisticFactory;
    private final FantasyPointsService fantasyPointsService;

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

        // Guardar primero para obtener IDs
        statistics = playerStatisticRepository.saveAll(statistics);

        // Calcular fantasy points para todas las estadísticas guardadas
        for (PlayerStatistic stat : statistics) {
            int points = stat.calculateFantasyPoints();
            stat.setTotalFantasyPoints(points);
        }

        // Guardar de nuevo con los puntos calculados
        statistics = playerStatisticRepository.saveAll(statistics);

        // Trigger actualización de puntos de equipos
        if (!statistics.isEmpty()) {
            // Recoger TODOS los matchIds únicos del batch
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
        statistic.setMatchId(getInteger(data, "matchId"));
        statistic.setIsHomeTeam(getBoolean(data, "isHomeTeam"));
        statistic.setRole((String) data.get("role"));
        statistic.setFotmobRating(getDouble(data, "fotmobRating"));
        statistic.setMinutesPlayed(getInteger(data, "minutesPlayed"));
        statistic.setGoals(getInteger(data, "goals"));
        statistic.setAssists(getInteger(data, "assists"));
        statistic.setTotalShots(getInteger(data, "totalShots"));
        statistic.setAccuratePasses(getInteger(data, "accuratePasses"));
        statistic.setTotalPasses(getInteger(data, "totalPasses"));
        statistic.setChancesCreated(getInteger(data, "chancesCreated"));
        statistic.setExpectedAssists(getDouble(data, "expectedAssists"));
        statistic.setXgAndXa(getDouble(data, "xgAndXa"));
        statistic.setDefensiveActions(getInteger(data, "defensiveActions"));
        statistic.setTouches(getInteger(data, "touches"));
        statistic.setAccurateLongBalls(getInteger(data, "accurateLongBalls"));
        statistic.setTotalLongBalls(getInteger(data, "totalLongBalls"));
        statistic.setDispossessed(getInteger(data, "dispossessed"));
        statistic.setTackles(getInteger(data, "tackles"));
        statistic.setBlocks(getInteger(data, "blocks"));
        statistic.setClearances(getInteger(data, "clearances"));
        statistic.setInterceptions(getInteger(data, "interceptions"));
        statistic.setRecoveries(getInteger(data, "recoveries"));
        statistic.setDribbledPast(getInteger(data, "dribbledPast"));
        statistic.setDuelsWon(getInteger(data, "duelsWon"));
        statistic.setDuelsLost(getInteger(data, "duelsLost"));
        statistic.setGroundDuelsWon(getInteger(data, "groundDuelsWon"));
        statistic.setTotalGroundDuels(getInteger(data, "totalGroundDuels"));
        statistic.setAerialDuelsWon(getInteger(data, "aerialDuelsWon"));
        statistic.setTotalAerialDuels(getInteger(data, "totalAerialDuels"));
        statistic.setWasFouled(getInteger(data, "wasFouled"));
        statistic.setFoulsCommitted(getInteger(data, "foulsCommitted"));

        // Mapear campos específicos por tipo de jugador
        if (statistic instanceof GoalkeeperStatistic goalkeeper) {
            goalkeeper.setPassesIntoFinalThird(getInteger(data, "passesIntoFinalThird"));
            goalkeeper.setSaves(getInteger(data, "saves"));
            goalkeeper.setGoalsConceded(getInteger(data, "goalsConceded"));
            goalkeeper.setXgotFaced(getDouble(data, "xgotFaced"));
            goalkeeper.setGoalsPrevented(getDouble(data, "goalsPrevented"));
            goalkeeper.setSweeperActions(getInteger(data, "sweeperActions"));
            goalkeeper.setHighClaims(getInteger(data, "highClaims"));
            goalkeeper.setDivingSaves(getInteger(data, "divingSaves"));
            goalkeeper.setSavesInsideBox(getInteger(data, "savesInsideBox"));
            goalkeeper.setPunches(getInteger(data, "punches"));
            goalkeeper.setGoalKeeperThrows(getInteger(data, "goalKeeperThrows"));
        } else if (statistic instanceof DefenderStatistic defender) {
            defender.setExpectedGoals(getDouble(data, "expectedGoals"));
            defender.setExpectedGoalsOnTarget(getDouble(data, "expectedGoalsOnTarget"));
            defender.setXgNonPenalty(getDouble(data, "xgNonPenalty"));
            defender.setShotsOnTarget(getInteger(data, "shotsOnTarget"));
            defender.setTotalShotsOnTarget(getInteger(data, "totalShotsOnTarget"));
            defender.setTouchesOppBox(getInteger(data, "touchesOppBox"));
            defender.setPenaltiesWon(getInteger(data, "penaltiesWon"));
            defender.setHeadedClearances(getInteger(data, "headedClearances"));
            defender.setBlockedShots(getInteger(data, "blockedShots"));
        } else if (statistic instanceof MidfielderStatistic midfielder) {
            midfielder.setAccurateCrosses(getInteger(data, "accurateCrosses"));
            midfielder.setTotalCrosses(getInteger(data, "totalCrosses"));
            midfielder.setCorners(getInteger(data, "corners"));
            midfielder.setSuccessfulDribbles(getInteger(data, "successfulDribbles"));
            midfielder.setTotalDribbles(getInteger(data, "totalDribbles"));
            midfielder.setExpectedGoals(getDouble(data, "expectedGoals"));
            midfielder.setExpectedGoalsOnTarget(getDouble(data, "expectedGoalsOnTarget"));
            midfielder.setXgNonPenalty(getDouble(data, "xgNonPenalty"));
            midfielder.setShotsOnTarget(getInteger(data, "shotsOnTarget"));
            midfielder.setTotalShotsOnTarget(getInteger(data, "totalShotsOnTarget"));
            midfielder.setTouchesOppBox(getInteger(data, "touchesOppBox"));
            midfielder.setPenaltiesWon(getInteger(data, "penaltiesWon"));
            midfielder.setPassesIntoFinalThird(getInteger(data, "passesIntoFinalThird"));
            midfielder.setBigChancesMissed(getInteger(data, "bigChancesMissed"));
            midfielder.setHeadedClearances(getInteger(data, "headedClearances"));
        } else if (statistic instanceof ForwardStatistic forward) {
            forward.setSuccessfulDribbles(getInteger(data, "successfulDribbles"));
            forward.setTotalDribbles(getInteger(data, "totalDribbles"));
            forward.setExpectedGoals(getDouble(data, "expectedGoals"));
            forward.setExpectedGoalsOnTarget(getDouble(data, "expectedGoalsOnTarget"));
            forward.setXgNonPenalty(getDouble(data, "xgNonPenalty"));
            forward.setShotsOnTarget(getInteger(data, "shotsOnTarget"));
            forward.setTotalShotsOnTarget(getInteger(data, "totalShotsOnTarget"));
            forward.setTouchesOppBox(getInteger(data, "touchesOppBox"));
            forward.setPassesIntoFinalThird(getInteger(data, "passesIntoFinalThird"));
            forward.setPenaltiesWon(getInteger(data, "penaltiesWon"));
            forward.setBigChancesMissed(getInteger(data, "bigChancesMissed"));
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
        // Convertir Integer u otros tipos a String
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
            System.out.println("Data retrieved for playerId " + playerId + " and matchId " + matchId + ": " + data);
            
            if (data == null || data.isEmpty()) {
                return null;
            }

            // Determine player type first
            String playerTypeStr = (String) data.get("player_type");
            if (playerTypeStr == null) {
                return null;
            }
            
            PlayerStatistic.PlayerType playerType = PlayerStatistic.PlayerType.valueOf(playerTypeStr);
            
            // Create appropriate DTO based on player type
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
        
        // Goalkeeper-specific fields
        dto.setPassesIntoFinalThird(getIntegerValue(data, "gk_passes_into_final_third"));
        dto.setSaves(getIntegerValue(data, "gk_saves"));
        dto.setGoalsConceded(getIntegerValue(data, "gk_goals_conceded"));
        dto.setXgotFaced(getDoubleOrNull(data, "gk_xgot_faced"));
        dto.setGoalsPrevented(getDoubleOrNull(data, "gk_goals_prevented"));
        dto.setSweeperActions(getIntegerValue(data, "gk_sweeper_actions"));
        dto.setHighClaims(getIntegerValue(data, "gk_high_claims"));
        dto.setDivingSaves(getIntegerValue(data, "gk_diving_saves"));
        dto.setSavesInsideBox(getIntegerValue(data, "gk_saves_inside_box"));
        dto.setPunches(getIntegerValue(data, "gk_punches"));
        dto.setGoalKeeperThrows(getIntegerValue(data, "gk_throws"));
        
        return dto;
    }
    
    private DefenderStatisticDTO mapToDefenderDTO(Map<String, Object> data) {
        DefenderStatisticDTO dto = new DefenderStatisticDTO();
        mapBaseFields(dto, data);
        
        // Defender-specific fields
        dto.setExpectedGoals(getDoubleOrNull(data, "def_expected_goals"));
        dto.setExpectedGoalsOnTarget(getDoubleOrNull(data, "def_expected_goals_on_target"));
        dto.setXgNonPenalty(getDoubleOrNull(data, "def_xg_non_penalty"));
        dto.setShotsOnTarget(getIntegerValue(data, "def_shots_on_target"));
        dto.setTotalShotsOnTarget(getIntegerValue(data, "def_total_shots_on_target"));
        dto.setTouchesOppBox(getIntegerValue(data, "def_touches_opp_box"));
        dto.setPenaltiesWon(getIntegerValue(data, "def_penalties_won"));
        dto.setHeadedClearances(getIntegerValue(data, "def_headed_clearances"));
        dto.setBlockedShots(getIntegerValue(data, "def_blocked_shots"));
        
        return dto;
    }
    
    private MidfielderStatisticDTO mapToMidfielderDTO(Map<String, Object> data) {
        MidfielderStatisticDTO dto = new MidfielderStatisticDTO();
        mapBaseFields(dto, data);
        
        // Midfielder-specific fields
        dto.setAccurateCrosses(getIntegerValue(data, "accurate_crosses"));
        dto.setTotalCrosses(getIntegerValue(data, "total_crosses"));
        dto.setCorners(getIntegerValue(data, "corners"));
        dto.setSuccessfulDribbles(getIntegerValue(data, "mid_successful_dribbles"));
        dto.setTotalDribbles(getIntegerValue(data, "mid_total_dribbles"));
        dto.setExpectedGoals(getDoubleOrNull(data, "mid_expected_goals"));
        dto.setExpectedGoalsOnTarget(getDoubleOrNull(data, "mid_expected_goals_on_target"));
        dto.setXgNonPenalty(getDoubleOrNull(data, "mid_xg_non_penalty"));
        dto.setShotsOnTarget(getIntegerValue(data, "mid_shots_on_target"));
        dto.setTotalShotsOnTarget(getIntegerValue(data, "mid_total_shots_on_target"));
        dto.setTouchesOppBox(getIntegerValue(data, "mid_touches_opp_box"));
        dto.setPenaltiesWon(getIntegerValue(data, "mid_penalties_won"));
        dto.setPassesIntoFinalThird(getIntegerValue(data, "mid_passes_into_final_third"));
        dto.setBigChancesMissed(getIntegerValue(data, "mid_big_chances_missed"));
        dto.setHeadedClearances(getIntegerValue(data, "mid_headed_clearances"));
        
        return dto;
    }
    
    private ForwardStatisticDTO mapToForwardDTO(Map<String, Object> data) {
        ForwardStatisticDTO dto = new ForwardStatisticDTO();
        mapBaseFields(dto, data);
        
        // Forward-specific fields
        dto.setSuccessfulDribbles(getIntegerValue(data, "fwd_successful_dribbles"));
        dto.setTotalDribbles(getIntegerValue(data, "fwd_total_dribbles"));
        dto.setExpectedGoals(getDoubleOrNull(data, "fwd_expected_goals"));
        dto.setExpectedGoalsOnTarget(getDoubleOrNull(data, "fwd_expected_goals_on_target"));
        dto.setXgNonPenalty(getDoubleOrNull(data, "fwd_xg_non_penalty"));
        dto.setShotsOnTarget(getIntegerValue(data, "fwd_shots_on_target"));
        dto.setTotalShotsOnTarget(getIntegerValue(data, "fwd_total_shots_on_target"));
        dto.setTouchesOppBox(getIntegerValue(data, "fwd_touches_opp_box"));
        dto.setPassesIntoFinalThird(getIntegerValue(data, "fwd_passes_into_final_third"));
        dto.setPenaltiesWon(getIntegerValue(data, "fwd_penalties_won"));
        dto.setBigChancesMissed(getIntegerValue(data, "fwd_big_chances_missed"));
        
        return dto;
    }
    
    private void mapBaseFields(BasePlayerStatisticDTO dto, Map<String, Object> data) {
        // Basic info
        dto.setId(getIntegerValue(data, "id"));
        dto.setPlayerId(getIntegerValue(data, "player_id"));
        dto.setMatchId(getIntegerValue(data, "match_id"));
        dto.setIsHomeTeam(getBooleanValue(data, "is_home_team"));
        
        String playerTypeStr = (String) data.get("player_type");
        if (playerTypeStr != null) {
            dto.setPlayerType(PlayerStatistic.PlayerType.valueOf(playerTypeStr));
        }
        dto.setRole((String) data.get("role"));
        
        // Performance
        dto.setFotmobRating(getDoubleOrNull(data, "fotmob_rating"));
        dto.setMinutesPlayed(getIntegerValue(data, "minutes_played"));
        dto.setTotalFantasyPoints(getIntegerValue(data, "total_fantasy_points"));
        
        // Offensive stats
        dto.setGoals(getIntegerValue(data, "goals"));
        dto.setAssists(getIntegerValue(data, "assists"));
        dto.setTotalShots(getIntegerValue(data, "total_shots"));
        dto.setChancesCreated(getIntegerValue(data, "chances_created"));
        
        // Passing stats
        Integer accuratePasses = getIntegerValue(data, "accurate_passes");
        Integer totalPasses = getIntegerValue(data, "total_passes");
        dto.setAccuratePasses(accuratePasses);
        dto.setTotalPasses(totalPasses);
        if (totalPasses != null && totalPasses > 0 && accuratePasses != null) {
            double accuracy = Math.round((double) accuratePasses / totalPasses * 10000.0) / 100.0;
            dto.setPassAccuracy(accuracy);
        }
        dto.setAccurateLongBalls(getIntegerValue(data, "accurate_long_balls"));
        dto.setTotalLongBalls(getIntegerValue(data, "total_long_balls"));
        
        // Expected stats
        dto.setExpectedAssists(getDoubleOrNull(data, "expected_assists"));
        dto.setXgAndXa(getDoubleOrNull(data, "xg_and_xa"));
        
        // Physical stats
        dto.setTouches(getIntegerValue(data, "touches"));
        dto.setDefensiveActions(getIntegerValue(data, "defensive_actions"));
        dto.setDispossessed(getIntegerValue(data, "dispossessed"));
        
        // Defending stats
        dto.setTackles(getIntegerValue(data, "tackles"));
        dto.setBlocks(getIntegerValue(data, "blocks"));
        dto.setClearances(getIntegerValue(data, "clearances"));
        dto.setInterceptions(getIntegerValue(data, "interceptions"));
        dto.setRecoveries(getIntegerValue(data, "recoveries"));
        dto.setDribbledPast(getIntegerValue(data, "dribbled_past"));
        
        // Duels
        dto.setDuelsWon(getIntegerValue(data, "duels_won"));
        dto.setDuelsLost(getIntegerValue(data, "duels_lost"));
        dto.setGroundDuelsWon(getIntegerValue(data, "ground_duels_won"));
        dto.setTotalGroundDuels(getIntegerValue(data, "total_ground_duels"));
        dto.setAerialDuelsWon(getIntegerValue(data, "aerial_duels_won"));
        dto.setTotalAerialDuels(getIntegerValue(data, "total_aerial_duels"));
        
        // Fouls and cards
        dto.setWasFouled(getIntegerValue(data, "was_fouled"));
        dto.setFoulsCommitted(getIntegerValue(data, "fouls_committed"));
        dto.setYellowCards(getIntegerValue(data, "yellow_cards"));
        dto.setRedCards(getIntegerValue(data, "red_cards"));
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
            
            // Extract values from the map
            Number matchesPlayed = (Number) data.get("matches_played");
            String playerType = (String) data.get("player_type");
            
            if (matchesPlayed == null || matchesPlayed.intValue() == 0) {
                return null;
            }
            
            int matches = matchesPlayed.intValue();
            summary.setMatchesPlayed(matches);
            summary.setPlayerType(playerType);
            
            // Get totals
            int totalGoals = getIntValue(data, "total_goals");
            int totalAssists = getIntValue(data, "total_assists");
            int totalMinutesPlayed = getIntValue(data, "total_minutes_played");
            int totalFantasyPoints = getIntValue(data, "total_fantasy_points");
            int totalShots = getIntValue(data, "total_shots");
            int totalAccuratePasses = getIntValue(data, "total_accurate_passes");
            int totalPasses = getIntValue(data, "total_passes");
            int totalChancesCreated = getIntValue(data, "total_chances_created");
            int totalDefensiveActions = getIntValue(data, "total_defensive_actions");
            int totalTouches = getIntValue(data, "total_touches");
            int totalTackles = getIntValue(data, "total_tackles");
            int totalInterceptions = getIntValue(data, "total_interceptions");
            int totalDuelsWon = getIntValue(data, "total_duels_won");
            int totalDuelsLost = getIntValue(data, "total_duels_lost");
            int totalYellowCards = getIntValue(data, "total_yellow_cards");
            int totalRedCards = getIntValue(data, "total_red_cards");
            
            // Set totals
            summary.setTotalGoals(totalGoals);
            summary.setTotalAssists(totalAssists);
            summary.setTotalMinutesPlayed(totalMinutesPlayed);
            summary.setTotalFantasyPoints(totalFantasyPoints);
            summary.setTotalShots(totalShots);
            summary.setTotalAccuratePasses(totalAccuratePasses);
            summary.setTotalPasses(totalPasses);
            summary.setTotalChancesCreated(totalChancesCreated);
            summary.setTotalDefensiveActions(totalDefensiveActions);
            summary.setTotalTouches(totalTouches);
            summary.setTotalTackles(totalTackles);
            summary.setTotalInterceptions(totalInterceptions);
            summary.setTotalDuelsWon(totalDuelsWon);
            summary.setTotalDuelsLost(totalDuelsLost);
            summary.setTotalYellowCards(totalYellowCards);
            summary.setTotalRedCards(totalRedCards);
            
            // Calculate and set averages
            summary.setAvgRating(getDoubleValue(data, "avg_rating"));
            summary.setAvgMinutesPlayed(Math.round((double) totalMinutesPlayed / matches * 100.0) / 100.0);
            summary.setAvgGoals(Math.round((double) totalGoals / matches * 100.0) / 100.0);
            summary.setAvgAssists(Math.round((double) totalAssists / matches * 100.0) / 100.0);
            summary.setAvgShots(Math.round((double) totalShots / matches * 100.0) / 100.0);
            summary.setAvgPassAccuracy(totalPasses > 0 ? Math.round((double) totalAccuratePasses / totalPasses * 10000.0) / 100.0 : 0.0);
            summary.setAvgTouches(Math.round((double) totalTouches / matches * 100.0) / 100.0);
            summary.setAvgDefensiveActions(Math.round((double) totalDefensiveActions / matches * 100.0) / 100.0);
            summary.setAvgDuelsWon(Math.round((double) totalDuelsWon / matches * 100.0) / 100.0);
            summary.setAvgFantasyPoints(Math.round((double) totalFantasyPoints / matches * 100.0) / 100.0);
            
            double avgExpectedAssists = getDoubleValue(data, "avg_expected_assists");
            double avgXgAndXa = getDoubleValue(data, "avg_xg_and_xa");
            
            summary.setAvgExpectedAssists(avgExpectedAssists > 0 ? avgExpectedAssists : null);
            summary.setAvgXgAndXa(avgXgAndXa > 0 ? avgXgAndXa : null);
            summary.setAvgExpectedGoals(null); // Will be calculated from specific tables if needed
            
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

            // Pre-cargar entidades JPA completas para calcular el breakdown de puntos
            List<PlayerStatistic> playerStats = playerStatisticRepository.findByPlayerId(playerId);
            Map<Integer, PlayerStatistic> statsByMatchId = new java.util.HashMap<>();
            for (PlayerStatistic stat : playerStats) {
                statsByMatchId.put(stat.getMatchId(), stat);
            }

            List<PlayerMatchSummaryDTO> summaries = new ArrayList<>();

            for (Map<String, Object> data : results) {
                PlayerMatchSummaryDTO dto = new PlayerMatchSummaryDTO();

                // Match info
                dto.setMatchId(getIntegerValue(data, "matchId"));
                dto.setRound(getIntegerValue(data, "round"));
                dto.setHomeTeam((String) data.get("homeTeam"));
                dto.setAwayTeam((String) data.get("awayTeam"));
                dto.setOpponent((String) data.get("opponent"));
                dto.setIsHomeTeam(getBooleanValue(data, "isHomeTeam"));

                // Match result
                dto.setGoalsScored(getIntegerValue(data, "goalsScored"));
                dto.setGoalsConceded(getIntegerValue(data, "goalsConceded"));

                // Player performance
                dto.setMinutesPlayed(getIntegerValue(data, "minutesPlayed"));
                dto.setFotmobRating(getDoubleOrNull(data, "fotmobRating"));
                dto.setFantasyPoints(getIntegerValue(data, "fantasyPoints"));

                // Offensive stats
                dto.setGoals(getIntegerValue(data, "goals"));
                dto.setAssists(getIntegerValue(data, "assists"));
                dto.setTotalShots(getIntegerValue(data, "totalShots"));
                dto.setChancesCreated(getIntegerValue(data, "chancesCreated"));
                dto.setExpectedAssists(getDoubleOrNull(data, "expectedAssists"));
                dto.setExpectedGoals(getDoubleOrNull(data, "expectedGoals"));

                // Passing stats
                dto.setTotalPasses(getIntegerValue(data, "totalPasses"));
                dto.setAccuratePasses(getIntegerValue(data, "accuratePasses"));
                dto.setTotalCrosses(getIntegerValue(data, "totalCrosses"));

                // Calculate pass accuracy
                Integer totalPasses = getIntegerValue(data, "totalPasses");
                Integer accuratePasses = getIntegerValue(data, "accuratePasses");
                if (totalPasses != null && totalPasses > 0 && accuratePasses != null) {
                    double accuracy = Math.round((double) accuratePasses / totalPasses * 10000.0) / 100.0;
                    dto.setPassAccuracy(accuracy);
                }

                // Physical stats
                dto.setTouches(getIntegerValue(data, "touches"));
                dto.setDefensiveActions(getIntegerValue(data, "defensiveActions"));

                // Defensive stats
                dto.setTackles(getIntegerValue(data, "tackles"));
                dto.setInterceptions(getIntegerValue(data, "interceptions"));
                dto.setClearances(getIntegerValue(data, "clearances"));
                dto.setBlocks(getIntegerValue(data, "blocks"));
                dto.setRecoveries(getIntegerValue(data, "recoveries"));

                // Duels
                dto.setDuelsWon(getIntegerValue(data, "duelsWon"));
                dto.setDuelsLost(getIntegerValue(data, "duelsLost"));

                // Cards
                dto.setYellowCards(getIntegerValue(data, "yellowCards"));
                dto.setRedCards(getIntegerValue(data, "redCards"));

                // Goalkeeper specific
                dto.setSaves(getIntegerValue(data, "saves"));
                dto.setGoalsAllowed(getIntegerValue(data, "goalsAllowed"));

                // Points breakdown
                Integer matchId = dto.getMatchId();
                if (matchId != null && statsByMatchId.containsKey(matchId)) {
                    PlayerStatistic stat = statsByMatchId.get(matchId);
                    dto.setPointsBreakdown(stat.calculateFantasyPointsBreakdown());
                }

                summaries.add(dto);
            }

            // Agrupar por jornada, filtrando aquellos sin round definido
            Map<Integer, List<PlayerMatchSummaryDTO>> matchesByRound = summaries.stream()
                .filter(dto -> dto.getRound() != null)
                .collect(Collectors.groupingBy(PlayerMatchSummaryDTO::getRound));

            // Convertir a lista de JornadaMatchesDTO y ordenar por jornada descendente
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
