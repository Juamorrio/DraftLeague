package com.DraftLeague.models.Team;

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public TeamService(TeamRepository teamRepository, PlayerRepository playerRepository, 
                      PlayerTeamRepository playerTeamRepository, UserRepository userRepository, PlayerService playerService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.playerService = playerService;
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
            
            newPlayerTeams.add(playerTeam);
        }
        
        
        team.getPlayerTeams().addAll(newPlayerTeams);
        
        return teamRepository.save(team);
    }
}
