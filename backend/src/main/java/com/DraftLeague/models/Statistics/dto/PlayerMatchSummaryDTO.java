package com.DraftLeague.models.Statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMatchSummaryDTO {
    private Integer matchId;
    private Integer round;
    private String opponent;
    private String homeTeam;
    private String awayTeam;
    private Boolean isHomeTeam;
    private Integer goalsScored;
    private Integer goalsConceded;
    private Integer minutesPlayed;
    private Double fotmobRating;
    private Integer fantasyPoints;
}
