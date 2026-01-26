package com.DraftLeague.models.Market;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.League.LeagueRepository;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerRepository;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Player.PlayerTeamRepository;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Team.TeamRepository;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MarketService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketService.class);
    
    private final MarketPlayerRepository marketPlayerRepository;
    private final PlayerRepository playerRepository;
    private final LeagueRepository leagueRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PlayerTeamRepository playerTeamRepository;

    public MarketService(MarketPlayerRepository marketPlayerRepository, 
                        PlayerRepository playerRepository,
                        LeagueRepository leagueRepository,
                        UserRepository userRepository,
                        TeamRepository teamRepository,
                        PlayerTeamRepository playerTeamRepository) {
        this.marketPlayerRepository = marketPlayerRepository;
        this.playerRepository = playerRepository;
        this.leagueRepository = leagueRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.playerTeamRepository = playerTeamRepository;
    }

    @Transactional
    public void initializeMarket(Integer leagueId) {
        logger.info("Inicializando mercado para liga {} con 10 jugadores", leagueId);
        
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));

        List<Player> allPlayers = playerRepository.findAll();
        logger.info("Total de jugadores en BD: {}", allPlayers.size());
        
        if (allPlayers.isEmpty()) {
            logger.warn("No hay jugadores en la base de datos para inicializar el mercado");
            return;
        }
        
        Collections.shuffle(allPlayers);
        List<Player> selectedPlayers = allPlayers.subList(0, 10);

        for (Player player : selectedPlayers) {
            MarketPlayer marketPlayer = new MarketPlayer();
            marketPlayer.setPlayer(player);
            marketPlayer.setLeague(league);
            marketPlayer.setCurrentBid(player.getMarketValue().longValue());
            marketPlayer.setStatus(StatusMarketPlayer.AVAILABLE);
            marketPlayer.setAuctionEndTime(LocalDateTime.now().plusHours(24));
            marketPlayerRepository.save(marketPlayer);
        }
        
        logger.info("Mercado inicializado con {} jugadores", selectedPlayers.size());
    }

    public List<MarketPlayer> getAvailableMarketPlayers(Integer leagueId) {
        logger.info("Obteniendo jugadores del mercado para liga {}", leagueId);
        
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));
        
        List<MarketPlayer> availablePlayers = marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE);
        logger.info("Jugadores disponibles en mercado: {}", availablePlayers.size());
        
        if (availablePlayers.isEmpty()) {
            logger.info("Mercado vacío, inicializando automáticamente...");
            initializeMarket(leagueId);
            availablePlayers = marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE);
            logger.info("Después de inicializar, jugadores disponibles: {}", availablePlayers.size());
        }
        
        return availablePlayers;
    }

    @Transactional
    public void placeBid(Integer marketPlayerId, String username, Long bidAmount) {
        MarketPlayer marketPlayer = marketPlayerRepository.findById(marketPlayerId)
                .orElseThrow(() -> new IllegalStateException("Jugador de mercado no encontrado"));

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Long playerMarketValue = marketPlayer.getPlayer().getMarketValue().longValue();
        if (bidAmount < playerMarketValue) {
            throw new IllegalStateException("La puja no puede ser inferior al valor de mercado del jugador");
        }

        marketPlayer.setCurrentBid(bidAmount);
        marketPlayer.setHighestBidder(user);
        marketPlayerRepository.save(marketPlayer);
    }

    @Transactional
    public void finalizeAuction(Integer marketPlayerId) {
        MarketPlayer marketPlayer = marketPlayerRepository.findById(marketPlayerId)
                .orElseThrow(() -> new IllegalStateException("Jugador de mercado no encontrado"));

        if (marketPlayer.getHighestBidder() == null) {
            marketPlayerRepository.save(marketPlayer);
            return;
        }
        User winner = marketPlayer.getHighestBidder();
        League league = marketPlayer.getLeague();

        Team team = teamRepository.findByLeagueAndUser(league, winner);
        
        

        PlayerTeam playerTeam = new PlayerTeam();
        playerTeam.setTeam(team);
        playerTeam.setPlayer(marketPlayer.getPlayer());
        playerTeam.setLined(false);
        playerTeam.setIsCaptain(false);
        playerTeam.setSellPrice(marketPlayer.getCurrentBid().intValue());
        playerTeam.setBuyPrice(marketPlayer.getCurrentBid().intValue());
        playerTeamRepository.save(playerTeam);

        marketPlayer.setStatus(StatusMarketPlayer.SOLD);
        marketPlayerRepository.save(marketPlayer);
    }
    
    @Transactional
    public void finalizeExpiredAuctions() {
        List<MarketPlayer> expiredAuctions = marketPlayerRepository
                .findAll().stream().filter(p -> p.getHighestBidder() != null).toList();
        
        for (MarketPlayer marketPlayer : expiredAuctions) {
            finalizeAuction(marketPlayer.getId());
        }
    }

    @Transactional
    public void refreshMarket(Integer leagueId) {
        
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));

        List<MarketPlayer> expiredAuctions = marketPlayerRepository
                .findByLeagueAndStatusAndAuctionEndTimeBefore(league, StatusMarketPlayer.AVAILABLE, LocalDateTime.now());
        for (MarketPlayer expiredAuction : expiredAuctions) {
            finalizeAuction(expiredAuction.getId());
        }

        List<MarketPlayer> oldMarketPlayers = marketPlayerRepository.findByLeague(league);
        marketPlayerRepository.deleteAll(oldMarketPlayers);

        List<Player> allPlayers = playerRepository.findAll();
        Collections.shuffle(allPlayers);
        List<Player> selectedPlayers = allPlayers.subList(0, Math.min(10, allPlayers.size()));

        for (Player player : selectedPlayers) {
            MarketPlayer marketPlayer = new MarketPlayer();
            marketPlayer.setPlayer(player);
            marketPlayer.setLeague(league);
            marketPlayer.setCurrentBid(0L);
            marketPlayer.setStatus(StatusMarketPlayer.AVAILABLE);
            marketPlayer.setAuctionEndTime(LocalDateTime.now().plusHours(24));
            marketPlayerRepository.save(marketPlayer);
        }
        
    }
}
