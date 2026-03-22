package com.DraftLeague.dto;

import com.DraftLeague.models.Statistics.PlayerStatistic.PlayerType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlayerStatisticRequest {

    @NotNull
    private String playerId;

    @NotNull
    private Integer matchId;

    @NotNull
    private Boolean isHomeTeam;

    @NotNull
    private PlayerType playerType;

    private String role;
    private Double rating;

    @NotNull
    @Min(0)
    private Integer minutesPlayed;

    @Min(0) private Integer goals = 0;
    @Min(0) private Integer assists = 0;
    @Min(0) private Integer totalShots = 0;
    @Min(0) private Integer shotsOnTarget = 0;
    @Min(0) private Integer accuratePasses = 0;
    @Min(0) private Integer totalPasses = 0;
    @Min(0) private Integer chancesCreated = 0;
    @Min(0) private Integer successfulDribbles = 0;
    @Min(0) private Integer totalDribbles = 0;
    @Min(0) private Integer dribbledPast = 0;
    @Min(0) private Integer offsides = 0;
    @Min(0) private Integer accurateCrosses = 0;
    @Min(0) private Integer totalCrosses = 0;
    @Min(0) private Integer tackles = 0;
    @Min(0) private Integer blocks = 0;
    @Min(0) private Integer interceptions = 0;
    @Min(0) private Integer duelsWon = 0;
    @Min(0) private Integer duelsLost = 0;
    @Min(0) private Integer wasFouled = 0;
    @Min(0) private Integer foulsCommitted = 0;
    @Min(0) private Integer yellowCards = 0;
    @Min(0) private Integer redCards = 0;
    @Min(0) private Integer penaltiesWon = 0;
    @Min(0) private Integer penaltyScored = 0;
    @Min(0) private Integer penaltyMissed = 0;
    @Min(0) private Integer penaltyCommitted = 0;
    @Min(0) private Integer saves;
    @Min(0) private Integer penaltiesSaved;
    private Boolean cleanSheet;
    @Min(0) private Integer goalsConceded;
    private Boolean isSubstitute = false;
    private Boolean isCaptain = false;
    @Min(0) private Integer shirtNumber;
    @Min(0) private Integer totalFantasyPoints = 0;
}
