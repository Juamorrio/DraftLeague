package com.DraftLeague.models.Statistics;
import com.DraftLeague.models.Statistics.MidfielderStatistic;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.models.Statistics.PlayerStatistic;

/**
 * Midfielder-specific statistics.
 */
@Getter
@Setter
@Entity
@Table(name = "midfielder_statistic")
public class MidfielderStatistic extends PlayerStatistic {

    @Min(0)
    @Column(name = "successful_dribbles")
    private Integer successfulDribbles = 0;

    @Min(0)
    @Column(name = "total_dribbles")
    private Integer totalDribbles = 0;

    @Min(0)
    @Column(name = "penalties_won")
    private Integer penaltiesWon = 0;

    public MidfielderStatistic() {
        super();
        this.setPlayerType(PlayerType.MIDFIELDER);
    }
}
