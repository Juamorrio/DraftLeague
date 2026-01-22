package com.DraftLeague.models.Player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.DraftLeague.models.Team.Team;

import java.util.List;

@Repository
public interface PlayerTeamRepository extends JpaRepository<PlayerTeam, Integer> {
    
    List<PlayerTeam> findByTeam(Team team);
    
    void deleteByTeam(Team team);
    
    boolean existsByTeamAndPlayer(Team team, Player player);
}
