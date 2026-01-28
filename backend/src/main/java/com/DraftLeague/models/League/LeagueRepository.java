package com.DraftLeague.models.League;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.DraftLeague.models.user.User;

public interface LeagueRepository extends JpaRepository<League, Long> {
	Optional<League> findByCode(String code);
	List<League> findByCreatedBy(User createdBy);
}
