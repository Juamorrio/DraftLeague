package com.DraftLeague.repositories;
import com.DraftLeague.models.Team.TeamPlayerGameweekPoints;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import com.DraftLeague.models.Team.Team;

@Repository
public interface TeamPlayerGameweekPointsRepository extends JpaRepository<TeamPlayerGameweekPoints, Integer> {

    // Obtener todos los jugadores de un equipo en una jornada específica
    List<TeamPlayerGameweekPoints> findByTeamAndGameweek(Team team, Integer gameweek);

    // Obtener histórico de un jugador en un equipo
    List<TeamPlayerGameweekPoints> findByTeamAndPlayerId(Team team, String playerId);

    // Obtener histórico de un jugador por teamId (sin necesidad del objeto Team)
    List<TeamPlayerGameweekPoints> findByTeamIdAndPlayerId(Integer teamId, String playerId);

    // Verificar si ya existe registro
    Optional<TeamPlayerGameweekPoints> findByTeamAndPlayerIdAndGameweek(
        Team team, String playerId, Integer gameweek
    );

    // Obtener solo titulares de una jornada
    List<TeamPlayerGameweekPoints> findByTeamAndGameweekAndIsInLineupTrue(Team team, Integer gameweek);

    // Ordenados por puntos para un equipo/jornada
    List<TeamPlayerGameweekPoints> findByTeamAndGameweekOrderByPointsDesc(Team team, Integer gameweek);

    @Transactional
    void deleteAllByTeam(Team team);
}
