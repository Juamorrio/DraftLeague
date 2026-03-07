package com.DraftLeague.services;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.DraftLeague.dto.CreateLeagueRequest;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.TeamGameweekPointsRepository;
import com.DraftLeague.repositories.TeamPlayerGameweekPointsRepository;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.repositories.PlayerTeamRepository;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.models.League.League;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.MarketPlayerRepository;
import com.DraftLeague.repositories.NotificationLeagueRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

@Service
public class LeagueService {

    private static final Logger logger = LoggerFactory.getLogger(LeagueService.class);

    private final LeagueRepository leagueRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final NotificationLeagueRepository notificationLeagueRepository;
    private final com.DraftLeague.repositories.NotificationRepository notificationRepository;
    private final MarketPlayerRepository marketPlayerRepository;
    private final TeamGameweekPointsRepository teamGameweekPointsRepository;
    private final TeamPlayerGameweekPointsRepository teamPlayerGameweekPointsRepository;

    public LeagueService(LeagueRepository leagueRepository, UserRepository userRepository, TeamRepository teamRepository,
                         PlayerRepository playerRepository, PlayerTeamRepository playerTeamRepository,
                         NotificationLeagueRepository notificationLeagueRepository,
                         com.DraftLeague.repositories.NotificationRepository notificationRepository,
                         MarketPlayerRepository marketPlayerRepository,
                         TeamGameweekPointsRepository teamGameweekPointsRepository,
                         TeamPlayerGameweekPointsRepository teamPlayerGameweekPointsRepository) {
        this.leagueRepository = leagueRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.notificationLeagueRepository = notificationLeagueRepository;
        this.notificationRepository = notificationRepository;
        this.marketPlayerRepository = marketPlayerRepository;
        this.teamGameweekPointsRepository = teamGameweekPointsRepository;
        this.teamPlayerGameweekPointsRepository = teamPlayerGameweekPointsRepository;
    }

    public League createLeague(CreateLeagueRequest req) {
        League league = new League();
        league.setName(req.getName().trim());
        league.setDescription(emptyToNull(req.getDescription()));
        league.setMaxTeams(req.getMaxTeams());
        league.setInitialBudget(req.getInitialBudget());
        league.setCaptainEnable(Boolean.TRUE.equals(req.getCaptainEnable()));
        league.setMarketEndHour(req.getMarketEndHour());
        league.setCreatedAt(new Date(System.currentTimeMillis() - 1000));
        league.setCode(generateUniqueCode());
        
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            User creator = userRepository.findUserByUsername(username).orElse(null);
            if (creator != null) {
                league.setCreatedBy(creator);
            }
        }
        
        League saved = leagueRepository.save(league);

        try {
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
        } catch (Exception e) {
            logger.warn("Could not auto-create team for league creator: {}", e.getMessage());
        }

