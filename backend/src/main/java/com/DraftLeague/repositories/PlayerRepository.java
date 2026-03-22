package com.DraftLeague.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.repositories.PlayerRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, String> {
    boolean existsById(String id);

    @Query("SELECT p.id FROM Player p")
    List<String> findAllIds();

    List<Player> findByIdNotIn(List<String> ids);
}
