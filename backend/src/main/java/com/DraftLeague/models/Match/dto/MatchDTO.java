package com.DraftLeague.models.Match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    private Integer homeTeamId;
    private Integer awayTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private String homeTeamName;
    private String awayTeamName;
}
