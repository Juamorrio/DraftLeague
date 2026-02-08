package com.DraftLeague.models.Player;

import com.DraftLeague.models.Player.Dto.PlayerImportDto;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerImportService {

	private final PlayerRepository repo;
	private final ObjectMapper objectMapper;
	@PersistenceContext
	private EntityManager entityManager;

	public PlayerImportService(PlayerRepository repo, ObjectMapper objectMapper) {
		this.repo = repo;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public int importFromJsonResource() throws Exception {
		ClassPathResource resource = new ClassPathResource("scraping/players_data.json");

		try (InputStream is = resource.getInputStream()) {
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
						p.setTeamId(dto.getTeamId());
						p.setMarketValue(dto.getMarketValue() != null ? dto.getMarketValue() : 100000);
						updated++;
					} else {
						p = new Player();
						p.setId(playerId);
						p.setFullName(dto.getFullName());
						p.setPosition(mapPosition(dto.getPosition()));
						p.setAvatarUrl(dto.getAvatarUrl());
						p.setTeamId(dto.getTeamId());
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
			
			System.out.println("Importación completada: " + created + " jugadores creados, " + updated + " jugadores actualizados");
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


	public String syncPlayers() throws Exception {
		ClassPathResource resource = new ClassPathResource("scraping/players.py");
		String scriptPath = resource.getFile().getAbsolutePath();
		
		List<String> command = new ArrayList<>();
		command.add("python");
		command.add(scriptPath);
		
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

}
