package com.DraftLeague.repositories;

import com.DraftLeague.models.Gameweek.GameweekState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameweekStateRepository extends JpaRepository<GameweekState, Integer> {
}
