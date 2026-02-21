package com.DraftLeague.models.Statistics.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ForwardStatisticDTO extends BasePlayerStatisticDTO {

    private Integer successfulDribbles;
    private Integer totalDribbles;
    private Integer penaltiesWon;
    private Integer offsides;
}
