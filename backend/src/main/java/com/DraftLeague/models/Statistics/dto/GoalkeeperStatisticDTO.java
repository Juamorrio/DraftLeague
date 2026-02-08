package com.DraftLeague.models.Statistics.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO for goalkeeper-specific statistics.
 * Includes saving abilities and distribution from the back.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GoalkeeperStatisticDTO extends BasePlayerStatisticDTO {
    
    private Integer passesIntoFinalThird;
    
    private Integer saves;
    private Integer goalsConceded;
    private Double xgotFaced;
    private Double goalsPrevented;
    
    private Integer sweeperActions;
    private Integer highClaims;
    private Integer divingSaves;
    private Integer savesInsideBox;
    private Integer punches;
    private Integer goalKeeperThrows;
}
