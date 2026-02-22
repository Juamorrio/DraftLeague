package com.DraftLeague.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.DraftLeague.models.Team.Team;

import java.util.List;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.repositories.PlayerTeamRepository;

@Repository
public interface PlayerTeamRepository extends JpaRepository<PlayerTeam, Integer> {
    
    List<PlayerTeam> findByTeam(Team team);
    
    void deleteByTeam(Team team);
    
    boolean existsByTeamAndPlayer(Team team, Player player);

    List<PlayerTeam> findPlayerTeamsByTeamIdIn(List<Integer> teamIds);
}
