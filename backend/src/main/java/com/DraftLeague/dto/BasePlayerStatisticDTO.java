package com.DraftLeague.dto;

import com.DraftLeague.models.Statistics.PlayerStatistic.PlayerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.DraftLeague.models.Statistics.PlayerStatistic;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BasePlayerStatisticDTO {

    private Integer id;
    private String playerId;
    private Integer matchId;
    private Boolean isHomeTeam;
    private PlayerType playerType;
    private String role;

    private Double rating;
    private Integer minutesPlayed;
    private Integer totalFantasyPoints;

    private Integer goals;
    private Integer assists;
    private Integer totalShots;
    private Integer shotsOnTarget;
    private Integer chancesCreated;
    private Integer successfulDribbles;
    private Integer totalDribbles;
    private Integer dribbledPast;
    private Integer offsides;

    private Integer accuratePasses;
    private Integer totalPasses;
    private Double passAccuracy;
    private Integer accurateCrosses;
    private Integer totalCrosses;

    private Integer tackles;
    private Integer blocks;
    private Integer interceptions;

    private Integer duelsWon;
    private Integer duelsLost;

    private Integer wasFouled;
    private Integer foulsCommitted;
    private Integer yellowCards;
    private Integer redCards;

    private Integer penaltiesWon;
    private Integer penaltyScored;
    private Integer penaltyMissed;

    private Boolean isSubstitute;
    private Boolean isCaptain;
    private Integer shirtNumber;
    private Integer penaltyCommitted;
}
