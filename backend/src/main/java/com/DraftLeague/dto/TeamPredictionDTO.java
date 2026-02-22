package com.DraftLeague.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for team prediction response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPredictionDTO {

    private Integer teamId;
    private String teamName;
    private Double totalPredictedPoints;
    private List<Integer> confidenceInterval; 
    private List<PlayerPrediction> players;
    private Integer nextMatchId;
    private Integer round;
    private Boolean isHomeTeam;
    private String opponent;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerPrediction {
        private String playerId;
        private String playerName;
        private String position;
        private Double predictedPoints;
    }
}
