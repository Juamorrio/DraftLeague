package com.DraftLeague.services;
import com.DraftLeague.models.Statistics.GoalkeeperStatistic;
import com.DraftLeague.models.Statistics.DefenderStatistic;
import com.DraftLeague.models.Statistics.MidfielderStatistic;
import com.DraftLeague.models.Statistics.ForwardStatistic;
import com.DraftLeague.services.PlayerStatisticFactory;

import org.springframework.stereotype.Component;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Statistics.PlayerStatistic;

@Component
public class PlayerStatisticFactory {

    public PlayerStatistic createStatistic(String playerType) {
        if (playerType == null) {
            throw new IllegalArgumentException("Player type cannot be null");
        }

        return switch (playerType.toUpperCase()) {
            case "GOALKEEPER" -> new GoalkeeperStatistic();
            case "DEFENDER" -> new DefenderStatistic();
            case "MIDFIELDER" -> new MidfielderStatistic();
            case "FORWARD" -> new ForwardStatistic();
            default -> throw new IllegalArgumentException("Unknown player type: " + playerType);
        };
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
