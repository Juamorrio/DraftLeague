package com.DraftLeague.models.Match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingMatchDTO {
    private Integer homeTeamId;
    private Integer awayTeamId;
    private String matchDate;
    private String homeTeamName;
    private String awayTeamName;
}
