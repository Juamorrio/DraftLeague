package com.DraftLeague.models.Team;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;

import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerRepository;
import com.DraftLeague.models.Player.PlayerService;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Player.PlayerTeamRepository;
import com.DraftLeague.models.Team.dto.UpdateTeamPlayersRequest;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.user.UserRepository;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PlayerService playerService;
    private final PlayerTeamRepository playerTeamRepository;

    @Autowired
    public TeamService(TeamRepository teamRepository, PlayerRepository playerRepository, 
                      PlayerTeamRepository playerTeamRepository, UserRepository userRepository, PlayerService playerService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.playerService = playerService;
        this.playerTeamRepository = playerTeamRepository;
    }

    @Transactional
    public Team postTeam(@Valid Team team) {
        this.teamRepository.save(team);
        return team;
    }

    @Transactional
    public Team updateTeam(@Valid Team team, Integer teamId) {
        Team existingTeam = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        existingTeam.setBudget(team.getBudget());
        existingTeam.setGameweekPoints(team.getGameweekPoints());
        existingTeam.setTotalPoints(team.getTotalPoints());
        existingTeam.setCaptainId(team.getCaptainId());
        existingTeam.setPlayerTeams(team.getPlayerTeams());
        return this.teamRepository.save(existingTeam);
    }

    @Transactional
    public void deleteTeam(Integer teamId) {
        Team teamDelete = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        this.teamRepository.delete(teamDelete);
    }

    @Transactional(readOnly = true)
    public Team getTeamById(Integer teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    @Transactional(readOnly = true)
    public Team getTeamByUserId(Integer userId) {
        return teamRepository.findAll().stream()
                .filter(team -> team.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Team not found for user"));
    }

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @Transactional
    public void resetGameweekPoints() {
        List<Team> teams = teamRepository.findAll();
        for (Team team : teams) {
            team.setGameweekPoints(0);
        }
        teamRepository.saveAll(teams);
    }

    @Transactional(readOnly = true)
    public Team getTeamByUserAndLeague(Integer leagueId, Integer userId) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        List<Team> teams = teamRepository.findByUser(user);
        return teams.stream()
            .filter(t -> t.getLeague().getId().equals(leagueId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No tienes un equipo en esta liga"));
    }

    @Transactional
    public Team updateTeamPlayers(Integer leagueId, Integer userId, UpdateTeamPlayersRequest request) {
        Team team = getTeamByUserAndLeague(leagueId, userId);
        
        team.getPlayerTeams().clear();
        
        int totalCost = 0;
        List<PlayerTeam> newPlayerTeams = new ArrayList<>();
        
        for (UpdateTeamPlayersRequest.PlayerSelection selection : request.getPlayers()) {
            Player player = playerService.getPlayerById(Integer.parseInt(selection.getPlayerId()));
            
            totalCost += player.getMarketValue();
            
            PlayerTeam playerTeam = new PlayerTeam();
            playerTeam.setPlayer(player);
            playerTeam.setTeam(team);
            playerTeam.setLined(selection.getLined());
            playerTeam.setIsCaptain(selection.getIsCaptain() != null ? selection.getIsCaptain() : false);
            playerTeam.setSellPrice(player.getMarketValue());
            playerTeam.setBuyPrice(player.getMarketValue());
            
            newPlayerTeams.add(playerTeam);
        }
        
        
        team.getPlayerTeams().addAll(newPlayerTeams);
        
        return teamRepository.save(team);
    }

    @Transactional
    public Team buyoutPlayer(Integer leagueId, Integer sellerUserId, Integer playerId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new RuntimeException("No autenticado");
        String buyerUsername = auth.getName();

        User buyer = userRepository.findUserByUsername(buyerUsername)
            .orElseThrow(() -> new RuntimeException("Usuario comprador no encontrado"));

        Team buyerTeam = getTeamByUserAndLeague(leagueId, buyer.getId());
        Team sellerTeam = getTeamByUserAndLeague(leagueId, sellerUserId);

        PlayerTeam target = sellerTeam.getPlayerTeams().stream()
            .filter(pt -> pt.getPlayer().getId().equals(playerId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("El jugador no pertenece al equipo seleccionado"));

        int price = Optional.ofNullable(target.getBuyPrice())
            .or(() -> Optional.ofNullable(target.getSellPrice()))
            .orElse(target.getPlayer().getMarketValue());

        if (buyerTeam.getBudget() < price) {
            throw new RuntimeException("Presupuesto insuficiente para el clausulazo");
        }

        buyerTeam.setBudget(buyerTeam.getBudget() - price);
        sellerTeam.setBudget(sellerTeam.getBudget() + price);

        target.setTeam(buyerTeam);
        target.setIsCaptain(false);
        target.setLined(false);
        target.setBuyPrice(price);
        playerTeamRepository.save(target);
        teamRepository.save(buyerTeam);
        teamRepository.save(sellerTeam);

        return buyerTeam;
    }
}
