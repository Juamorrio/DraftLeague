package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Defender-specific statistics.
 * Defenders can have attacking contributions.
 */
@Getter
@Setter
@Entity
@Table(name = "defender_statistic")
public class DefenderStatistic extends PlayerStatistic {

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
    @Column(name = "headed_clearances")
    private Integer headedClearances = 0;

    @Min(0)
    @Column(name = "blocked_shots")
    private Integer blockedShots = 0;

    public DefenderStatistic() {
        super();
        this.setPlayerType(PlayerType.DEFENDER);
    }
}