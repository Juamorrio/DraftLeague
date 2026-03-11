package com.DraftLeague.models.Statistics;
import com.DraftLeague.models.Statistics.DefenderStatistic;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.models.Statistics.PlayerStatistic;

/**
 * Defender-specific statistics.
 */
@Getter
@Setter
@Entity
@Table(name = "defender_statistic")
public class DefenderStatistic extends PlayerStatistic {

    public DefenderStatistic() {
        super();
        this.setPlayerType(PlayerType.DEFENDER);
    }
}
