package com.DraftLeague.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.DraftLeague.models.League.League;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.PlayerTeamRepository;
import com.DraftLeague.services.PlayerService;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final LeagueRepository leagueRepository;

    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository, 
                         PlayerTeamRepository playerTeamRepository, LeagueRepository leagueRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.leagueRepository = leagueRepository;
    }

    public Player createPlayer(Player player) {
        return playerRepository.save(player);
    }

    public Player getPlayerById(String id) {
        return playerRepository.findById(id).orElseThrow(() -> new RuntimeException("Player not found"));
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }
 
    public List<Player> getPlayersByUserAndLeague(User user, Integer leagueId) {
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new RuntimeException("League not found"));
        
        Team team = teamRepository.findByLeagueAndUser(league, user);
        if (team == null) {
            return List.of();
        }
        
        List<PlayerTeam> playerTeams = playerTeamRepository.findByTeam(team);
        return playerTeams.stream()
                .map(PlayerTeam::getPlayer)
                .collect(Collectors.toList());
    }
    

    public List<Player> getAvailablePlayersForUserInLeague(User user, Integer leagueId) {
        List<Player> ownedPlayers = getPlayersByUserAndLeague(user, leagueId);
        List<String> ownedPlayerIds = ownedPlayers.stream()
                .map(Player::getId)
                .collect(Collectors.toList());
        
        return playerRepository.findAll().stream()
                .filter(p -> !ownedPlayerIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    public void deletePlayer(String id) {
        Player player = getPlayerById(id);
        playerRepository.delete(player);
    }

    public void purchasePlayer(String playerId, Integer leagueId, User user) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalStateException("Jugador no encontrado"));
        
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));
        
        Team team = teamRepository.findByLeagueAndUser(league, user);
        if (team == null) {
            throw new IllegalStateException("No tienes un equipo en esta liga");
        }
                
        boolean alreadyOwned = playerTeamRepository.existsByTeamAndPlayer(team, player);
        if (alreadyOwned) {
            throw new IllegalStateException("Ya posees este jugador en esta liga");
        }
        
        PlayerTeam playerTeam = new PlayerTeam();
        playerTeam.setTeam(team);
        playerTeam.setPlayer(player);
        playerTeam.setLined(false);
        playerTeam.setIsCaptain(false);
        playerTeam.setSellPrice(player.getMarketValue());
        playerTeam.setBuyPrice(player.getMarketValue());
        
        playerTeamRepository.save(playerTeam);
    }

    public byte[] fetchPlayerTeamImage(String teamId) {
    String url = "https://media.api-sports.io/football/teams/" + teamId + ".png";
    
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
    
    try {
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("Error: HTTP " + response.statusCode());
        }
    } catch (Exception e) {
        throw new RuntimeException("Error fetching player image", e);
    }
}

}
