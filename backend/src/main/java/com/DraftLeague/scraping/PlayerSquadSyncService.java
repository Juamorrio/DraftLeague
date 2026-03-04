package com.DraftLeague.scraping;

import com.DraftLeague.dto.PlayerImportDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java replacement for players.py.
 * Fetches La Liga squad data from API-Football /players/squads endpoint.
 */
@Service
public class PlayerSquadSyncService {

    private static final Map<String, String> POSITION_MAP = Map.of(
            "Goalkeeper", "GK",
            "Defender",   "CB",
            "Midfielder", "CM",
            "Attacker",   "ST"
    );

    private final ApiFootballClient apiClient;

    public PlayerSquadSyncService(ApiFootballClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Fetches all squads for every La Liga team and returns them as a flat list of PlayerImportDto.
     * Equivalent to running players.py and reading the resulting players_data.json.
     */
    public List<PlayerImportDto> fetchAllSquads() {
        List<PlayerImportDto> all = new ArrayList<>();

        for (Integer teamId : ApiFootballClient.TEAM_IDS) {
            try {
                List<Map<String, Object>> response = apiClient.fetchSquad(teamId);
                if (response.isEmpty()) {
                    System.out.println("PlayerSquadSyncService: no data for team " + teamId);
                    continue;
                }

                List<Map<String, Object>> squad = getList(response.get(0), "players");
                System.out.println("PlayerSquadSyncService: team " + teamId + " → " + squad.size() + " players");

                for (Map<String, Object> playerData : squad) {
                    Object id = playerData.get("id");
                    if (id == null) continue;

                    String apiPosition = (String) playerData.getOrDefault("position", "Midfielder");
                    String position = POSITION_MAP.getOrDefault(apiPosition, "CM");

                    PlayerImportDto dto = new PlayerImportDto();
                    dto.setId(id.toString());
                    dto.setFullName((String) playerData.getOrDefault("name", "Desconocido"));
                    dto.setPosition(position);
                    dto.setAvatarUrl((String) playerData.getOrDefault("photo", ""));
                    dto.setTeamId(teamId);
                    dto.setMarketValue(null);  // not available on free tier
                    all.add(dto);
                }

                // Respect API-Football free-tier rate limit (500ms between calls)
                Thread.sleep(500);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("PlayerSquadSyncService: error for team " + teamId + ": " + e.getMessage());
            }
        }

        System.out.println("PlayerSquadSyncService: total players fetched: " + all.size());
        return all;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> parent, String key) {
        Object val = parent == null ? null : parent.get(key);
        return (val instanceof List) ? (List<Map<String, Object>>) val : new ArrayList<>();
    }
}
