package com.DraftLeague.models.Match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    private Integer matchId;
    private Integer homeTeamId;
    private Integer awayTeamId;
    private Integer homeScore;
    private Integer awayScore;
    
    @JsonProperty("homeClub")
    private String homeTeamName;
    
    @JsonProperty("awayClub")
    private String awayTeamName;
}
