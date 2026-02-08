package com.DraftLeague.models.Statistics.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DefenderStatisticDTO extends BasePlayerStatisticDTO {
    
    private Double expectedGoals;
    private Double expectedGoalsOnTarget;
    private Double xgNonPenalty;
    private Integer shotsOnTarget;
    private Integer totalShotsOnTarget;
    private Integer touchesOppBox;
    private Integer penaltiesWon;
    
    private Integer headedClearances;
    private Integer blockedShots;
}
