package com.DraftLeague.models.Player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Team.TeamRepository;

import jakarta.transaction.Transactional;

@Service
public class playerTeamService {
    @Autowired
    private PlayerTeamRepository playerTeamRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private PlayerRepository playerRepository;
    
    @Transactional
    public PlayerTeam buyPlayer(String playerId, Integer teamId, Integer price) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        
        PlayerTeam playerTeam = new PlayerTeam();
        playerTeam.setPlayer(player);
        playerTeam.setTeam(team);
        playerTeam.setSellPrice(price);
        playerTeam.setIsCaptain(false);
        playerTeam.setLined(false);
        
        return playerTeamRepository.save(playerTeam);
    }
}
