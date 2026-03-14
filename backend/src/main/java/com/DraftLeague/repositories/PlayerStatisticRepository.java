package com.DraftLeague.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.PlayerStatisticRepository;

@Repository
public interface PlayerStatisticRepository extends JpaRepository<PlayerStatistic, Integer> {

    List<PlayerStatistic> findByPlayerId(String playerId);

    List<PlayerStatistic> findByMatchId(Integer matchId);

    List<PlayerStatistic> findByPlayerType(PlayerStatistic.PlayerType playerType);

    @Query("SELECT ps FROM PlayerStatistic ps WHERE ps.playerId = :playerId AND ps.matchId IN :matchIds")
    Optional<PlayerStatistic> findByPlayerIdAndMatchIdIn(@Param("playerId") String playerId, @Param("matchIds") Set<Integer> matchIds);

    @Query(value = """
        SELECT ps.*
        FROM player_statistic ps
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
            ps.rating as rating,
            ps.total_fantasy_points as fantasyPoints,
            ps.goals,
            ps.assists,
            ps.total_shots as totalShots,
            ps.shots_on_target as shotsOnTarget,
            ps.chances_created as chancesCreated,
            ps.total_passes as totalPasses,
            ps.accurate_passes as accuratePasses,
            ps.tackles,
            ps.interceptions,
            ps.blocks,
            ps.duels_won as duelsWon,
            ps.duels_lost as duelsLost,
            ps.yellow_cards as yellowCards,
            ps.red_cards as redCards,
            ps.saves as saves,
            ps.goals_conceded as goalsAllowed,
            ps.player_type as playerType,
            ps.is_captain as isCaptain,
            ps.shirt_number as shirtNumber,
            ps.penalty_committed as penaltyCommitted
        FROM player_statistic ps
        LEFT JOIN matches m ON ps.match_id = m.id
        WHERE ps.player_id = :playerId
        ORDER BY m.round DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getPlayerMatchesSummary(@Param("playerId") String playerId);


    @Query("SELECT ps FROM PlayerStatistic ps WHERE ps.playerId = :playerId ORDER BY ps.matchId DESC")
    List<PlayerStatistic> findRecentStatsByPlayerId(@Param("playerId") String playerId, Pageable pageable);

    @Query(value = """
        SELECT
            COALESCE(SUM(recent.yellow_cards), 0) AS total_yellow_cards,
            COALESCE(SUM(recent.red_cards), 0)    AS total_red_cards
        FROM (
            SELECT yellow_cards, red_cards
            FROM player_statistic
            WHERE player_id = :playerId
            ORDER BY match_id DESC
            LIMIT :limit
        ) recent
        """, nativeQuery = true)
    Map<String, Object> findRecentDisciplineByPlayerId(@Param("playerId") String playerId,
                                                       @Param("limit") int limit);

    @Query(value = """
        SELECT
            COUNT(*) as matches_played,
            COALESCE(SUM(goals), 0) as total_goals,
            COALESCE(SUM(assists), 0) as total_assists,
            COALESCE(SUM(minutes_played), 0) as total_minutes_played,
            COALESCE(SUM(total_fantasy_points), 0) as total_fantasy_points,
            COALESCE(SUM(total_shots), 0) as total_shots,
            COALESCE(SUM(accurate_passes), 0) as total_accurate_passes,
            COALESCE(SUM(total_passes), 0) as total_passes,
            COALESCE(SUM(chances_created), 0) as total_chances_created,
            COALESCE(SUM(tackles), 0) as total_tackles,
            COALESCE(SUM(interceptions), 0) as total_interceptions,
            COALESCE(SUM(duels_won), 0) as total_duels_won,
            COALESCE(SUM(duels_lost), 0) as total_duels_lost,
            COALESCE(SUM(yellow_cards), 0) as total_yellow_cards,
            COALESCE(SUM(red_cards), 0) as total_red_cards,
            COALESCE(SUM(penalty_committed), 0) as total_penalty_committed,
            SUM(CASE WHEN is_captain = 1 THEN 1 ELSE 0 END) as times_captain,
            COALESCE(AVG(rating), 0) as avg_rating
        FROM player_statistic
        WHERE player_id = :playerId
        """, nativeQuery = true)
    Map<String, Object> getPlayerStatisticsSummaryData(@Param("playerId") String playerId);

    @Query(value = """
        SELECT
            player_id,
            COUNT(*) as matches_played,
            COALESCE(SUM(goals), 0) as total_goals,
            COALESCE(SUM(assists), 0) as total_assists,
            COALESCE(SUM(minutes_played), 0) as total_minutes_played,
            COALESCE(SUM(total_fantasy_points), 0) as total_fantasy_points,
            COALESCE(SUM(total_shots), 0) as total_shots,
            COALESCE(SUM(accurate_passes), 0) as total_accurate_passes,
            COALESCE(SUM(total_passes), 0) as total_passes,
            COALESCE(SUM(chances_created), 0) as total_chances_created,
            COALESCE(SUM(tackles), 0) as total_tackles,
            COALESCE(SUM(interceptions), 0) as total_interceptions,
            COALESCE(SUM(duels_won), 0) as total_duels_won,
            COALESCE(SUM(duels_lost), 0) as total_duels_lost,
            COALESCE(SUM(yellow_cards), 0) as total_yellow_cards,
            COALESCE(SUM(red_cards), 0) as total_red_cards,
            COALESCE(SUM(penalty_committed), 0) as total_penalty_committed,
            SUM(CASE WHEN is_captain = 1 THEN 1 ELSE 0 END) as times_captain,
            COALESCE(AVG(rating), 0) as avg_rating
        FROM player_statistic
        GROUP BY player_id
        """, nativeQuery = true)
    List<Map<String, Object>> getAllPlayerStatisticsSummaryData();

    @Query(value = """
        SELECT player_id, match_id, minutes_played, rating, total_fantasy_points,
               goals, assists, shots_on_target, chances_created, tackles,
               interceptions, blocks, saves, goals_conceded, clean_sheet
        FROM (
            SELECT player_id, match_id, minutes_played, rating, total_fantasy_points,
                   goals, assists, shots_on_target, chances_created, tackles,
                   interceptions, blocks, saves, goals_conceded, clean_sheet,
                   ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY match_id DESC) AS rn
            FROM player_statistic
        ) ranked
        WHERE rn <= :limit
        """, nativeQuery = true)
    List<Map<String, Object>> findRecentStatsForAllPlayers(@Param("limit") int limit);


    @Query(value = """
        SELECT player_id,
               COALESCE(SUM(yellow_cards), 0) AS total_yellow_cards,
               COALESCE(SUM(red_cards), 0)    AS total_red_cards
        FROM (
            SELECT player_id, yellow_cards, red_cards,
                   ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY match_id DESC) AS rn
            FROM player_statistic
        ) ranked
        WHERE rn <= :limit
        GROUP BY player_id
        """, nativeQuery = true)
    List<Map<String, Object>> findRecentDisciplineForAllPlayers(@Param("limit") int limit);
}
