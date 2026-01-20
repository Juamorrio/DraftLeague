package com.DraftLeague.models.Player;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Integer> {
boolean existsById(Integer id);
}
