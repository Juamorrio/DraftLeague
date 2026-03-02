package com.DraftLeague.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.dto.CreateLeagueRequest;

@Getter
@Setter
public class CreateLeagueRequest {

    @NotBlank
    @Size(max = 60)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    @Min(2)
    @Max(50)
    private Integer maxTeams;

    @NotNull
    @Min(0)
    private Integer initialBudget;

    @NotBlank
    private String marketEndHour;

    @NotNull
    private Boolean captainEnable;

}
