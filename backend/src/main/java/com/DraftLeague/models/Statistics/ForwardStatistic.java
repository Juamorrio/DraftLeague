package com.DraftLeague.models.Statistics;
import com.DraftLeague.models.Statistics.ForwardStatistic;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.models.Statistics.PlayerStatistic;

/**
 * Forward-specific statistics.
 */
@Getter
@Setter
@Entity
@Table(name = "forward_statistic")
public class ForwardStatistic extends PlayerStatistic {

    public ForwardStatistic() {
        super();
        this.setPlayerType(PlayerType.FORWARD);
    }
}
