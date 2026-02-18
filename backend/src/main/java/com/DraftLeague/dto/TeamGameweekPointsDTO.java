package com.DraftLeague.dto;

import lombok.Data;
import java.util.Date;

@Data
public class TeamGameweekPointsDTO {
    private Integer teamId;
    private String teamName;
    private Integer gameweek;
    private Integer totalPoints;
    private Integer goalkeeperPoints;
    private Integer defenderPoints;
    private Integer midfielderPoints;
    private Integer forwardPoints;
    private Integer captainBonus;
    private String captainId;
    private String captainName;
    private String topScorerId;
    private String topScorerName;
    private Integer topScorerPoints;
    private Date calculatedAt;
}