        return saved;
    }

    public League getLeagueById(Long id) {
        return leagueRepository.findById(id).orElseThrow(() -> new RuntimeException("League not found"));
    }

    public League updateLeague(Long id, League league) {
        League existingLeague = getLeagueById(id);
        
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            User currentUser = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            boolean isCreator = existingLeague.getCreatedBy() != null && 
                               existingLeague.getCreatedBy().getId().equals(currentUser.getId());
            boolean isAdmin = "ADMIN".equals(currentUser.getRole());
            
            if (!isCreator && !isAdmin) {
                throw new RuntimeException("No tienes permisos para editar esta liga");
            }
        } else {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        existingLeague.setName(league.getName());
        existingLeague.setDescription(league.getDescription());
        return leagueRepository.save(existingLeague);
    }

    @Transactional
    public void deleteLeague(Long id) {
        League league = getLeagueById(id);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            User currentUser = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean isCreator = league.getCreatedBy() != null &&
                               league.getCreatedBy().getId().equals(currentUser.getId());
            boolean isAdmin = "ADMIN".equals(currentUser.getRole());

            if (!isCreator && !isAdmin) {
                throw new RuntimeException("No tienes permisos para eliminar esta liga");
            }
        } else {
            throw new RuntimeException("Usuario no autenticado");
        }

        // Delete notifications, then notification_league rows (FK constraint)
        notificationRepository.deleteAllByLeagueId(league.getId());
        notificationLeagueRepository.deleteAllByLeagueId(league.getId());

        // Delete market players (FK to league)
        marketPlayerRepository.deleteAll(marketPlayerRepository.findByLeague(league));

        List<Team> teams = teamRepository.findByLeague(league);
        if (teams != null && !teams.isEmpty()) {
            for (Team t : teams) {
                // Delete gameweek history rows that FK-reference the team
                teamPlayerGameweekPointsRepository.deleteAllByTeam(t);
                teamGameweekPointsRepository.deleteAllByTeam(t);
                // Delete player-team assignments
                playerTeamRepository.deleteAll(playerTeamRepository.findByTeam(t));
            }
            teamRepository.deleteAll(teams);
        }

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

    public List<Map<String,Object>> getLeaguesByUserId(int userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<League> leagues = teamRepository.findDistinctLeaguesByUser(user);
        List<Map<String,Object>> list = new ArrayList<>();
        for (League league : leagues) {
            java.util.Map<String,Object> row = new java.util.HashMap<>();
            row.put("id", league.getId());
            row.put("code", league.getCode());
            row.put("name", league.getName());
            row.put("description", league.getDescription());
            row.put("maxTeams", league.getMaxTeams());
            row.put("initialBudget", league.getInitialBudget());
            row.put("marketEndHour", league.getMarketEndHour());
            row.put("captainEnable", league.getCaptainEnable());
            row.put("createdById", league.getCreatedBy() != null ? league.getCreatedBy().getId() : null);
            row.put("participants", teamRepository.countByLeague(league));

            Team myTeam = teamRepository.findByLeagueAndUser(league, user);
            Integer totalPoints = myTeam != null && myTeam.getTotalPoints() != null ? myTeam.getTotalPoints() : 0;
            row.put("totalPoints", totalPoints);

            int position = 0;
            List<Team> ranking = teamRepository.findByLeagueOrderByTotalPointsDesc(league);
            for (int i = 0; i < ranking.size(); i++) {
                Team t = ranking.get(i);
                if (t.getUser() != null && t.getUser().getId().equals(user.getId())) {
                    position = i + 1;
                    break;
                }
            }
            row.put("position", position == 0 ? null : position);
            list.add(row);
        }
        return list;
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
        if (code == null || code.trim().isEmpty()) throw new RuntimeException("CÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³digo invÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡lido");
        League league = leagueRepository.findByCode(code.trim().toUpperCase())
            .orElseThrow(() -> new RuntimeException("Liga no encontrada"));
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new RuntimeException("No autenticado");
        String username = auth.getName();
        User user = userRepository.findUserByUsername(username).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Team existingTeam = teamRepository.findByLeagueAndUser(league, user);
        if (existingTeam != null) {
            throw new RuntimeException("Ya estÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡s en esta liga");
        }
        long count = teamRepository.countByLeague(league);
        if (league.getMaxTeams() != null && count >= league.getMaxTeams()) {
            throw new RuntimeException("La liga estÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ completa");
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

        try {
            assignInitialSquad(league, team, 11);
        } catch (Exception e) {
            logger.warn("Could not assign initial squad on league join: {}", e.getMessage());
        }
        return league;
    }

    private void assignInitialSquad(League league, Team team, int size) {
        List<Team> teamsInLeague = teamRepository.findByLeagueOrderByTotalPointsDesc(league);
        Set<String> used = new HashSet<>();
        for (Team t : teamsInLeague) {
            for (PlayerTeam pt : playerTeamRepository.findByTeam(t)) {
                if (pt.getPlayer() != null) used.add(pt.getPlayer().getId());
            }
        }

        int needPOR = 1, needDEF = 4, needMID = 3, needDEL = 3, needCOACH = 1;

        List<Player> all = playerRepository.findAll();
        List<Player> lowPOR = new ArrayList<>(), highPOR = new ArrayList<>();
        List<Player> lowDEF = new ArrayList<>(), highDEF = new ArrayList<>();
        List<Player> lowMID = new ArrayList<>(), highMID = new ArrayList<>();
        List<Player> lowDEL = new ArrayList<>(), highDEL = new ArrayList<>();
        List<Player> lowCOA = new ArrayList<>(), highCOA = new ArrayList<>();

        for (Player p : all) {
            if (p == null || p.getMarketValue() == null) continue;
            if (used.contains(p.getId())) continue;
            int val = p.getMarketValue();
            boolean isLow = val <= 10_000_000;
            boolean isHigh = !isLow && val <= 20_000_000;
            if (!isLow && !isHigh) continue; 
            Position pos = p.getPosition();
            if (pos == null) continue;
            switch (pos) {
                case POR -> { if (isLow) lowPOR.add(p); else highPOR.add(p); }
                case DEF -> { if (isLow) lowDEF.add(p); else highDEF.add(p); }
                case MID -> { if (isLow) lowMID.add(p); else highMID.add(p); }
                case DEL -> { if (isLow) lowDEL.add(p); else highDEL.add(p); }
                case COACH -> { if (isLow) lowCOA.add(p); else highCOA.add(p); }
                default -> { /* ignore other positions for initial assignment */ }
            }
        }

        Collections.shuffle(lowPOR); Collections.shuffle(highPOR);
        Collections.shuffle(lowDEF); Collections.shuffle(highDEF);
        Collections.shuffle(lowMID); Collections.shuffle(highMID);
        Collections.shuffle(lowDEL); Collections.shuffle(highDEL);
        Collections.shuffle(lowCOA); Collections.shuffle(highCOA);

        List<Player> chosen = new ArrayList<>();
        pickByQuota(lowPOR, highPOR, needPOR, chosen);
        pickByQuota(lowDEF, highDEF, needDEF, chosen);
        pickByQuota(lowMID, highMID, needMID, chosen);
        pickByQuota(lowDEL, highDEL, needDEL, chosen);
        pickByQuota(lowCOA, highCOA, needCOACH, chosen);

        if (chosen.isEmpty()) return;

        List<PlayerTeam> playerTeams = new ArrayList<>();
        for (Player p : chosen) {
            PlayerTeam pt = new PlayerTeam();
            pt.setPlayer(p);
            pt.setTeam(team);
            pt.setIsCaptain(false);
            pt.setLined(false);
            int price = p.getMarketValue() == null ? 0 : p.getMarketValue();
            pt.setSellPrice(price);
            pt.setBuyPrice(price);
            playerTeams.add(pt);
        }
        playerTeamRepository.saveAll(playerTeams);
    }

    private void pickByQuota(List<Player> low, List<Player> high, int need, List<Player> out) {
        if (need <= 0) return;
        int taken = 0;
        for (Player p : low) {
            if (taken >= need) break;
            out.add(p);
            taken++;
        }
        if (taken < need) {
            for (Player p : high) {
                if (taken >= need) break;
                out.add(p);
                taken++;
            }
        }
    }
}
