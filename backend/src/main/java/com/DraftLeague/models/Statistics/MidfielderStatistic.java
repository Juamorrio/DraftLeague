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

    public MidfielderStatistic() {
        super();
        this.setPlayerType(PlayerType.MIDFIELDER);
    }
}
