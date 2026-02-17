package com.DraftLeague.models.Statistics;

import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Match.Match;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "player_statistic")
public abstract class PlayerStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "player_id", nullable = false)
    private String playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    @NotNull
    @Column(name = "match_id", nullable = false)
    private Integer matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", insertable = false, updatable = false)
    private Match match;

    @NotNull
    @Column(name = "is_home_team", nullable = false)
    private Boolean isHomeTeam;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "player_type", nullable = false)
    private PlayerType playerType;

    @Column(name = "role")
    private String role;

    @Column(name = "fotmob_rating")
    private Double fotmobRating;

    @NotNull
    @Min(0)
    @Column(name = "minutes_played", nullable = false)
    private Integer minutesPlayed;

    @Min(0)
    @Column(name = "goals")
    private Integer goals = 0;

    @Min(0)
    @Column(name = "assists")
    private Integer assists = 0;

    @Min(0)
    @Column(name = "total_shots")
    private Integer totalShots = 0;

    @Min(0)
    @Column(name = "accurate_passes")
    private Integer accuratePasses = 0;

    @Min(0)
    @Column(name = "total_passes")
    private Integer totalPasses = 0;

    @Min(0)
    @Column(name = "chances_created")
    private Integer chancesCreated = 0;

    @Column(name = "expected_assists")
    private Double expectedAssists;

    @Column(name = "xg_and_xa")
    private Double xgAndXa;

    @Min(0)
    @Column(name = "defensive_actions")
    private Integer defensiveActions = 0;

    @Min(0)
    @Column(name = "touches")
    private Integer touches = 0;

    @Min(0)
    @Column(name = "accurate_long_balls")
    private Integer accurateLongBalls = 0;

    @Min(0)
    @Column(name = "total_long_balls")
    private Integer totalLongBalls = 0;

    @Min(0)
    @Column(name = "dispossessed")
    private Integer dispossessed = 0;

    @Min(0)
    @Column(name = "tackles")
    private Integer tackles = 0;

    @Min(0)
    @Column(name = "blocks")
    private Integer blocks = 0;

    @Min(0)
    @Column(name = "clearances")
    private Integer clearances = 0;

    @Min(0)
    @Column(name = "interceptions")
    private Integer interceptions = 0;

    @Min(0)
    @Column(name = "recoveries")
    private Integer recoveries = 0;

    @Min(0)
    @Column(name = "dribbled_past")
    private Integer dribbledPast = 0;

    @Min(0)
    @Column(name = "duels_won")
    private Integer duelsWon = 0;

    @Min(0)
    @Column(name = "duels_lost")
    private Integer duelsLost = 0;

    @Min(0)
    @Column(name = "ground_duels_won")
    private Integer groundDuelsWon = 0;

    @Min(0)
    @Column(name = "total_ground_duels")
    private Integer totalGroundDuels = 0;

    @Min(0)
    @Column(name = "aerial_duels_won")
    private Integer aerialDuelsWon = 0;

    @Min(0)
    @Column(name = "total_aerial_duels")
    private Integer totalAerialDuels = 0;

    @Min(0)
    @Column(name = "was_fouled")
    private Integer wasFouled = 0;

    @Min(0)
    @Column(name = "fouls_committed")
    private Integer foulsCommitted = 0;

    @Min(0)
    @Column(name = "yellow_cards")
    private Integer yellowCards = 0;

    @Min(0)
    @Column(name = "red_cards")
    private Integer redCards = 0;

    // Goalkeeper specific fields (available for all, but mainly used by GK)
    @Min(0)
    @Column(name = "saves")
    private Integer saves;

    @Min(0)
    @Column(name = "saves_inside_box")
    private Integer savesInsideBox;

    @Min(0)
    @Column(name = "penalties_saved")
    private Integer penaltiesSaved;

    @Column(name = "clean_sheet")
    private Boolean cleanSheet;

    @Min(0)
    @Column(name = "goals_conceded")
    private Integer goalsConceded;

    @Column(name = "total_fantasy_points")
    private Integer totalFantasyPoints = 0;

    /**
     * Calcula los puntos fantasy basándose en las estadísticas del jugador
     * Sistema de puntuación inspirado en LaLiga Fantasy
     */
    public int calculateFantasyPoints() {
        int points = 0;

        // Puntos base por minutos jugados
        if (minutesPlayed >= 60) {
            points += 2;  // Jugó más de 60 minutos
        } else if (minutesPlayed > 0) {
            points += 1;  // Jugó menos de 60 minutos
        }

        // Goles (varía según posición)
        if (goals != null && goals > 0) {
            switch (playerType) {
                case GOALKEEPER:
                case DEFENDER:
                    points += goals * 6;  // Gol de defensa/portero vale más
                    break;
                case MIDFIELDER:
                    points += goals * 5;
                    break;
                case FORWARD:
                    points += goals * 4;  // Gol de delantero vale menos
                    break;
            }
        }

        // Asistencias
        if (assists != null && assists > 0) {
            points += assists * 3;
        }

        // Bonus por rating
        if (fotmobRating != null) {
            if (fotmobRating >= 9.0) {
                points += 5;  // Rendimiento excepcional
            } else if (fotmobRating >= 8.0) {
                points += 3;  // Muy buen rendimiento
            } else if (fotmobRating >= 7.0) {
                points += 1;  // Buen rendimiento
            } else if (fotmobRating < 5.0) {
                points -= 1;  // Mal rendimiento
            }
        }

        // Tarjetas
        if (yellowCards != null && yellowCards > 0) {
            points -= yellowCards;  // -1 por amarilla
        }
        if (redCards != null && redCards > 0) {
            points -= redCards * 3;  // -3 por roja
        }

        // Acciones defensivas (para defensores y mediocampistas)
        if (playerType == PlayerType.DEFENDER || playerType == PlayerType.MIDFIELDER) {
            if (tackles != null) points += tackles / 3;  // +1 cada 3 tackles
            if (interceptions != null) points += interceptions / 3;  // +1 cada 3 intercepciones
            if (clearances != null) points += clearances / 4;  // +1 cada 4 despejes
        }

        // Duelos ganados (todos los jugadores)
        if (duelsWon != null && duelsWon >= 5) {
            points += 1;  // Bonus por ganar muchos duelos
        }

        // Penalización por pérdidas de balón excesivas
        if (dispossessed != null && dispossessed >= 3) {
            points -= 1;
        }

        // BONIFICACIONES ESPECIALES
        // Clean Sheet (portero/defensa sin goles recibidos y jugó >60 min)
        if ((playerType == PlayerType.GOALKEEPER || playerType == PlayerType.DEFENDER)
            && minutesPlayed >= 60) {
            if (cleanSheet != null && cleanSheet) {
                points += 4; // +4 por clean sheet
            }
        }

        // Hat-trick (3+ goles)
        if (goals != null && goals >= 3) {
            points += 5; // +5 bonus por hat-trick
        }

        // Penalti parado (solo porteros)
        if (playerType == PlayerType.GOALKEEPER && penaltiesSaved != null && penaltiesSaved > 0) {
            points += penaltiesSaved * 5; // +5 por cada penalti parado
        }

        // Asistencia doble (2 asistencias)
        if (assists != null && assists == 2) {
            points += 3; // +3 bonus
        }

        // Asistencia triple o más (3+ asistencias)
        if (assists != null && assists >= 3) {
            points += 8; // +8 bonus (total +11 con el anterior)
        }

        // Múltiples goles concedidos (penalización para porteros)
        if (playerType == PlayerType.GOALKEEPER && goalsConceded != null && goalsConceded >= 3) {
            points -= 2; // -2 por recibir 3 o más goles
        }

        // Asegurar que no sea negativo
        return Math.max(points, 0);
    }

    /**
     * Calcula un desglose detallado de los puntos fantasy por cada concepto.
     * Solo incluye conceptos con valor != 0.
     */
    public Map<String, Integer> calculateFantasyPointsBreakdown() {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        // Puntos base por minutos jugados
        if (minutesPlayed >= 60) {
            breakdown.put("minutesPlayed", 2);
            total += 2;
        } else if (minutesPlayed > 0) {
            breakdown.put("minutesPlayed", 1);
            total += 1;
        }

        // Goles (varía según posición)
        if (goals != null && goals > 0) {
            int goalPoints = 0;
            switch (playerType) {
                case GOALKEEPER:
                case DEFENDER:
                    goalPoints = goals * 6;
                    break;
                case MIDFIELDER:
                    goalPoints = goals * 5;
                    break;
                case FORWARD:
                    goalPoints = goals * 4;
                    break;
            }
            if (goalPoints != 0) {
                breakdown.put("goals", goalPoints);
                total += goalPoints;
            }
        }

        // Asistencias
        if (assists != null && assists > 0) {
            int assistPoints = assists * 3;
            breakdown.put("assists", assistPoints);
            total += assistPoints;
        }

        // Bonus por rating
        if (fotmobRating != null) {
            int ratingPoints = 0;
            if (fotmobRating >= 9.0) {
                ratingPoints = 5;
            } else if (fotmobRating >= 8.0) {
                ratingPoints = 3;
            } else if (fotmobRating >= 7.0) {
                ratingPoints = 1;
            } else if (fotmobRating < 5.0) {
                ratingPoints = -1;
            }
            if (ratingPoints != 0) {
                breakdown.put("ratingBonus", ratingPoints);
                total += ratingPoints;
            }
        }

        // Tarjetas
        if (yellowCards != null && yellowCards > 0) {
            int ycPoints = -yellowCards;
            breakdown.put("yellowCards", ycPoints);
            total += ycPoints;
        }
        if (redCards != null && redCards > 0) {
            int rcPoints = -redCards * 3;
            breakdown.put("redCards", rcPoints);
            total += rcPoints;
        }

        // Acciones defensivas (para defensores y mediocampistas)
        if (playerType == PlayerType.DEFENDER || playerType == PlayerType.MIDFIELDER) {
            int defPoints = 0;
            if (tackles != null) defPoints += tackles / 3;
            if (interceptions != null) defPoints += interceptions / 3;
            if (clearances != null) defPoints += clearances / 4;
            if (defPoints != 0) {
                breakdown.put("defensiveActions", defPoints);
                total += defPoints;
            }
        }

        // Duelos ganados
        if (duelsWon != null && duelsWon >= 5) {
            breakdown.put("duelsBonus", 1);
            total += 1;
        }

        // Penalización por pérdidas de balón
        if (dispossessed != null && dispossessed >= 3) {
            breakdown.put("dispossessedPenalty", -1);
            total -= 1;
        }

        // Clean Sheet
        if ((playerType == PlayerType.GOALKEEPER || playerType == PlayerType.DEFENDER)
            && minutesPlayed >= 60) {
            if (cleanSheet != null && cleanSheet) {
                breakdown.put("cleanSheet", 4);
                total += 4;
            }
        }

        // Hat-trick
        if (goals != null && goals >= 3) {
            breakdown.put("hatTrick", 5);
            total += 5;
        }

        // Penalti parado
        if (playerType == PlayerType.GOALKEEPER && penaltiesSaved != null && penaltiesSaved > 0) {
            int penPoints = penaltiesSaved * 5;
            breakdown.put("penaltySaved", penPoints);
            total += penPoints;
        }

        // Asistencia doble
        if (assists != null && assists == 2) {
            breakdown.put("doubleAssist", 3);
            total += 3;
        }

        // Asistencia triple o más
        if (assists != null && assists >= 3) {
            breakdown.put("tripleAssist", 8);
            total += 8;
        }

        // Múltiples goles concedidos
        if (playerType == PlayerType.GOALKEEPER && goalsConceded != null && goalsConceded >= 3) {
            breakdown.put("multipleGoalsConceded", -2);
            total -= 2;
        }

        breakdown.put("total", Math.max(total, 0));
        return breakdown;
    }

    public enum PlayerType {
        GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD
    }
}