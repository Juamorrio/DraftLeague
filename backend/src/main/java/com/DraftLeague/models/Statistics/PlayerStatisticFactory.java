package com.DraftLeague.models.Statistics;

import org.springframework.stereotype.Component;

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
