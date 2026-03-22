package com.DraftLeague.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Query("SELECT COUNT(pt) FROM PlayerTeam pt WHERE pt.player.id = :playerId")
    long countByPlayerId(@Param("playerId") String playerId);

    List<PlayerTeam> findByPlayer(Player player);

    @Transactional
    @Modifying
    @Query("UPDATE PlayerTeam pt SET pt.sellPrice = :newPrice WHERE pt.player.id = :playerId")
    void updateSellPriceByPlayerId(@Param("playerId") String playerId, @Param("newPrice") int newPrice);

    @Query("SELECT pt.player.id, COUNT(pt) FROM PlayerTeam pt GROUP BY pt.player.id")
    List<Object[]> countOwnershipForAllPlayers();
}
