package com.DraftLeague.models.Team;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.DraftLeague.models.user.User;
import com.DraftLeague.models.League.League;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {

    List<Team> findByUser(User user);
    List<Team> findByLeagueOrderByTotalPointsDesc(League league);
    Team findByLeagueAndUser(League league, User user);
    List<Team> findByLeague(League league);
    long countByLeague(League league);
    
    @Query("SELECT DISTINCT t.league FROM Team t WHERE t.user = :user AND t.league IS NOT NULL")
    List<League> findDistinctLeaguesByUser(@Param("user") User user);
    
}
