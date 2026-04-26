package com.DraftLeague.models.Statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class GoalkeeperStats {

    @Min(0)
    @Column(name = "saves")
    private Integer saves;

    @Min(0)
    @Column(name = "penalties_saved")
    private Integer penaltiesSaved;

    @Column(name = "clean_sheet")
    private Boolean cleanSheet;

    @Min(0)
    @Column(name = "goals_conceded")
    private Integer goalsConceded;
}
