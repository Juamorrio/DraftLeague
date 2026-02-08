package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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
    private Integer playerId;

    @NotNull
    @Column(name = "match_id", nullable = false)
    private Integer matchId;

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

    @Column(name = "total_fantasy_points")
    private Integer totalFantasyPoints = 0;

    public enum PlayerType {
        GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD
    }
}