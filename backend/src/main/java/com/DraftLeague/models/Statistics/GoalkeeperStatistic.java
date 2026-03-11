package com.DraftLeague.models.Statistics;
import com.DraftLeague.models.Statistics.GoalkeeperStatistic;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.models.Statistics.PlayerStatistic;

/**
 * Goalkeeper-specific statistics.
 */
@Getter
@Setter
@Entity
@Table(name = "goalkeeper_statistic")
public class GoalkeeperStatistic extends PlayerStatistic {

    public GoalkeeperStatistic() {
        super();
        this.setPlayerType(PlayerType.GOALKEEPER);
    }
}
