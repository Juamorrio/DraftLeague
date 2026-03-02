package com.DraftLeague.models.Team;
import com.DraftLeague.models.Team.TeamPlayerGameweekPoints;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Player.Position;

@Getter
@Setter
@Entity
@Table(name = "team_player_gameweek_points")
public class TeamPlayerGameweekPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Column(name = "gameweek", nullable = false)
    private Integer gameweek;

    // Datos del jugador (denormalizados para queries rÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡pidas)
    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "position", nullable = false)
    private String position; // POR, DEF, MID, DEL

    // PuntuaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n
    @Column(name = "points")
    private Integer points = 0; // Con multiplicador de capitÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡n

    @Column(name = "base_points")
    private Integer basePoints = 0; // Sin multiplicador

    @Column(name = "minutes_played")
    private Integer minutesPlayed = 0;

    @Column(name = "match_id")
    private Integer matchId;

    // Estado en el equipo
    @Column(name = "is_in_lineup")
    private Boolean isInLineup = false;

    @Column(name = "is_captain")
    private Boolean isCaptain = false;

    @Column(name = "is_benched")
    private Boolean isBenched = false;

    // Timestamps
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "calculated_at")
    private Date calculatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
