package com.DraftLeague.models.League;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.DraftLeague.models.League.dto.CreateLeagueRequest;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Team.TeamRepository;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.user.UserRepository;

@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public LeagueService(LeagueRepository leagueRepository, UserRepository userRepository, TeamRepository teamRepository) {
        this.leagueRepository = leagueRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    public League createLeague(CreateLeagueRequest req) {
        League league = new League();
        league.setName(req.getName().trim());
        league.setDescription(emptyToNull(req.getDescription()));
        league.setMaxTeams(req.getMaxTeams());
        league.setInitialBudget(req.getInitialBudget());
        league.setCaptainEnable(Boolean.TRUE.equals(req.getCaptainEnable()));
        league.setMarketEndHour(req.getMarketEndHour());
        league.setWildCardsEnable(Boolean.TRUE.equals(req.getWildCardsEnable()));
        league.setCreatedAt(new Date(System.currentTimeMillis() - 1000));
        league.setCode(generateUniqueCode());
        League saved = leagueRepository.save(league);

        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                User user = userRepository.findUserByUsername(username).orElse(null);
                if (user != null) {
                    Team team = new Team();
                    team.setLeague(saved);
                    team.setUser(user);
                    team.setBudget(saved.getInitialBudget());
                    team.setCreatedAt(new Date());
                    team.setGameweekPoints(0);
                    team.setTotalPoints(0);
                    team.setWildcardUsed(false);
                    team.setCaptainId(null);
                    teamRepository.save(team);
                }
            }
        } catch (Exception ignored) {}

        return saved;
    }

    public League getLeagueById(Long id) {
        return leagueRepository.findById(id).orElseThrow(() -> new RuntimeException("League not found"));
    }

    public League updateLeague(Long id, League league) {
        League existingLeague = getLeagueById(id);
        existingLeague.setName(league.getName());
        existingLeague.setDescription(league.getDescription());
        return leagueRepository.save(existingLeague);
    }

    public void deleteLeague(Long id) {
        League league = getLeagueById(id);
        leagueRepository.delete(league);
    }

    private String generateUniqueCode() {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        for (int attempts = 0; attempts < 20; attempts++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
            String code = sb.toString();
            if (leagueRepository.findByCode(code).isEmpty()) return code;
        }
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append('X');
        return sb.toString();
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public List<League> getAllLeagues() {
        return leagueRepository.findAll();
    }

    public List<League> getLeaguesByUserId(int userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Team> teams = teamRepository.findByUser(user);
        List<League> leagues = teams.stream()
                                    .map(Team::getLeague)
                                    .distinct()
                                    .toList();
        return leagues;
    }

    public List<Map<String,Object>> getRanking(Long leagueId) {
        League league = getLeagueById(leagueId);
        List<Team> teams = teamRepository.findByLeagueOrderByTotalPointsDesc(league);
        final int[] pos = {1};
        java.util.List<Map<String,Object>> list = new java.util.ArrayList<>();
        for (Team t : teams) {
            java.util.Map<String,Object> row = new java.util.HashMap<>();
            row.put("teamId", t.getId());
            row.put("userId", t.getUser().getId());
            row.put("userDisplayName", t.getUser().getDisplayName());
            row.put("totalPoints", t.getTotalPoints() == null ? 0 : t.getTotalPoints());
            row.put("position", pos[0]++);
            list.add(row);
        }
        return list;
    }

    public League joinLeagueByCode(String code) {
        if (code == null || code.trim().isEmpty()) throw new RuntimeException("Código inválido");
        League league = leagueRepository.findByCode(code.trim().toUpperCase())
            .orElseThrow(() -> new RuntimeException("Liga no encontrada"));
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new RuntimeException("No autenticado");
        String username = auth.getName();
        User user = userRepository.findUserByUsername(username).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!teamRepository.findByLeagueAndUser(league, user).isEmpty()) {
            throw new RuntimeException("Ya estás en esta liga");
        }
        long count = teamRepository.countByLeague(league);
        if (league.getMaxTeams() != null && count >= league.getMaxTeams()) {
            throw new RuntimeException("La liga está completa");
        }
        Team team = new Team();
        team.setLeague(league);
        team.setUser(user);
        team.setBudget(league.getInitialBudget());
        team.setCreatedAt(new Date());
        team.setGameweekPoints(0);
        team.setTotalPoints(0);
        team.setWildcardUsed(false);
        team.setCaptainId(null);
        teamRepository.save(team);
        return league;
    }
}
