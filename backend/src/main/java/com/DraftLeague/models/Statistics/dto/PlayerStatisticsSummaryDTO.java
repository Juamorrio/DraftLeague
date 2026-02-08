package com.DraftLeague.models.Statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatisticsSummaryDTO {

    private Integer playerId;
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
    private Double avgTouches;
    private Double avgDefensiveActions;
    private Double avgDuelsWon;
    private Double avgFantasyPoints;
    
    private Integer totalShots;
    private Integer totalAccuratePasses;
    private Integer totalPasses;
    private Integer totalChancesCreated;
    private Integer totalDefensiveActions;
    private Integer totalTouches;
    private Integer totalTackles;
    private Integer totalInterceptions;
    private Integer totalDuelsWon;
    private Integer totalDuelsLost;
    private Integer totalYellowCards;
    private Integer totalRedCards;
    
    private Double avgExpectedGoals;
    private Double avgExpectedAssists;
    private Double avgXgAndXa;
}
