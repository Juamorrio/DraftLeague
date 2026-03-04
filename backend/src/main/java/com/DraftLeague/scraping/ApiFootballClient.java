package com.DraftLeague.scraping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for the API-Football v3 REST API.
 * Replaces the Python ApiPublic class and all requests.get() calls.
 */
@Component
public class ApiFootballClient {

    private static final String BASE_URL = "https://v3.football.api-sports.io";
    private static final int LEAGUE_ID = 140;   // La Liga
    private static final int SEASON    = 2025;

    /** All 20 La Liga team IDs (mirrors public.py TEAM_IDS) */
    public static final List<Integer> TEAM_IDS = List.of(
        541, 529, 530, 548, 543, 531, 533, 727, 547, 798,
        538, 728, 546, 536, 532, 540, 542, 718, 539, 797
    );

    private final WebClient webClient;

    public ApiFootballClient(@Value("${api.football.key}") String apiKey) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) 
                .build();
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("x-apisports-key", apiKey)
                .exchangeStrategies(strategies)
                .build();
    }

    /** GET /fixtures?league=140&season=2025 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchFixtures() {
        Map<String, Object> body = webClient.get()
                .uri(u -> u.path("/fixtures")
                        .queryParam("league", LEAGUE_ID)
                        .queryParam("season", SEASON)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return extractResponse(body);
    }

    /** GET /fixtures/statistics?fixture={fixtureId} */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchFixtureStatistics(int fixtureId) {
        Map<String, Object> body = webClient.get()
                .uri(u -> u.path("/fixtures/statistics")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return extractResponse(body);
    }

    /** GET /players/squads?team={teamId} */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchSquad(int teamId) {
        Map<String, Object> body = webClient.get()
                .uri(u -> u.path("/players/squads")
                        .queryParam("team", teamId)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return extractResponse(body);
    }

    /** GET /fixtures/players?fixture={fixtureId} */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchFixturePlayers(int fixtureId) {
        Map<String, Object> body = webClient.get()
                .uri(u -> u.path("/fixtures/players")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return extractResponse(body);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractResponse(Map<String, Object> body) {
        if (body == null) return List.of();
        Object response = body.get("response");
        if (response instanceof List) return (List<Map<String, Object>>) response;
        return List.of();
    }
}
