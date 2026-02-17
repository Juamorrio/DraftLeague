package com.DraftLeague.models.Statistics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PlayerStatisticRepository extends JpaRepository<PlayerStatistic, Integer> {

    List<PlayerStatistic> findByPlayerId(String playerId);

    List<PlayerStatistic> findByMatchId(Integer matchId);

    List<PlayerStatistic> findByPlayerType(PlayerStatistic.PlayerType playerType);

    @Query("SELECT ps FROM PlayerStatistic ps WHERE ps.playerId = :playerId AND ps.matchId IN :matchIds")
    Optional<PlayerStatistic> findByPlayerIdAndMatchIdIn(@Param("playerId") String playerId, @Param("matchIds") Set<Integer> matchIds);

    @Query(value = """
        SELECT 
            ps.*,
            gs.passes_into_final_third as gk_passes_into_final_third,
            gs.saves as gk_saves,
            gs.goals_conceded as gk_goals_conceded,
            gs.xgot_faced as gk_xgot_faced,
            gs.goals_prevented as gk_goals_prevented,
            gs.sweeper_actions as gk_sweeper_actions,
            gs.high_claims as gk_high_claims,
            gs.diving_saves as gk_diving_saves,
            gs.saves_inside_box as gk_saves_inside_box,
            gs.punches as gk_punches,
            gs.throws as gk_throws,
            ds.expected_goals as def_expected_goals,
            ds.expected_goals_on_target as def_expected_goals_on_target,
            ds.xg_non_penalty as def_xg_non_penalty,
            ds.shots_on_target as def_shots_on_target,
            ds.total_shots_on_target as def_total_shots_on_target,
            ds.touches_opp_box as def_touches_opp_box,
            ds.penalties_won as def_penalties_won,
            ds.headed_clearances as def_headed_clearances,
            ds.blocked_shots as def_blocked_shots,
            ms.accurate_crosses,
            ms.total_crosses,
            ms.corners,
            ms.successful_dribbles as mid_successful_dribbles,
            ms.total_dribbles as mid_total_dribbles,
            ms.expected_goals as mid_expected_goals,
            ms.expected_goals_on_target as mid_expected_goals_on_target,
            ms.xg_non_penalty as mid_xg_non_penalty,
            ms.shots_on_target as mid_shots_on_target,
            ms.total_shots_on_target as mid_total_shots_on_target,
            ms.touches_opp_box as mid_touches_opp_box,
            ms.penalties_won as mid_penalties_won,
            ms.passes_into_final_third as mid_passes_into_final_third,
            ms.big_chances_missed as mid_big_chances_missed,
            ms.headed_clearances as mid_headed_clearances,
            fs.successful_dribbles as fwd_successful_dribbles,
            fs.total_dribbles as fwd_total_dribbles,
            fs.expected_goals as fwd_expected_goals,
            fs.expected_goals_on_target as fwd_expected_goals_on_target,
            fs.xg_non_penalty as fwd_xg_non_penalty,
            fs.shots_on_target as fwd_shots_on_target,
            fs.total_shots_on_target as fwd_total_shots_on_target,
            fs.touches_opp_box as fwd_touches_opp_box,
            fs.passes_into_final_third as fwd_passes_into_final_third,
            fs.penalties_won as fwd_penalties_won,
            fs.big_chances_missed as fwd_big_chances_missed
        FROM player_statistic ps
        LEFT JOIN goalkeeper_statistic gs ON ps.id = gs.id
        LEFT JOIN defender_statistic ds ON ps.id = ds.id
        LEFT JOIN midfielder_statistic ms ON ps.id = ms.id
        LEFT JOIN forward_statistic fs ON ps.id = fs.id
        WHERE ps.player_id = :playerId AND ps.match_id = :matchId
        LIMIT 1
        """, nativeQuery = true)
    Map<String, Object> findPlayerMatchStatisticData(@Param("playerId") String playerId, @Param("matchId") Integer matchId);

    @Query("SELECT ps FROM PlayerStatistic ps WHERE ps.playerId = :playerId ORDER BY ps.matchId DESC")
    List<PlayerStatistic> findByPlayerIdOrderByMatchIdDesc(@Param("playerId") String playerId);

    @Query(value = """
        SELECT
            ps.match_id as matchId,
            m.round as round,
            m.home_club as homeTeam,
            m.away_club as awayTeam,
            CASE
                WHEN ps.is_home_team = 1 THEN m.away_club
                ELSE m.home_club
            END as opponent,
            ps.is_home_team as isHomeTeam,
            CASE
                WHEN ps.is_home_team = 1 THEN m.home_goals
                ELSE m.away_goals
            END as goalsScored,
            CASE
                WHEN ps.is_home_team = 1 THEN m.away_goals
                ELSE m.home_goals
            END as goalsConceded,
            ps.minutes_played as minutesPlayed,
            ps.fotmob_rating as fotmobRating,
            ps.total_fantasy_points as fantasyPoints,
            ps.goals,
            ps.assists,
            ps.total_shots as totalShots,
            ps.chances_created as chancesCreated,
            ps.expected_assists as expectedAssists,
            ps.total_passes as totalPasses,
            ps.accurate_passes as accuratePasses,
            ps.touches,
            ps.defensive_actions as defensiveActions,
            ps.tackles,
            ps.interceptions,
            ps.clearances,
            ps.blocks,
            ps.recoveries,
            ps.duels_won as duelsWon,
            ps.duels_lost as duelsLost,
            ps.yellow_cards as yellowCards,
            ps.red_cards as redCards,
            COALESCE(ms.accurate_crosses, 0) as totalCrosses,
            COALESCE(ms.expected_goals, COALESCE(fs.expected_goals, COALESCE(ds.expected_goals, NULL))) as expectedGoals,
            COALESCE(gs.saves, NULL) as saves,
            COALESCE(gs.goals_conceded, NULL) as goalsAllowed,
            ps.player_type as playerType
        FROM player_statistic ps
        LEFT JOIN matches m ON ps.match_id = m.id
        LEFT JOIN midfielder_statistic ms ON ps.id = ms.id
        LEFT JOIN forward_statistic fs ON ps.id = fs.id
        LEFT JOIN defender_statistic ds ON ps.id = ds.id
        LEFT JOIN goalkeeper_statistic gs ON ps.id = gs.id
        WHERE ps.player_id = :playerId
        ORDER BY m.round DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getPlayerMatchesSummary(@Param("playerId") String playerId);

    @Query(value = """
        SELECT 
            COUNT(*) as matches_played,
            player_type,
            COALESCE(SUM(goals), 0) as total_goals,
            COALESCE(SUM(assists), 0) as total_assists,
            COALESCE(SUM(minutes_played), 0) as total_minutes_played,
            COALESCE(SUM(total_fantasy_points), 0) as total_fantasy_points,
            COALESCE(SUM(total_shots), 0) as total_shots,
            COALESCE(SUM(accurate_passes), 0) as total_accurate_passes,
            COALESCE(SUM(total_passes), 0) as total_passes,
            COALESCE(SUM(chances_created), 0) as total_chances_created,
            COALESCE(SUM(defensive_actions), 0) as total_defensive_actions,
            COALESCE(SUM(touches), 0) as total_touches,
            COALESCE(SUM(tackles), 0) as total_tackles,
            COALESCE(SUM(interceptions), 0) as total_interceptions,
            COALESCE(SUM(duels_won), 0) as total_duels_won,
            COALESCE(SUM(duels_lost), 0) as total_duels_lost,
            COALESCE(SUM(yellow_cards), 0) as total_yellow_cards,
            COALESCE(SUM(red_cards), 0) as total_red_cards,
            COALESCE(AVG(fotmob_rating), 0) as avg_rating,
            COALESCE(AVG(expected_assists), 0) as avg_expected_assists,
            COALESCE(AVG(xg_and_xa), 0) as avg_xg_and_xa
        FROM player_statistic 
        WHERE player_id = :playerId
        GROUP BY player_type
        """, nativeQuery = true)
    Map<String, Object> getPlayerStatisticsSummaryData(@Param("playerId") String playerId);
}

