package com.DraftLeague.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLeagueRequest {

    @NotBlank
    @Size(max = 60)
    private String name;

    @Size(max = 255)
    private String description;
}
