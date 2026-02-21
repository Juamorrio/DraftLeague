package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Forward-specific statistics.
 */
@Getter
@Setter
@Entity
@Table(name = "forward_statistic")
public class ForwardStatistic extends PlayerStatistic {

    @Min(0)
    @Column(name = "successful_dribbles")
    private Integer successfulDribbles = 0;

    @Min(0)
    @Column(name = "total_dribbles")
    private Integer totalDribbles = 0;

    @Min(0)
    @Column(name = "penalties_won")
    private Integer penaltiesWon = 0;

    @Min(0)
    @Column(name = "offsides")
    private Integer offsides = 0;

    public ForwardStatistic() {
        super();
        this.setPlayerType(PlayerType.FORWARD);
    }
}
