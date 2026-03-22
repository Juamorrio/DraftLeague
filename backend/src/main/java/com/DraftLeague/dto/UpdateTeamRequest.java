package com.DraftLeague.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeamRequest {

    @Min(0)
    private Integer budget;

    @Min(0)
    private Integer gameweekPoints;

    @Min(0)
    private Integer totalPoints;

    @Min(0)
    private Integer captainId;
}
