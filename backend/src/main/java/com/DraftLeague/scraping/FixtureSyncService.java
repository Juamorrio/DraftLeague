package com.DraftLeague.scraping;

import com.DraftLeague.dto.MatchDTO;
import com.DraftLeague.dto.UpcomingMatchDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java replacement for home.py.
 * Fetches La Liga fixtures from API-Football and builds played/upcoming match maps.
 */
@Service
public class FixtureSyncService {

    private static final Pattern ROUND_PATTERN = Pattern.compile("(\\d+)$");

    private final ApiFootballClient apiClient;

    public FixtureSyncService(ApiFootballClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Fetches all La Liga fixtures and returns played matches grouped by jornada key ("jornada_N").
     * Enriches each match with xG from /fixtures/statistics.
     */
    public Map<String, List<MatchDTO>> fetchPlayedMatches() {
        List<Map<String, Object>> fixtures = apiClient.fetchFixtures();
        Map<String, List<MatchDTO>> result = new HashMap<>();

        for (Map<String, Object> fixture : fixtures) {
            Map<String, Object> fixtureInfo = getMap(fixture, "fixture");
            Map<String, Object> status = getMap(fixtureInfo, "status");
            if (!"FT".equals(status.get("short"))) continue;

            Integer roundNum = parseRound(getMap(fixture, "league"));
            if (roundNum == null) continue;

            Map<String, Object> teams = getMap(fixture, "teams");
            Map<String, Object> goals = getMap(fixture, "goals");
            Map<String, Object> homeTeam = getMap(teams, "home");
            Map<String, Object> awayTeam = getMap(teams, "away");

            MatchDTO dto = new MatchDTO();
            dto.setFixtureId(toInt(fixtureInfo.get("id")));
            dto.setHomeTeamId(toInt(homeTeam.get("id")));
            dto.setAwayTeamId(toInt(awayTeam.get("id")));
            dto.setHomeTeamName((String) homeTeam.get("name"));
            dto.setAwayTeamName((String) awayTeam.get("name"));
            dto.setHomeScore(toInt(goals.get("home")));
            dto.setAwayScore(toInt(goals.get("away")));

            // Enrich with xG
            double[] xg = fetchXg(dto.getFixtureId());
            dto.setHomeXg(xg[0] >= 0 ? xg[0] : null);
            dto.setAwayXg(xg[1] >= 0 ? xg[1] : null);

            result.computeIfAbsent("jornada_" + roundNum, k -> new ArrayList<>()).add(dto);
        }

        return result;
    }

    /**
     * Fetches all La Liga fixtures and returns upcoming matches (NS) grouped by jornada key.
     */
    public Map<String, List<UpcomingMatchDTO>> fetchUpcomingMatches() {
        List<Map<String, Object>> fixtures = apiClient.fetchFixtures();
        Map<String, List<UpcomingMatchDTO>> result = new HashMap<>();

        for (Map<String, Object> fixture : fixtures) {
            Map<String, Object> fixtureInfo = getMap(fixture, "fixture");
            Map<String, Object> status = getMap(fixtureInfo, "status");
            if (!"NS".equals(status.get("short"))) continue;

            Integer roundNum = parseRound(getMap(fixture, "league"));
            if (roundNum == null) continue;

            Map<String, Object> teams = getMap(fixture, "teams");
            Map<String, Object> homeTeam = getMap(teams, "home");
            Map<String, Object> awayTeam = getMap(teams, "away");

            UpcomingMatchDTO dto = new UpcomingMatchDTO();
            dto.setFixtureId(toInt(fixtureInfo.get("id")));
            dto.setHomeTeamId(toInt(homeTeam.get("id")));
            dto.setAwayTeamId(toInt(awayTeam.get("id")));
            dto.setHomeTeamName((String) homeTeam.get("name"));
            dto.setAwayTeamName((String) awayTeam.get("name"));
            dto.setMatchDate((String) fixtureInfo.get("date"));

            result.computeIfAbsent("jornada_" + roundNum, k -> new ArrayList<>()).add(dto);
        }

        return result;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /** Returns [homeXg, awayXg]; negative value means unavailable. */
    private double[] fetchXg(Integer fixtureId) {
        double[] xg = {-1, -1};
        if (fixtureId == null) return xg;
        try {
            List<Map<String, Object>> teams = apiClient.fetchFixtureStatistics(fixtureId);
            for (int i = 0; i < Math.min(teams.size(), 2); i++) {
                List<Map<String, Object>> stats = getList(teams.get(i), "statistics");
                for (Map<String, Object> stat : stats) {
                    if ("expected_goals".equals(stat.get("type"))) {
                        Object val = stat.get("value");
                        if (val != null) {
                            try { xg[i] = Double.parseDouble(val.toString()); } catch (NumberFormatException ignored) {}
                        }
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
        return xg;
    }

    private Integer parseRound(Map<String, Object> league) {
        Object round = league.get("round");
        if (round == null) return null;
        Matcher m = ROUND_PATTERN.matcher(round.toString());
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> parent, String key) {
        Object val = parent == null ? null : parent.get(key);
        return (val instanceof Map) ? (Map<String, Object>) val : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> parent, String key) {
        Object val = parent == null ? null : parent.get(key);
        return (val instanceof List) ? (List<Map<String, Object>>) val : new ArrayList<>();
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
