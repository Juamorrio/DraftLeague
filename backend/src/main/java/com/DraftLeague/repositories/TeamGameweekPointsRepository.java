package com.DraftLeague.repositories;
import com.DraftLeague.models.Team.TeamGameweekPoints;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.repositories.TeamGameweekPointsRepository;

public interface TeamGameweekPointsRepository extends JpaRepository<TeamGameweekPoints, Integer> {

    Optional<TeamGameweekPoints> findByTeamAndGameweek(Team team, Integer gameweek);

    List<TeamGameweekPoints> findByTeamOrderByGameweekAsc(Team team);

    List<TeamGameweekPoints> findByGameweek(Integer gameweek);

    @Transactional
    void deleteAllByTeam(Team team);
}
