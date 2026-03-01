package com.DraftLeague.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    private Integer fixtureId;
    private Integer homeTeamId;
    private Integer awayTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private Double homeXg;
    private Double awayXg;
    private String homeTeamName;
    private String awayTeamName;
}
