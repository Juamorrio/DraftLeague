package com.DraftLeague.scraping;

import com.DraftLeague.models.Match.Match;
import com.DraftLeague.repositories.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java replacement for players_data.py.
 * Fetches per-match player statistics from API-Football for a given gameweek,
 * and returns them in the same Map format consumed by PlayerStatisticService.saveBulkFromJson().
 */
@Service
public class GameweekStatsSyncService {

    private static final Logger log = LoggerFactory.getLogger(GameweekStatsSyncService.class);

    private static final Map<String, String> POSITION_MAP = Map.of(
            "G", "GOALKEEPER",
            "D", "DEFENDER",
            "M", "MIDFIELDER",
            "F", "FORWARD"
    );

    private final ApiFootballClient apiClient;
    private final MatchRepository matchRepository;

    public GameweekStatsSyncService(ApiFootballClient apiClient, MatchRepository matchRepository) {
        this.apiClient = apiClient;
        this.matchRepository = matchRepository;
    }

    /**
     * Fetches player statistics for all matches in {@code gameweek} and returns
     * a list of stat maps ready for PlayerStatisticService.saveBulkFromJson().
     *
     * @param gameweek La Liga round number (1-based)
     */
    public List<Map<String, Object>> fetchStats(int gameweek) {
        List<Match> matches = matchRepository.findByRound(gameweek);
        if (matches.isEmpty()) {
            throw new RuntimeException("No se encontraron partidos para la jornada " + gameweek
                    + ". Sincroniza los partidos primero (sync-matches).");
        }

        List<Map<String, Object>> allStats = new ArrayList<>();
        int success = 0;

        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            Integer fixtureId = match.getApiFootballFixtureId();
            if (fixtureId == null) continue;

            log.info("GameweekStatsSyncService: [{}/{}] fixture {} ({} vs {})",
                    i + 1, matches.size(), fixtureId, match.getHomeClub(), match.getAwayClub());

            try {
                List<Map<String, Object>> playerStats = processFixture(fixtureId);
                if (!playerStats.isEmpty()) {
                    allStats.addAll(playerStats);
                    success++;
                    log.info("  OK: {} jugadores", playerStats.size());
                } else {
                    log.warn("  WARNING: sin estadísticas disponibles para fixture {}", fixtureId);
                }

                // Respect API-Football rate limit
                if (i < matches.size() - 1) Thread.sleep(500);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("  ERROR fixture {}: {}", fixtureId, e.getMessage(), e);
            }
        }

