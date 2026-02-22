package com.DraftLeague.dto;

import lombok.Data;
import com.DraftLeague.models.Player.Position;

@Data
public class GameweekRankingDTO {
    private Integer position;
    private Integer teamId;
    private Integer userId;
    private String userDisplayName;
    private Integer gameweekPoints;
    private Integer totalPoints;
}
