package com.DraftLeague.models.Statistics.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GoalkeeperStatisticDTO extends BasePlayerStatisticDTO {

    private Integer saves;
    private Integer goalsConceded;
}
