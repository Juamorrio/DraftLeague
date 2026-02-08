package com.DraftLeague.models.Statistics.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO for forward-specific statistics.
 * Pure attacking focus on goal-scoring and chance creation.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ForwardStatisticDTO extends BasePlayerStatisticDTO {
    
    private Integer successfulDribbles;
    private Integer totalDribbles;
    
    private Double expectedGoals;
    private Double expectedGoalsOnTarget;
    private Double xgNonPenalty;
    private Integer shotsOnTarget;
    private Integer totalShotsOnTarget;
    private Integer touchesOppBox;
    private Integer passesIntoFinalThird;
    private Integer penaltiesWon;
    private Integer bigChancesMissed;
}
