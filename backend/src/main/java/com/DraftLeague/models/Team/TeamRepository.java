package com.DraftLeague.models.Team;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.DraftLeague.models.user.User;
import com.DraftLeague.models.League.League;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {

    List<Team> findByUser(User user);
    List<Team> findByLeagueOrderByTotalPointsDesc(League league);
    List<Team> findByLeagueAndUser(League league, User user);
    long countByLeague(League league);
    
}
