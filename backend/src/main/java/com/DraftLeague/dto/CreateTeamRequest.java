package com.DraftLeague.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeamRequest {

    @NotNull
    @Min(0)
    private Integer budget;

    @NotNull
    private Boolean wildcardUsed = false;

    @NotNull
    private Integer userId;

    @NotNull
    private Long leagueId;
}
