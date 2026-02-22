package com.DraftLeague.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MidfielderStatisticDTO extends BasePlayerStatisticDTO {

    private Integer successfulDribbles;
    private Integer totalDribbles;
    private Integer penaltiesWon;
}
