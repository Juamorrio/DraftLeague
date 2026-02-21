package com.DraftLeague.models.Match;

import com.DraftLeague.models.Match.dto.MatchDTO;
import com.DraftLeague.models.Match.dto.UpcomingMatchDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchService {

    private final ObjectMapper objectMapper;
    private final MatchRepository matchRepository;

    public MatchService(ObjectMapper objectMapper, MatchRepository matchRepository) {
        this.objectMapper = objectMapper;
        this.matchRepository = matchRepository;
    }

    public Map<String, List<MatchDTO>> getPlayedMatches() {
        try {
            ClassPathResource resource = new ClassPathResource("scraping/matches.json");
            InputStream inputStream = resource.getInputStream();
            TypeReference<Map<String, List<MatchDTO>>> typeRef = new TypeReference<>() {};
            return objectMapper.readValue(inputStream, typeRef);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public Map<String, List<UpcomingMatchDTO>> getUpcomingMatches() {
        try {
            ClassPathResource resource = new ClassPathResource("scraping/upcoming_matches.json");
            InputStream inputStream = resource.getInputStream();
            TypeReference<Map<String, List<UpcomingMatchDTO>>> typeRef = new TypeReference<>() {};
            return objectMapper.readValue(inputStream, typeRef);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public String syncMatches() throws Exception {
        ClassPathResource resource = new ClassPathResource("scraping/home.py");
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

    public String importMatchesFromJson() {
        try {
            int importedCount = 0;
            
            // Import played matches
            Map<String, List<MatchDTO>> matchesByRound = getPlayedMatches();
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
                    
                    matchRepository.save(match);
                    importedCount++;
                }
            }
            
            // Import upcoming matches
            Map<String, List<UpcomingMatchDTO>> upcomingByRound = getUpcomingMatches();
            for (Map.Entry<String, List<UpcomingMatchDTO>> entry : upcomingByRound.entrySet()) {
                String roundKey = entry.getKey();
                Integer roundNumber = Integer.parseInt(roundKey.replace("jornada_", ""));
                
                for (UpcomingMatchDTO upcomingDTO : entry.getValue()) {
                    // Check by teamIds and round since upcoming matches don't have matchId
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
                    
                    matchRepository.save(match);
                    importedCount++;
                }
            }
            
            return "Imported " + importedCount + " matches successfully";
        } catch (Exception e) {
            throw new RuntimeException("Error importing matches: " + e.getMessage(), e);
        }
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public Match getMatchByFixtureId(Integer fixtureId) {
        return matchRepository.findByApiFootballFixtureId(fixtureId).orElse(null);
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

    /**
     * Get the next upcoming round number (the lowest round with UPCOMING matches).
     * @return the next round number, or null if no upcoming matches exist
     */
    public Integer getNextRound() {
        List<Match> upcoming = matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING);
        if (upcoming.isEmpty()) return null;
        return upcoming.get(0).getRound();
    }

    /**
     * Get all matches in the next upcoming round.
     * @return list of matches in the next round, or empty list
     */
    public List<Match> getNextRoundMatches() {
        Integer nextRound = getNextRound();
        if (nextRound == null) return new ArrayList<>();
        return matchRepository.findByRound(nextRound);
    }

    /**
     * Get the next upcoming match for a specific club (by API-Football team ID).
     * Searches for the earliest upcoming match where the club plays as home or away.
     * @param clubTeamId the API-Football team ID
     * @return the next match, or null if not found
     */
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
