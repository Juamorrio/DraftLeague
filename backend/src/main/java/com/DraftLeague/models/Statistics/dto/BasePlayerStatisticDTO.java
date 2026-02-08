package com.DraftLeague.models.Statistics.dto;

import com.DraftLeague.models.Statistics.PlayerStatistic.PlayerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base DTO containing common fields for all player statistics.
 * Should be extended by position-specific DTOs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BasePlayerStatisticDTO {

    private Integer id;
    private Integer playerId;
    private Integer matchId;
    private Boolean isHomeTeam;
    private PlayerType playerType;
    private String role;
    
    private Double fotmobRating;
    private Integer minutesPlayed;
    private Integer totalFantasyPoints;
    
    private Integer goals;
    private Integer assists;
    private Integer totalShots;
    private Integer chancesCreated;
    
    private Integer accuratePasses;
    private Integer totalPasses;
    private Double passAccuracy;
    private Integer accurateLongBalls;
    private Integer totalLongBalls;
    
    private Double expectedAssists;
    private Double xgAndXa;
    
    private Integer touches;
    private Integer defensiveActions;
    private Integer dispossessed;
    
    private Integer tackles;
    private Integer blocks;
    private Integer clearances;
    private Integer interceptions;
    private Integer recoveries;
    private Integer dribbledPast;
    
    private Integer duelsWon;
    private Integer duelsLost;
    private Integer groundDuelsWon;
    private Integer totalGroundDuels;
    private Integer aerialDuelsWon;
    private Integer totalAerialDuels;
    
    private Integer wasFouled;
    private Integer foulsCommitted;
    private Integer yellowCards;
    private Integer redCards;
}
