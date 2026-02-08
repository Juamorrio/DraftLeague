package com.DraftLeague.models.Statistics.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MidfielderStatisticDTO extends BasePlayerStatisticDTO {
    
    private Integer accurateCrosses;
    private Integer totalCrosses;
    private Integer corners;
    
    private Integer successfulDribbles;
    private Integer totalDribbles;
    
    private Double expectedGoals;
    private Double expectedGoalsOnTarget;
    private Double xgNonPenalty;
    private Integer shotsOnTarget;
    private Integer totalShotsOnTarget;
    private Integer touchesOppBox;
    private Integer penaltiesWon;
    private Integer passesIntoFinalThird;
    private Integer bigChancesMissed;
    
    private Integer headedClearances;
}
