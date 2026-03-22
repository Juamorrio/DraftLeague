package com.DraftLeague.services;

import com.DraftLeague.dto.XGBoostPredictionResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for the XGBoost ML microservice.
 * Uses Java 11+ HttpClient — consistent with PlayerService.fetchPlayerTeamImage().
 * All methods return Optional.empty() on any error, enabling automatic fallback to the heuristic.
 */
@Service
public class XGBoostClient {

    private static final Logger log = LoggerFactory.getLogger(XGBoostClient.class);

    @Value("${ml.xgboost.url:http://localhost:8000}")
    private String baseUrl;

    @Value("${ml.xgboost.internal-key:}")
    private String internalKey;

    @Value("${ml.xgboost.timeout-seconds:5}")
    private int timeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends a prediction request for a single player to the XGBoost service.
     *
     * @param playerId  player identifier (used as path variable)
     * @param features  map of 16 feature names → values
     * @return prediction result, or empty if the service is unavailable or returns an error
     */
    public Optional<XGBoostPredictionResult> predict(String playerId, Map<String, Object> features) {
        try {
            String json = objectMapper.writeValueAsString(features);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/predict/player/" + playerId))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("XGBoost service returned HTTP {} for player {}", response.statusCode(), playerId);
                return Optional.empty();
            }

            Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
            double predictedPoints = ((Number) body.get("predictedPoints")).doubleValue();
            @SuppressWarnings("unchecked")
            Map<String, Double> importance = (Map<String, Double>) body.get("featuresImportance");
            String modelSource = (String) body.getOrDefault("modelSource", "XGBOOST");

            return Optional.of(new XGBoostPredictionResult(predictedPoints, importance, modelSource));

        } catch (Exception e) {
            log.warn("XGBoost predict failed for player {}: {}", playerId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Triggers asynchronous model retraining. Fire-and-forget — does not block the caller.
     *
     * @param gameweek the gameweek number that just completed (for logging)
     */
    public void triggerRetraining(int gameweek) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("gameweek", gameweek));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/train"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Key", internalKey)
                    .timeout(Duration.ofSeconds(90))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() == 200) {
                            log.info("XGBoost retraining completed for gameweek {}: {}", gameweek, resp.body());
                        } else {
                            log.warn("XGBoost retraining returned HTTP {} for gameweek {}", resp.statusCode(), gameweek);
                        }
                    })
                    .exceptionally(ex -> {
                        log.warn("XGBoost retraining request failed for gameweek {}: {}", gameweek, ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.warn("Could not trigger XGBoost retraining for gameweek {}: {}", gameweek, e.getMessage());
        }
    }

    /**
     * Checks whether the ML service is reachable and has a model loaded.
     */
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