        log.info("GameweekStatsSyncService: jornada {} — {}/{} partidos OK, {} jugadores",
                gameweek, success, matches.size(), allStats.size());
        return allStats;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<Map<String, Object>> processFixture(int fixtureId) {
        List<Map<String, Object>> response = apiClient.fetchFixturePlayers(fixtureId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (int teamIdx = 0; teamIdx < response.size(); teamIdx++) {
            boolean isHome = (teamIdx == 0);
            List<Map<String, Object>> players = getList(response.get(teamIdx), "players");

            for (Map<String, Object> playerEntry : players) {
                Map<String, Object> stats = parsePlayerStats(playerEntry, fixtureId, isHome);
                if (stats != null && toInt(stats.get("minutesPlayed")) > 0) {
                    result.add(stats);
                }
            }
        }

        return result;
    }

    private Map<String, Object> parsePlayerStats(Map<String, Object> playerEntry, int fixtureId, boolean isHome) {
        Map<String, Object> playerInfo = getMap(playerEntry, "player");
        Object playerId = playerInfo.get("id");
        if (playerId == null) return null;

        List<Map<String, Object>> statsList = getList(playerEntry, "statistics");
        if (statsList.isEmpty()) return null;
        Map<String, Object> s = statsList.get(0);

        Map<String, Object> games    = getMap(s, "games");
        Map<String, Object> shots    = getMap(s, "shots");
        Map<String, Object> goalsData= getMap(s, "goals");
        Map<String, Object> passes   = getMap(s, "passes");
        Map<String, Object> tackles  = getMap(s, "tackles");
        Map<String, Object> duels    = getMap(s, "duels");
        Map<String, Object> dribbles = getMap(s, "dribbles");
        Map<String, Object> fouls    = getMap(s, "fouls");
        Map<String, Object> cards    = getMap(s, "cards");
        Map<String, Object> penalty  = getMap(s, "penalty");

        // Map position code
        String posCode = objToString(games.get("position"), "M");
        // API sometimes returns full word instead of letter
        Map<String, String> fullPosMap = Map.of(
                "Goalkeeper", "G", "Defender", "D", "Midfielder", "M", "Forward", "F");
        posCode = fullPosMap.getOrDefault(posCode, posCode.isEmpty() ? "M" : posCode.substring(0, 1));
        String playerType = POSITION_MAP.getOrDefault(posCode, "MIDFIELDER");

        Integer totalPasses = toInt(passes.get("total"));
        Integer passAccuracy = toInt(passes.get("accuracy"));
        Integer accuratePasses = null;
        if (totalPasses != null && passAccuracy != null && totalPasses > 0) {
            accuratePasses = Math.round(totalPasses * passAccuracy / 100f);
        }

        Integer duelsTotal = toInt(duels.get("total"));
        Integer duelsWon   = toInt(duels.get("won"));
        Integer duelsLost  = (duelsTotal != null && duelsWon != null) ? duelsTotal - duelsWon : null;

        Map<String, Object> stat = new HashMap<>();
        stat.put("playerType",         playerType);
        stat.put("playerId",           playerId.toString());
        stat.put("fixtureId",          fixtureId);
        stat.put("isHomeTeam",         isHome);
        stat.put("rating",             toDouble(games.get("rating")));
        stat.put("minutesPlayed",      toInt(games.get("minutes")) != null ? toInt(games.get("minutes")) : 0);
        stat.put("goals",              toInt(goalsData.get("total")));
        stat.put("assists",            toInt(goalsData.get("assists")));
        stat.put("totalShots",         toInt(shots.get("total")));
        stat.put("shotsOnTarget",      toInt(shots.get("on")));
        stat.put("chancesCreated",     toInt(passes.get("key")));
        stat.put("totalPasses",        totalPasses);
        stat.put("accuratePasses",     accuratePasses);
        stat.put("tackles",            toInt(tackles.get("total")));
        stat.put("blocks",             toInt(tackles.get("blocks")));
        stat.put("interceptions",      toInt(tackles.get("interceptions")));
        stat.put("duelsWon",           duelsWon);
        stat.put("duelsLost",          duelsLost);
        stat.put("successfulDribbles", toInt(dribbles.get("success")));
        stat.put("totalDribbles",      toInt(dribbles.get("attempts")));
        stat.put("dribbledPast",       toInt(dribbles.get("past")));
        stat.put("wasFouled",          toInt(fouls.get("drawn")));
        stat.put("foulsCommitted",     toInt(fouls.get("committed")));
        stat.put("yellowCards",        toInt(cards.get("yellow")));
        stat.put("redCards",           toInt(cards.get("red")));
        stat.put("offsides",           toInt(s.get("offsides")));
        stat.put("penaltyScored",      toInt(penalty.get("scored")));
        stat.put("penaltyMissed",      toInt(penalty.get("missed")));
        stat.put("penaltiesWon",       toInt(penalty.get("won")));
        stat.put("isSubstitute",       games.getOrDefault("substitute", false));
        stat.put("isCaptain",          games.getOrDefault("captain", false));
        stat.put("shirtNumber",        toInt(games.get("number")));
        stat.put("penaltyCommitted",   toInt(penalty.get("commited"))); // API typo: "commited"

        if ("GOALKEEPER".equals(playerType)) {
            stat.put("saves",           toInt(goalsData.get("saves")));
            stat.put("goalsConceded",   toInt(goalsData.get("conceded")));
            stat.put("penaltiesSaved",  toInt(penalty.get("saved")));
        }

        return stat;
    }

    // ── type helpers ─────────────────────────────────────────────────────────

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
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Double) return (Double) val;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String objToString(Object val, String defaultVal) {
        return val != null ? val.toString() : defaultVal;
    }
}
