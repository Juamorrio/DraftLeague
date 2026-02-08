package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Goalkeeper-specific statistics.
 * Goalkeepers have specific passing behavior and saving abilities.
 */
@Getter
@Setter
@Entity
@Table(name = "goalkeeper_statistic")
public class GoalkeeperStatistic extends PlayerStatistic {

    @Min(0)
    @Column(name = "passes_into_final_third")
    private Integer passesIntoFinalThird = 0;

    @Min(0)
    @Column(name = "saves")
    private Integer saves = 0;

    @Min(0)
    @Column(name = "goals_conceded")
    private Integer goalsConceded = 0;

    @Column(name = "xgot_faced")
    private Double xgotFaced;

    @Column(name = "goals_prevented")
    private Double goalsPrevented;

    @Min(0)
    @Column(name = "sweeper_actions")
    private Integer sweeperActions = 0;

    @Min(0)
    @Column(name = "high_claims")
    private Integer highClaims = 0;

    @Min(0)
    @Column(name = "diving_saves")
    private Integer divingSaves = 0;

    @Min(0)
    @Column(name = "saves_inside_box")
    private Integer savesInsideBox = 0;

    @Min(0)
    @Column(name = "punches")
    private Integer punches = 0;

    @Min(0)
    @Column(name = "throws")
    private Integer goalKeeperThrows = 0;

    public GoalkeeperStatistic() {
        super();
        this.setPlayerType(PlayerType.GOALKEEPER);
    }
}