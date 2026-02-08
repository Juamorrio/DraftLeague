package com.DraftLeague.models.Match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingMatchDTO {
    private Integer matchId;
    private Integer homeTeamId;
    private Integer awayTeamId;
    private String matchDate;
    
    @JsonProperty("homeClub")
    private String homeTeamName;
    
    @JsonProperty("awayClub")
    private String awayTeamName;
}
