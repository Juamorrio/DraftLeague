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

    /** Count how many fantasy teams currently own a given player (demand indicator). */
    @Query("SELECT COUNT(pt) FROM PlayerTeam pt WHERE pt.player.id = :playerId")
    long countByPlayerId(@Param("playerId") String playerId);

    /** Find all PlayerTeam entries for a given player so sell price can be refreshed. */
    List<PlayerTeam> findByPlayer(Player player);

    /** Bulk-update sell price for all teams that own a specific player. */
    @Transactional
    @Modifying
    @Query("UPDATE PlayerTeam pt SET pt.sellPrice = :newPrice WHERE pt.player.id = :playerId")
    void updateSellPriceByPlayerId(@Param("playerId") String playerId, @Param("newPrice") int newPrice);
}
