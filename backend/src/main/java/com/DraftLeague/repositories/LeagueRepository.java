package com.DraftLeague.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.League.League;
import com.DraftLeague.repositories.LeagueRepository;

@Repository
public interface LeagueRepository extends JpaRepository<League, Long> {
	Optional<League> findByCode(String code);
	List<League> findByCreatedBy(User createdBy);
}
