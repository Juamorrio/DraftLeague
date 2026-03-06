package com.DraftLeague.services;

import com.DraftLeague.dto.PlayerImportDto;
import com.DraftLeague.scraping.GameweekStatsSyncService;
import com.DraftLeague.scraping.PlayerSquadSyncService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.repositories.PlayerRepository;

@Service
public class PlayerImportService {

    private final PlayerRepository repo;
    private final ObjectMapper objectMapper;
    private final PlayerStatisticService playerStatisticService;
    private final PlayerSquadSyncService playerSquadSyncService;
    private final GameweekStatsSyncService gameweekStatsSyncService;
    @PersistenceContext
    private EntityManager entityManager;

    @Value("${scripts.path}")
    private String scriptsPath;

    public PlayerImportService(PlayerRepository repo, ObjectMapper objectMapper,
                               PlayerStatisticService playerStatisticService,
                               PlayerSquadSyncService playerSquadSyncService,
                               GameweekStatsSyncService gameweekStatsSyncService) {
        this.repo = repo;
        this.objectMapper = objectMapper;
        this.playerStatisticService = playerStatisticService;
        this.playerSquadSyncService = playerSquadSyncService;
        this.gameweekStatsSyncService = gameweekStatsSyncService;
    }

    @Transactional
    public int importFromJsonResource() throws Exception {
        Path path = Paths.get(scriptsPath, "players_data.json");

        try (InputStream is = Files.newInputStream(path)) {
            List<PlayerImportDto> dtos = objectMapper.readValue(is, new TypeReference<>() {});
            int updated = 0;
            int created = 0;

            for (PlayerImportDto dto : dtos) {
                try {
                    String playerId = dto.getId();
                    Player p = repo.findById(playerId).orElse(null);

                    if (p != null) {
                        p.setFullName(dto.getFullName());
                        p.setPosition(mapPosition(dto.getPosition()));
                        p.setAvatarUrl(dto.getAvatarUrl());
                        p.setClubId(dto.getTeamId());
                        p.setMarketValue(dto.getMarketValue() != null ? dto.getMarketValue() : 100000);
                        updated++;
                    } else {
                        p = new Player();
                        p.setId(playerId);
                        p.setFullName(dto.getFullName());
                        p.setPosition(mapPosition(dto.getPosition()));
                        p.setAvatarUrl(dto.getAvatarUrl());
                        p.setClubId(dto.getTeamId());
                        p.setMarketValue(dto.getMarketValue() != null ? dto.getMarketValue() : 100000);
                        p.setActive(Boolean.TRUE);
                        p.setTotalPoints(0);
                        created++;
                    }

                    repo.save(p);
                } catch (Exception e) {
                    System.err.println("Error al importar jugador: " + dto.getId() + " - " + dto.getFullName());
                    System.err.println("Detalles: " + e.getMessage());
                    throw e;
                }
            }

            return dtos.size();
        }
    }

    private Position mapPosition(String code) {
        if (code == null) return Position.COACH;
        code = code.trim().toUpperCase();
        switch (code) {
            case "GK":
                return Position.POR;
            case "LB": case "RB": case "CB": case "LWB": case "RWB":
                return Position.DEF;
            case "CM": case "CDM": case "CAM": case "LM": case "RM":
                return Position.MID;
            case "LW": case "RW": case "ST": case "CF": case "SS":
                return Position.DEL;
            default:
                return Position.MID;
        }
    }

    @Transactional
    public int importNewPlayersOnly() throws Exception {
        Path path = Paths.get(scriptsPath, "players_data.json");

        try (InputStream is = Files.newInputStream(path)) {
            List<PlayerImportDto> dtos = objectMapper.readValue(is, new TypeReference<>() {});

            List<Player> newPlayers = new ArrayList<>();
            for (PlayerImportDto dto : dtos) {
                if (repo.existsById(dto.getId())) continue;

                Player p = new Player();
                p.setId(dto.getId());
                p.setFullName(dto.getFullName());
                p.setPosition(mapPosition(dto.getPosition()));
                p.setAvatarUrl(dto.getAvatarUrl());
                p.setClubId(dto.getTeamId());
                p.setMarketValue(dto.getMarketValue() != null ? dto.getMarketValue() : 100000);
                p.setActive(Boolean.TRUE);
                p.setTotalPoints(0);
                newPlayers.add(p);
            }

            repo.saveAll(newPlayers);
            return newPlayers.size();
        }
    }

    /** Fetches all La Liga squads from API-Football and upserts players into the DB. */
    @Transactional
    public String syncPlayers() throws Exception {
        List<PlayerImportDto> dtos = playerSquadSyncService.fetchAllSquads();
        int created = 0, updated = 0;

        List<Player> toSave = new ArrayList<>(dtos.size());
        for (PlayerImportDto dto : dtos) {
            Player p = repo.findById(dto.getId()).orElse(null);
            if (p != null) {
                p.setFullName(dto.getFullName());
                p.setPosition(mapPosition(dto.getPosition()));
                p.setAvatarUrl(dto.getAvatarUrl());
                p.setClubId(dto.getTeamId());
                p.setMarketValue(dto.getMarketValue() != null ? dto.getMarketValue() : 100000);
                updated++;
            } else {
                p = new Player();
                p.setId(dto.getId());
                p.setFullName(dto.getFullName());
                p.setPosition(mapPosition(dto.getPosition()));
                p.setAvatarUrl(dto.getAvatarUrl());
                p.setClubId(dto.getTeamId());
                p.setMarketValue(dto.getMarketValue() != null ? dto.getMarketValue() : 100000);
                p.setActive(Boolean.TRUE);
                p.setTotalPoints(0);
                created++;
            }
            toSave.add(p);
        }
        repo.saveAll(toSave);
        return "Sync completado: " + created + " creados, " + updated + " actualizados";
    }

    /** Fetches player statistics for the given gameweek from API-Football and imports them into the DB. */
    public String syncGameweekStats(Integer gameweek) throws Exception {
        List<Map<String, Object>> statsData = gameweekStatsSyncService.fetchStats(gameweek);
        playerStatisticService.saveBulkFromJson(statsData);
        return "Jornada " + gameweek + ": " + statsData.size() + " estadisticas importadas";
    }
}