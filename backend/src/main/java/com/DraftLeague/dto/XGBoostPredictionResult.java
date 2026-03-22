package com.DraftLeague.dto;

import java.util.Map;

/**
 * Immutable result returned by the XGBoost ML microservice for a single player prediction.
 */
public record XGBoostPredictionResult(
        double predictedPoints,
        Map<String, Double> featuresImportance,
        String modelSource
) {}
