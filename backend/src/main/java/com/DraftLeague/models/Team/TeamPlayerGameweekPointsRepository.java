package com.DraftLeague.models.Team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamPlayerGameweekPointsRepository extends JpaRepository<TeamPlayerGameweekPoints, Integer> {

    // Obtener todos los jugadores de un equipo en una jornada específica
    List<TeamPlayerGameweekPoints> findByTeamAndGameweek(Team team, Integer gameweek);

    // Obtener histórico de un jugador en un equipo
    List<TeamPlayerGameweekPoints> findByTeamAndPlayerId(Team team, String playerId);

    // Verificar si ya existe registro
    Optional<TeamPlayerGameweekPoints> findByTeamAndPlayerIdAndGameweek(
        Team team, String playerId, Integer gameweek
    );

    // Obtener solo titulares de una jornada
    List<TeamPlayerGameweekPoints> findByTeamAndGameweekAndIsInLineupTrue(Team team, Integer gameweek);

    // Ordenados por puntos para un equipo/jornada
    List<TeamPlayerGameweekPoints> findByTeamAndGameweekOrderByPointsDesc(Team team, Integer gameweek);
}
