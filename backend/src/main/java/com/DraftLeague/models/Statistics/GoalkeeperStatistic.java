package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Goalkeeper-specific statistics.
 */
@Getter
@Setter
@Entity
@Table(name = "goalkeeper_statistic")
public class GoalkeeperStatistic extends PlayerStatistic {

    @Min(0)
    @Column(name = "saves")
    private Integer saves = 0;

    @Min(0)
    @Column(name = "goals_conceded")
    private Integer goalsConceded = 0;

    public GoalkeeperStatistic() {
        super();
        this.setPlayerType(PlayerType.GOALKEEPER);
    }
}
