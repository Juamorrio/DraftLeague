package com.DraftLeague.models.Player;

import com.DraftLeague.models.Player.Dto.PlayerImportDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
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
		ClassPathResource resource = new ClassPathResource("scrapping/players_data.json");

		try (InputStream is = resource.getInputStream()) {
			List<PlayerImportDto> dtos = objectMapper.readValue(is, new TypeReference<>() {});

			for (PlayerImportDto dto : dtos) {
				Player p = new Player();

				p.setId(Integer.parseInt(dto.getId()));
				p.setFullName(dto.getFullName());
				p.setPosition(mapPosition(dto.getPosition()));
				p.setAvatarUrl(dto.getAvatarUrl());
				p.setTeamId(dto.getTeamId());
				p.setMarketValue(parseMarketValue(dto.getMarketValue()));
				p.setActive(Boolean.TRUE);
				p.setTotalPoints(0);

				PlayerStatistic defaultStat = getDefaultStatistic();
				p.setPlayerStatistic(defaultStat);

				repo.save(p);
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
				case "LW":
				case "RW":
					return Position.MID;
				case "ST":
				case "CF":
				case "SS":
					return Position.DEL;
				default:
					return Position.MID; 
			}
		}

		private PlayerStatistic getDefaultStatistic() {
			List<PlayerStatistic> stats = entityManager
				.createQuery("select ps from PlayerStatistic ps order by ps.id asc", PlayerStatistic.class)
				.setMaxResults(1)
				.getResultList();
			if (!stats.isEmpty()) {
				return stats.get(0);
			}
			PlayerStatistic ps = new PlayerStatistic();
			ps.setMinutesPlayed(0);
			ps.setGoals(0);
			ps.setAssists(0);
			ps.setShots(0);
			ps.setShotsOnTarget(0);
			ps.setFoulsDrawn(0);
			ps.setFoulsCommitted(0);
			ps.setOwnGoals(0);
			ps.setYellowCards(0);
			ps.setRedCards(0);
			ps.setRatingAvg(0.0);
			ps.setTotalFantasyPoints(0);
			entityManager.persist(ps);
			return ps;
		}

		private Integer parseMarketValue(String value){
			if (value == null){
				return 0;
			} else{
				value = value.trim().toUpperCase().replace("€", "").replace(",", "").replace(" ", "");
				try {
					if (value.endsWith("M")){
						return (int)(Float.parseFloat(value.substring(0, value.length() - 1)) * 1_000_000);
					} else if (value.endsWith("K")){
						return (int)(Float.parseFloat(value.substring(0, value.length() - 1)) * 1_000);
					} else {
						return Integer.parseInt(value);
					}
				} catch (Exception e){
					return 0;
				}
			}
		}

}
