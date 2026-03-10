package com.DraftLeague.services;

import com.DraftLeague.models.Statistics.DefenderStatistic;
import com.DraftLeague.models.Statistics.ForwardStatistic;
import com.DraftLeague.models.Statistics.GoalkeeperStatistic;
import com.DraftLeague.models.Statistics.MidfielderStatistic;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import org.springframework.stereotype.Component;

@Component
public class PlayerStatisticFactory {

    public PlayerStatistic createStatistic(String playerType) {
        if (playerType == null) {
            throw new IllegalArgumentException("Player type cannot be null");
        }
        return createStatistic(PlayerStatistic.PlayerType.valueOf(playerType.toUpperCase()));
    }

    public PlayerStatistic createStatistic(PlayerStatistic.PlayerType playerType) {
        if (playerType == null) {
            throw new IllegalArgumentException("Player type cannot be null");
        }
        return switch (playerType) {
            case GOALKEEPER -> new GoalkeeperStatistic();
            case DEFENDER -> new DefenderStatistic();
            case MIDFIELDER -> new MidfielderStatistic();
            case FORWARD -> new ForwardStatistic();
        };
    }
}
