package com.DraftLeague.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.repositories.PlayerRepository;

public interface PlayerRepository extends JpaRepository<Player, String> {
    boolean existsById(String id);
}
