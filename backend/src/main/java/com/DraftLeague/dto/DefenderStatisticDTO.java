package com.DraftLeague.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DefenderStatisticDTO extends BasePlayerStatisticDTO {

    private Integer penaltiesWon;
    private Integer successfulDribbles;
    private Integer totalDribbles;
}
