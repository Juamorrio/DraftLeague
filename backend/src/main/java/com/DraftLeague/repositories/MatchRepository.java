package com.DraftLeague.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.MatchRepository;

@Repository
public interface MatchRepository extends JpaRepository<Match, Integer> {
    Optional<Match> findByApiFootballFixtureId(Integer apiFootballFixtureId);
    List<Match> findByStatus(MatchStatus status);
    List<Match> findByStatusOrderByRoundAsc(MatchStatus status);
    List<Match> findByRound(Integer round);
    Optional<Match> findByRoundAndHomeTeamIdAndAwayTeamId(Integer round, Integer homeTeamId, Integer awayTeamId);
    Optional<Match> findFirstByStatusAndHomeTeamIdOrderByRoundAsc(MatchStatus status, Integer homeTeamId);
    Optional<Match> findFirstByStatusAndAwayTeamIdOrderByRoundAsc(MatchStatus status, Integer awayTeamId);
}
