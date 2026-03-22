package com.DraftLeague.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Player.Position;

/**
 * DTO for individual player prediction response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    // AI enrichment (nullable — omitted from JSON when null)
    private String aiAnalysis;   // narrative text from Claude API
    private String modelSource;  // "XGBOOST" | "HEURISTIC" | "XGBOOST+CLAUDE" | "HEURISTIC+CLAUDE"
}
