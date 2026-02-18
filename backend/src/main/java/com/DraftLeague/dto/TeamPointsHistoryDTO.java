package com.DraftLeague.dto;

import lombok.Data;
import java.util.List;

@Data
public class TeamPointsHistoryDTO {
    private Integer teamId;
    private String teamName;
    private Integer currentTotalPoints;
    private List<GameweekPointDTO> history;
}
