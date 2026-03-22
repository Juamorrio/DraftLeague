package com.DraftLeague.services;

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
        PlayerStatistic stat = new PlayerStatistic();
        stat.setPlayerType(playerType);
        return stat;
    }
}
