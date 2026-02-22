package com.DraftLeague.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.DraftLeague.models.Team.Team;
import com.DraftLeague.repositories.TeamRepository;

import jakarta.transaction.Transactional;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.PlayerTeamRepository;

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
