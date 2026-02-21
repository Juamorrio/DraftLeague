package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Defender-specific statistics.
 */
@Getter
@Setter
@Entity
@Table(name = "defender_statistic")
public class DefenderStatistic extends PlayerStatistic {

    @Min(0)
    @Column(name = "penalties_won")
    private Integer penaltiesWon = 0;

    @Min(0)
    @Column(name = "successful_dribbles")
    private Integer successfulDribbles = 0;

    @Min(0)
    @Column(name = "total_dribbles")
    private Integer totalDribbles = 0;

    public DefenderStatistic() {
        super();
        this.setPlayerType(PlayerType.DEFENDER);
    }
}
