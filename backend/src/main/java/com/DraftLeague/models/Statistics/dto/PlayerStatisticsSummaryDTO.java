package com.DraftLeague.models.Statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatisticsSummaryDTO {

    private String playerId;
    private String playerName;
    private String playerType;
    private Integer matchesPlayed;

    private Integer totalGoals;
    private Integer totalAssists;
    private Integer totalMinutesPlayed;
    private Integer totalFantasyPoints;

    private Double avgRating;
    private Double avgMinutesPlayed;
    private Double avgGoals;
    private Double avgAssists;
    private Double avgShots;
    private Double avgPassAccuracy;
    private Double avgDuelsWon;
    private Double avgFantasyPoints;

    private Integer totalShots;
    private Integer totalAccuratePasses;
    private Integer totalPasses;
    private Integer totalChancesCreated;
    private Integer totalTackles;
    private Integer totalInterceptions;
    private Integer totalDuelsWon;
    private Integer totalDuelsLost;
    private Integer totalYellowCards;
    private Integer totalRedCards;
    private Integer totalPenaltyCommitted;
    private Integer timesCaptain;
}
