package com.DraftLeague.services;

import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls the Anthropic Claude API to generate a short narrative analysis (in Spanish)
 * for a player's upcoming match prediction.
 *
 * If the API key is not configured or any error occurs, returns Optional.empty()
 * so the caller can continue without narrative enrichment.
 */
@Service
public class ClaudeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAnalysisService.class);
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Value("${anthropic.api.key:}")
    private String apiKey;

    @Value("${claude.analysis.enabled:true}")
    private boolean enabled;

    @Value("${claude.analysis.timeout-seconds:10}")
    private int timeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a short Spanish-language analysis for a player's upcoming match.
     *
     * @param playerName     display name of the player
     * @param position       position string (POR / DEF / MID / DEL)
     * @param opponent       name of the opposing team
     * @param isHome         whether the player's team plays at home
     * @param recentStats    last N matches stats (most recent first)
     * @param predictedPoints numeric prediction (from XGBoost or heuristic)
     * @param modelSource    which model produced the prediction
     * @return Optional containing the narrative text, or empty if unavailable
     */
    public Optional<String> generateAnalysis(
            String playerName,
            String position,
            String opponent,
            boolean isHome,
            List<PlayerStatistic> recentStats,
            double predictedPoints,
            String modelSource
    ) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String prompt = buildPrompt(playerName, position, opponent, isHome, recentStats, predictedPoints, modelSource);

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", MODEL,
                    "max_tokens", 200,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLAUDE_API_URL))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Claude API returned HTTP {} for player {}", response.statusCode(), playerName);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("content").path(0).path("text").asText(null);
            if (text == null || text.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text.trim());

        } catch (Exception e) {
            log.warn("Claude analysis failed for player {}: {}", playerName, e.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(
            String playerName,
            String position,
            String opponent,
            boolean isHome,
            List<PlayerStatistic> stats,
            double predictedPoints,
            String modelSource
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un analista de fantasy football de La Liga. ");
        sb.append("Proporciona un análisis conciso en español para el manager de fantasy.\n\n");
        sb.append("Jugador: ").append(playerName).append(" (").append(position).append(")\n");
        sb.append("Próximo partido: ").append(isHome ? "LOCAL" : "VISITANTE").append(" vs ").append(opponent).append("\n");
        sb.append(String.format("Puntos predichos: %.1f (modelo: %s)\n\n", predictedPoints, modelSource));
        sb.append("Estadísticas recientes (últimos ").append(stats.size()).append(" partidos):\n");

        for (int i = 0; i < stats.size(); i++) {
            PlayerStatistic s = stats.get(i);
            sb.append(String.format(
                    "- Partido %d: nota %.1f, %d min, %dG %dA%s\n",
                    i + 1,
                    s.getRating() != null ? s.getRating() : 6.0,
                    s.getMinutesPlayed() != null ? s.getMinutesPlayed() : 0,
                    s.getGoals() != null ? s.getGoals() : 0,
                    s.getAssists() != null ? s.getAssists() : 0,
                    Boolean.TRUE.equals(s.getCleanSheet()) ? ", portería a cero" : ""
            ));
        }

        sb.append("\nDa: (1) tendencia de rendimiento reciente, ");
        sb.append("(2) recomendación de titular o banquillo, ");
        sb.append("(3) nota de confianza. ");
        sb.append("Máximo 60 palabras. Sé directo y basado en datos.");

        return sb.toString();
    }
}
