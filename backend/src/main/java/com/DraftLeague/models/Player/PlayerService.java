package com.DraftLeague.models.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.League.LeagueRepository;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Team.TeamRepository;
import com.DraftLeague.models.user.User;

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

    public Player getPlayerById(int id) {
        return playerRepository.findById(id).orElseThrow(() -> new RuntimeException("Player not found"));
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }
 
    public List<Player> getPlayersByUserAndLeague(User user, Integer leagueId) {
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new RuntimeException("League not found"));
        
        List<Team> teams = teamRepository.findByLeagueAndUser(league, user);
        if (teams.isEmpty()) {
            return List.of();
        }
        
        Team team = teams.get(0);
        
        List<PlayerTeam> playerTeams = playerTeamRepository.findByTeam(team);
        return playerTeams.stream()
                .map(PlayerTeam::getPlayer)
                .collect(Collectors.toList());
    }
    

    public List<Player> getAvailablePlayersForUserInLeague(User user, Integer leagueId) {
        List<Player> ownedPlayers = getPlayersByUserAndLeague(user, leagueId);
        List<Integer> ownedPlayerIds = ownedPlayers.stream()
                .map(Player::getId)
                .collect(Collectors.toList());
        
        return playerRepository.findAll().stream()
                .filter(p -> !ownedPlayerIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    public void deletePlayer(int id) {
        Player player = getPlayerById(id);
        playerRepository.delete(player);
    }

    public void purchasePlayer(Integer playerId, Integer leagueId, User user) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalStateException("Jugador no encontrado"));
        
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));
        
        List<Team> teams = teamRepository.findByLeagueAndUser(league, user);
        if (teams.isEmpty()) {
            throw new IllegalStateException("No tienes un equipo en esta liga");
        }
        
        Team team = teams.get(0);
        
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
    String url = "https://images.fotmob.com/image_resources/logo/teamlogo/" + teamId + ".png";
    
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