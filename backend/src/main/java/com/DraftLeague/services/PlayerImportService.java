package com.DraftLeague.services;

import com.DraftLeague.dto.PlayerImportDto;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	@PersistenceContext
	private EntityManager entityManager;

	@Value("${scripts.path}")
	private String scriptsPath;

	public PlayerImportService(PlayerRepository repo, ObjectMapper objectMapper,
			                       PlayerStatisticService playerStatisticService) {
		this.repo = repo;
		this.objectMapper = objectMapper;
		this.playerStatisticService = playerStatisticService;
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
				case "LB":
				case "RB":
				case "CB":
				case "LWB":
				case "RWB":
					return Position.DEF;
				case "CM":
				case "CDM":
				case "CAM":
				case "LM":
				case "RM":
					return Position.MID;
				case "LW":
				case "RW":
				case "ST":
				case "CF":
				case "SS":
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
			int created = 0;

			for (PlayerImportDto dto : dtos) {
				String playerId = dto.getId();
				if (repo.existsById(playerId)) {
					continue; // jugador ya existe, no modificar
				}

				Player p = new Player();
				p.setId(playerId);
				p.setFullName(dto.getFullName());
				p.setPosition(mapPosition(dto.getPosition()));
				p.setAvatarUrl(dto.getAvatarUrl());
				p.setClubId(dto.getTeamId());
				p.setMarketValue(dto.getMarketValue() != null ? dto.getMarketValue() : 100000);
				p.setActive(Boolean.TRUE);
				p.setTotalPoints(0);
				repo.save(p);
				created++;
			}

			return created;
		}
	}

	public String syncPlayers() throws Exception {
		Path scriptPath = Paths.get(scriptsPath, "players.py").toAbsolutePath();
		
		List<String> command = new ArrayList<>();
		command.add("python");
		command.add(scriptPath.toString());
		
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		
		Process process = processBuilder.start();
		
		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
		}
		
		int exitCode = process.waitFor();
		
		if (exitCode != 0) {
			throw new RuntimeException("Error al ejecutar script de Python. Código de salida: " + exitCode + "\n" + output.toString());
		}
		
		return output.toString();
	}

	/**
	 * Runs players_data.py for the given gameweek, which fetches player statistics
	 * from API-Football, saves jornada_N_stats.json, then imports them into the DB.
	 */
	public String syncGameweekStats(Integer gameweek) throws Exception {
		// 1. Run: python players_data.py <gameweek>  (saves jornada_N_stats.json)
		Path scriptPath = Paths.get(scriptsPath, "players_data.py").toAbsolutePath();

		List<String> command = new ArrayList<>();
		command.add("python");
		command.add(scriptPath.toString());
		command.add(String.valueOf(gameweek));

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(Paths.get(scriptsPath).toFile());
		pb.redirectErrorStream(true);

		Process process = pb.start();

		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
		}

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new RuntimeException("Error al ejecutar script de estad\u00edsticas (c\u00f3digo " + exitCode + "):\n" + output);
		}

		// 2. Read the generated JSON file
		Path statsFile = Paths.get(scriptsPath, "jornada_" + gameweek + "_stats.json");
		if (!Files.exists(statsFile)) {
			throw new RuntimeException("El script no gener\u00f3 el archivo esperado: " + statsFile.getFileName());
		}

		try (InputStream is = Files.newInputStream(statsFile)) {
			List<Map<String, Object>> statsData = objectMapper.readValue(is, new TypeReference<>() {});
			// 3. Import directly into DB via PlayerStatisticService
			playerStatisticService.saveBulkFromJson(statsData);
		}

		return output.toString();
	}

}
