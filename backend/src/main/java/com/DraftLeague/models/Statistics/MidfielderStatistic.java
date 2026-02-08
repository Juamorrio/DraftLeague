package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Midfielder-specific statistics.
 * Midfielders are playmakers with crossing, dribbling and attacking abilities.
 */
@Getter
@Setter
@Entity
@Table(name = "midfielder_statistic")
public class MidfielderStatistic extends PlayerStatistic {

    @Min(0)
    @Column(name = "accurate_crosses")
    private Integer accurateCrosses = 0;

    @Min(0)
    @Column(name = "total_crosses")
    private Integer totalCrosses = 0;

    @Min(0)
    @Column(name = "corners")
    private Integer corners = 0;

    @Min(0)
    @Column(name = "successful_dribbles")
    private Integer successfulDribbles = 0;

    @Min(0)
    @Column(name = "total_dribbles")
    private Integer totalDribbles = 0;

    @Column(name = "expected_goals")
    private Double expectedGoals;

    @Column(name = "expected_goals_on_target")
    private Double expectedGoalsOnTarget;

    @Column(name = "xg_non_penalty")
    private Double xgNonPenalty;

    @Min(0)
    @Column(name = "shots_on_target")
    private Integer shotsOnTarget = 0;

    @Min(0)
    @Column(name = "total_shots_on_target")
    private Integer totalShotsOnTarget = 0;

    @Min(0)
    @Column(name = "touches_opp_box")
    private Integer touchesOppBox = 0;

    @Min(0)
    @Column(name = "penalties_won")
    private Integer penaltiesWon = 0;

    @Min(0)
    @Column(name = "passes_into_final_third")
    private Integer passesIntoFinalThird = 0;

    @Min(0)
    @Column(name = "big_chances_missed")
    private Integer bigChancesMissed = 0;

    @Min(0)
    @Column(name = "headed_clearances")
    private Integer headedClearances = 0;

    public MidfielderStatistic() {
        super();
        this.setPlayerType(PlayerType.MIDFIELDER);
    }
}