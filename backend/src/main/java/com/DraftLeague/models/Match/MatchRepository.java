package com.DraftLeague.models.Match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Integer> {
    Optional<Match> findByFotmobMatchId(Integer fotmobMatchId);
    List<Match> findByStatus(MatchStatus status);
    Optional<Match> findByRoundAndHomeTeamIdAndAwayTeamId(Integer round, Integer homeTeamId, Integer awayTeamId);
}
