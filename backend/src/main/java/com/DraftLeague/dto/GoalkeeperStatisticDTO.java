package com.DraftLeague.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GoalkeeperStatisticDTO extends BasePlayerStatisticDTO {

    private Integer saves;
    private Integer goalsConceded;
    private Integer penaltiesSaved;
    private Boolean cleanSheet;
}
