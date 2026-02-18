package com.DraftLeague.models.Team;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeamGameweekPointsRepository extends JpaRepository<TeamGameweekPoints, Integer> {

    Optional<TeamGameweekPoints> findByTeamAndGameweek(Team team, Integer gameweek);

    List<TeamGameweekPoints> findByTeamOrderByGameweekAsc(Team team);

    List<TeamGameweekPoints> findByGameweek(Integer gameweek);
}
