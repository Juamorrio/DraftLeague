package com.DraftLeague.services;

import com.DraftLeague.dto.MatchDTO;
import com.DraftLeague.dto.UpcomingMatchDTO;
import com.DraftLeague.scraping.FixtureSyncService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.MatchRepository;

@Service
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    private final ObjectMapper objectMapper;
    private final MatchRepository matchRepository;
    private final FixtureSyncService fixtureSyncService;

    @Value("${scripts.path}")
    private String scriptsPath;

    public MatchService(ObjectMapper objectMapper, MatchRepository matchRepository,
                        FixtureSyncService fixtureSyncService) {
        this.objectMapper = objectMapper;
        this.matchRepository = matchRepository;
        this.fixtureSyncService = fixtureSyncService;
    }

    public Map<String, List<MatchDTO>> getPlayedMatches() {
        try {
            Path path = Paths.get(scriptsPath, "matches.json");
            try (InputStream inputStream = Files.newInputStream(path)) {
                TypeReference<Map<String, List<MatchDTO>>> typeRef = new TypeReference<>() {};
                return objectMapper.readValue(inputStream, typeRef);
            }
        } catch (IOException e) {
            logger.error("Error leyendo matches.json: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    public Map<String, List<UpcomingMatchDTO>> getUpcomingMatches() {
        try {
            Path path = Paths.get(scriptsPath, "upcoming_matches.json");
            try (InputStream inputStream = Files.newInputStream(path)) {
                TypeReference<Map<String, List<UpcomingMatchDTO>>> typeRef = new TypeReference<>() {};
                return objectMapper.readValue(inputStream, typeRef);
            }
        } catch (IOException e) {
            logger.error("Error leyendo upcoming_matches.json: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    public String syncMatches() throws Exception {
        Map<String, List<MatchDTO>> played = fixtureSyncService.fetchPlayedMatches();
        Map<String, List<UpcomingMatchDTO>> upcoming = fixtureSyncService.fetchUpcomingMatches();
        return importMatchesFromData(played, upcoming);
    }

    @Transactional
    public String importMatchesFromData(Map<String, List<MatchDTO>> matchesByRound,
                                        Map<String, List<UpcomingMatchDTO>> upcomingByRound) {
        try {
            List<Match> toSave = new ArrayList<>();

            for (Map.Entry<String, List<MatchDTO>> entry : matchesByRound.entrySet()) {
                String roundKey = entry.getKey();
                Integer roundNumber = Integer.parseInt(roundKey.replace("jornada_", ""));

                for (MatchDTO matchDTO : entry.getValue()) {
                    if (matchDTO.getFixtureId() != null &&
                        matchRepository.findByApiFootballFixtureId(matchDTO.getFixtureId()).isPresent()) {
                        continue;
                    }

                    Match match = new Match();
                    match.setApiFootballFixtureId(matchDTO.getFixtureId());
                    match.setRound(roundNumber);
                    match.setHomeTeamId(matchDTO.getHomeTeamId());
                    match.setAwayTeamId(matchDTO.getAwayTeamId());
                    match.setHomeClub(matchDTO.getHomeTeamName() != null ? matchDTO.getHomeTeamName() : "");
                    match.setAwayClub(matchDTO.getAwayTeamName() != null ? matchDTO.getAwayTeamName() : "");
                    match.setHomeGoals(matchDTO.getHomeScore() != null ? matchDTO.getHomeScore() : 0);
                    match.setAwayGoals(matchDTO.getAwayScore() != null ? matchDTO.getAwayScore() : 0);
                    match.setHomeXg(matchDTO.getHomeXg());
                    match.setAwayXg(matchDTO.getAwayXg());
                    match.setStatus(MatchStatus.FINISHED);
                    toSave.add(match);
                }
            }

            for (Map.Entry<String, List<UpcomingMatchDTO>> entry : upcomingByRound.entrySet()) {
                String roundKey = entry.getKey();
                Integer roundNumber = Integer.parseInt(roundKey.replace("jornada_", ""));

                for (UpcomingMatchDTO upcomingDTO : entry.getValue()) {
                    if (matchRepository.findByRoundAndHomeTeamIdAndAwayTeamId(
                            roundNumber, upcomingDTO.getHomeTeamId(), upcomingDTO.getAwayTeamId()).isPresent()) {
                        continue;
                    }

                    Match match = new Match();
                    match.setRound(roundNumber);
                    match.setHomeTeamId(upcomingDTO.getHomeTeamId());
                    match.setAwayTeamId(upcomingDTO.getAwayTeamId());
                    match.setHomeClub(upcomingDTO.getHomeTeamName() != null ? upcomingDTO.getHomeTeamName() : "");
                    match.setAwayClub(upcomingDTO.getAwayTeamName() != null ? upcomingDTO.getAwayTeamName() : "");
                    match.setMatchDate(upcomingDTO.getMatchDate());
                    match.setStatus(MatchStatus.UPCOMING);
                    match.setHomeGoals(0);
                    match.setAwayGoals(0);
                    toSave.add(match);
                }
            }

            matchRepository.saveAll(toSave);
            return "Imported " + toSave.size() + " matches successfully";
        } catch (Exception e) {
            throw new RuntimeException("Error importing matches: " + e.getMessage(), e);
        }
    }

    public String importMatchesFromJson() {
        try {
            Map<String, List<MatchDTO>> played = getPlayedMatches();
            Map<String, List<UpcomingMatchDTO>> upcoming = getUpcomingMatches();
            return importMatchesFromData(played, upcoming);
        } catch (Exception e) {
            throw new RuntimeException("Error importing matches from JSON: " + e.getMessage(), e);
        }
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public Match getMatchByFixtureId(Integer fixtureId) {
        return matchRepository.findByApiFootballFixtureId(fixtureId).orElse(null);
    }

    public Match getMatchById(Integer id) {
        return matchRepository.findById(id).orElse(null);
    }

    public Map<String, List<MatchDTO>> getPlayedMatchesFromDB() {
        List<Match> matches = matchRepository.findByStatus(MatchStatus.FINISHED);
        Map<String, List<MatchDTO>> result = new HashMap<>();
        
        for (Match match : matches) {
            String roundKey = "jornada_" + match.getRound();
            result.computeIfAbsent(roundKey, k -> new ArrayList<>());
            
            MatchDTO dto = new MatchDTO();
            dto.setFixtureId(match.getApiFootballFixtureId());
            dto.setHomeTeamId(match.getHomeTeamId());
            dto.setAwayTeamId(match.getAwayTeamId());
            dto.setHomeScore(match.getHomeGoals());
            dto.setAwayScore(match.getAwayGoals());
            dto.setHomeXg(match.getHomeXg());
            dto.setAwayXg(match.getAwayXg());
            dto.setHomeTeamName(match.getHomeClub());
            dto.setAwayTeamName(match.getAwayClub());

            result.get(roundKey).add(dto);
        }

        return result;
    }

    public Map<String, List<UpcomingMatchDTO>> getUpcomingMatchesFromDB() {
        List<Match> matches = matchRepository.findByStatus(MatchStatus.UPCOMING);
        Map<String, List<UpcomingMatchDTO>> result = new HashMap<>();

        for (Match match : matches) {
            String roundKey = "jornada_" + match.getRound();
            result.computeIfAbsent(roundKey, k -> new ArrayList<>());

            UpcomingMatchDTO dto = new UpcomingMatchDTO();
            dto.setFixtureId(match.getApiFootballFixtureId());
            dto.setHomeTeamId(match.getHomeTeamId());
            dto.setAwayTeamId(match.getAwayTeamId());
            dto.setMatchDate(match.getMatchDate());
            dto.setHomeTeamName(match.getHomeClub());
            dto.setAwayTeamName(match.getAwayClub());
            
            result.get(roundKey).add(dto);
        }
        
        return result;
    }

    public List<String> getNameTeamMatch(Integer id){
        Match match = matchRepository.findById(id).orElseThrow(() -> new RuntimeException("Partido no encontrado"));
        List<String> teamNames = new ArrayList<>();
        teamNames.add(match.getHomeClub());
        teamNames.add(match.getAwayClub());
        return teamNames;
    }

    public Integer getNextRound() {
        List<Match> upcoming = matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING);
        if (upcoming.isEmpty()) return null;
        return upcoming.get(0).getRound();
    }


    public List<Match> getNextRoundMatches() {
        Integer nextRound = getNextRound();
        if (nextRound == null) return new ArrayList<>();
        return matchRepository.findByRound(nextRound);
    }

    public Match getNextMatchForClub(Integer clubTeamId) {
        if (clubTeamId == null) return null;

        var homeMatch = matchRepository.findFirstByStatusAndHomeTeamIdOrderByRoundAsc(
                MatchStatus.UPCOMING, clubTeamId);
        var awayMatch = matchRepository.findFirstByStatusAndAwayTeamIdOrderByRoundAsc(
                MatchStatus.UPCOMING, clubTeamId);

        if (homeMatch.isPresent() && awayMatch.isPresent()) {
            return homeMatch.get().getRound() <= awayMatch.get().getRound()
                    ? homeMatch.get() : awayMatch.get();
        }
        return homeMatch.orElseGet(() -> awayMatch.orElse(null));
    }
}
