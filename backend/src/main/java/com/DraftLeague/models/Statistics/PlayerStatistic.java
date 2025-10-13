package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PlayerStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @NotNull
    @Min(0)
    @Column(name = "minutes_played", nullable = false)
    private Integer minutesPlayed;

    @NotNull
    @Min(0)
    @Column(name = "goals", nullable = false)
    private Integer goals;

    @NotNull
    @Min(0)
    @Column(name = "assists", nullable = false)
    private Integer assists;

    @Min(0)
    @Column(name = "shots")
    private Integer shots;

    @Min(0)
    @Column(name = "shots_on_target")
    private Integer shotsOnTarget;

    @Min(0)
    @Column(name = "fouls_drawn")
    private Integer foulsDrawn;

    @Min(0)
    @Column(name = "fouls_committed")
    private Integer foulsCommitted;

    @Min(0)
    @Column(name = "own_goals")
    private Integer ownGoals;

    @NotNull
    @Min(0)
    @Column(name = "yellow_cards", nullable = false)
    private Integer yellowCards;

    @NotNull
    @Min(0)
    @Column(name = "red_cards", nullable = false)
    private Integer redCards;

    @Min(0)
    @Column(name = "rating_avg")
    private Double ratingAvg;

    @NotNull
    @Min(0)
    @Column(name = "total_fantasy_points", nullable = false)
    private Integer totalFantasyPoints;
}