package com.DraftLeague.models.League;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Long> {
	Optional<League> findByCode(String code);
}
