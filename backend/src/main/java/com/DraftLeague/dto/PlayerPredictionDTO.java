package com.DraftLeague.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for individual player prediction response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerPredictionDTO {

    private String playerId;
    private String playerName;
    private String position;
    private String playerType;

    // Prediction
    private Double predictedPoints;
    private List<Integer> confidenceInterval; // [min, max]

    // Feature importance (top factors influencing the prediction)
    private Map<String, Double> featuresImportance;

    // Match context
    private Integer nextMatchId;
    private Integer round;
    private Boolean isHomeTeam;
    private String opponent;
}
