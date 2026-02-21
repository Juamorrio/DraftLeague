package com.DraftLeague.models.Match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("homeClub")
    private String homeTeamName;

    @JsonProperty("awayClub")
    private String awayTeamName;
}
