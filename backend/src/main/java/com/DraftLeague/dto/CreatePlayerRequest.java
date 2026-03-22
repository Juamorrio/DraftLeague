package com.DraftLeague.dto;

import com.DraftLeague.models.Player.Position;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlayerRequest {

    @NotBlank
    private String id;

    @NotBlank
    @Size(max = 60)
    private String fullName;

    @NotNull
    private Position position;

    @NotNull
    @Min(0)
    private Integer marketValue;

    @NotNull
    private Boolean active;

    @Min(0)
    private Integer totalPoints = 0;

    private String avatarUrl;

    @NotNull
    private Integer clubId;
}
