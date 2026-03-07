package com.DraftLeague.services;

import com.DraftLeague.dto.MarketPlayerDTO;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Market.MarketPlayer;
import com.DraftLeague.models.Market.StatusMarketPlayer;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.MarketPlayerRepository;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.PlayerTeamRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.UserRepository;
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
    private final NotificationService notificationService;
    private final GameweekStateService gameweekStateService;

    public MarketService(MarketPlayerRepository marketPlayerRepository, 
                        PlayerRepository playerRepository,
                        LeagueRepository leagueRepository,
                        UserRepository userRepository,
                        TeamRepository teamRepository,
                        PlayerTeamRepository playerTeamRepository,
                        NotificationService notificationService,
                        GameweekStateService gameweekStateService) {
        this.marketPlayerRepository = marketPlayerRepository;
        this.playerRepository = playerRepository;
        this.leagueRepository = leagueRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.notificationService = notificationService;
        this.gameweekStateService = gameweekStateService;
    }

    @Transactional
    public void initializeMarket(Integer leagueId) {

        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));

        List<Integer> teamIds = teamRepository.findByLeague(league).stream()
                .map(Team::getId).toList();

        List<Player> allPlayers = new ArrayList<>(playerRepository.findAll().stream()
                .filter(p -> playerTeamRepository.findPlayerTeamsByTeamIdIn(teamIds).stream()
                        .noneMatch(pt -> pt.getPlayer().getId().equals(p.getId()))).toList());

        Collections.shuffle(allPlayers);
        List<Player> selectedPlayers = allPlayers.subList(0, Math.min(10, allPlayers.size()));

        List<MarketPlayer> newMarketPlayers = new ArrayList<>();
        for (Player player : selectedPlayers) {
            MarketPlayer marketPlayer = new MarketPlayer();
            marketPlayer.setPlayer(player);
            marketPlayer.setLeague(league);
            marketPlayer.setCurrentBid(player.getMarketValue().longValue());
            marketPlayer.setStatus(StatusMarketPlayer.AVAILABLE);
            marketPlayer.setAuctionEndTime(LocalDateTime.now().plusHours(24));
            newMarketPlayers.add(marketPlayer);
        }
        marketPlayerRepository.saveAll(newMarketPlayers);

        logger.info("Mercado inicializado con {} jugadores", newMarketPlayers.size());
    }

    public List<MarketPlayer> getAvailableMarketPlayers(Integer leagueId) {
        logger.info("Obteniendo jugadores del mercado para liga {}", leagueId);
        
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada"));
        
        List<MarketPlayer> availablePlayers = marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE);
        logger.info("Jugadores disponibles en mercado: {}", availablePlayers.size());
        
        if (availablePlayers.isEmpty()) {
            logger.info("Mercado vacÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­o, inicializando automÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ticamente...");
            initializeMarket(leagueId);
            availablePlayers = marketPlayerRepository.findByLeagueAndStatus(league, StatusMarketPlayer.AVAILABLE);
            logger.info("DespuÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©s de inicializar, jugadores disponibles: {}", availablePlayers.size());
        }
        
        return availablePlayers;
    }
    
    public List<MarketPlayerDTO> getAvailableMarketPlayersForUser(Integer leagueId, String username) {
        logger.info("Obteniendo jugadores del mercado para liga {} y usuario {}", leagueId, username);
        
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        
        List<MarketPlayer> availablePlayers = getAvailableMarketPlayers(leagueId);
        
        return availablePlayers.stream().map(mp -> {
            Map<String, Object> userBid = mp.getUserBid(user.getId());
            Long myBid = null;
            Boolean hasBid = false;
            
            if (userBid != null) {
                myBid = ((Number) userBid.get("amount")).longValue();
                hasBid = true;
            }
            
            return new MarketPlayerDTO(
                mp.getId(),
                mp.getPlayer(),
                mp.getAuctionEndTime(),
                mp.getStatus(),
                myBid,
                hasBid
            );
        }).toList();
    }

    @Transactional
    public void placeBid(Integer marketPlayerId, String username, Long bidAmount) {
        if (gameweekStateService.isTeamsLocked()) {
            throw new IllegalStateException("El mercado está cerrado durante la jornada activa");
        }

        MarketPlayer marketPlayer = marketPlayerRepository.findById(marketPlayerId)
                .orElseThrow(() -> new IllegalStateException("Jugador de mercado no encontrado"));

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        Long playerMarketValue = marketPlayer.getPlayer().getMarketValue().longValue();
        if (bidAmount < playerMarketValue) {
            throw new IllegalStateException("La puja no puede ser inferior al valor de mercado del jugador");
        }

        League league = marketPlayer.getLeague();
        Team team = teamRepository.findByLeagueAndUser(league, user);
        
        Map<String, Object> existingBid = marketPlayer.getUserBid(user.getId());
        Long previousBidAmount = 0L;
        
        if (existingBid != null) {
            previousBidAmount = ((Number) existingBid.get("amount")).longValue();
            team.setBudget(team.getBudget() + previousBidAmount.intValue());
        }
        
        if (team.getBudget() < bidAmount) {
            throw new IllegalStateException("Presupuesto insuficiente");
        }
        
        team.setBudget(team.getBudget() - bidAmount.intValue());
        teamRepository.save(team);
        
        if (existingBid != null) {
            marketPlayer.removeBid(user.getId());
        }
        
        marketPlayer.addBid(user.getId(), bidAmount);
        
        Map<String, Object> highestBid = marketPlayer.getHighestBidInfo();
        if (highestBid != null) {
            marketPlayer.setCurrentBid(((Number) highestBid.get("amount")).longValue());
            Integer highestBidderId = (Integer) highestBid.get("userId");
            User highestBidderUser = userRepository.findById(highestBidderId)
                    .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
            marketPlayer.setHighestBidder(highestBidderUser);
        }
        
        marketPlayerRepository.save(marketPlayer);
    }

    @Transactional
    public void finalizeAuction(Integer marketPlayerId) {
        MarketPlayer marketPlayer = marketPlayerRepository.findById(marketPlayerId)
                .orElseThrow(() -> new IllegalStateException("Jugador de mercado no encontrado"));

        Map<String, Object> highestBid = marketPlayer.getHighestBidInfo();
        
        if (highestBid == null) {
            // Auction ended without bids; mark as closed so it is not processed as AVAILABLE again.
            marketPlayer.setStatus(StatusMarketPlayer.SOLD);
            marketPlayerRepository.save(marketPlayer);
            return;
        }
        
        Integer winnerId = (Integer) highestBid.get("userId");
        Long winningAmount = ((Number) highestBid.get("amount")).longValue();
        
        User winner = userRepository.findById(winnerId)
                .orElseThrow(() -> new IllegalStateException("Usuario ganador no encontrado"));
        League league = marketPlayer.getLeague();

        List<Map<String, Object>> allBids = marketPlayer.getBidsList();
        List<Team> teamsToUpdate = new ArrayList<>();
        for (Map<String, Object> bid : allBids) {
            Integer bidderId = (Integer) bid.get("userId");
            Long bidAmount = ((Number) bid.get("amount")).longValue();

            if (!bidderId.equals(winnerId)) {
                User bidder = userRepository.findById(bidderId)
                        .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
                Team bidderTeam = teamRepository.findByLeagueAndUser(league, bidder);
                bidderTeam.setBudget(bidderTeam.getBudget() + bidAmount.intValue());
                teamsToUpdate.add(bidderTeam);
            }
        }
        teamRepository.saveAll(teamsToUpdate);

        Team winnerTeam = teamRepository.findByLeagueAndUser(league, winner);
        
        PlayerTeam playerTeam = new PlayerTeam();
        playerTeam.setTeam(winnerTeam);
        playerTeam.setPlayer(marketPlayer.getPlayer());
        playerTeam.setLined(false);
        playerTeam.setIsCaptain(false);
        playerTeam.setSellPrice(winningAmount.intValue());
        playerTeam.setBuyPrice(winningAmount.intValue());
        playerTeamRepository.save(playerTeam);

        marketPlayer.setStatus(StatusMarketPlayer.SOLD);
        marketPlayerRepository.save(marketPlayer);
        notificationService.createMarketBuyNotification(marketPlayer.getLeague().getId(), winner, marketPlayer.getPlayer(), winningAmount);
    }
    
    @Transactional
    public void finalizeExpiredAuctions() {
        List<MarketPlayer> expiredAuctions = marketPlayerRepository
                .findByStatusAndAuctionEndTimeBefore(StatusMarketPlayer.AVAILABLE, LocalDateTime.now());
        
        for (MarketPlayer marketPlayer : expiredAuctions) {
            finalizeAuction(marketPlayer.getId());
        }
    }

    @Transactional
    public void refreshMarket(Integer leagueId) {
        logger.info("=== Refrescando mercado ===");
        logger.info("LeagueId recibido: {}", leagueId);
        logger.info("Tipo: {}", leagueId.getClass().getName());
        logger.info("Convirtiendo a Long: {}", Long.valueOf(leagueId));
        
        League league = leagueRepository.findById(Long.valueOf(leagueId))
                .orElseThrow(() -> new IllegalStateException("Liga no encontrada con id: " + leagueId));

        List<MarketPlayer> expiredAuctions = marketPlayerRepository
                .findByLeagueAndStatusAndAuctionEndTimeBefore(league, StatusMarketPlayer.AVAILABLE, LocalDateTime.now());
        for (MarketPlayer expiredAuction : expiredAuctions) {
            finalizeAuction(expiredAuction.getId());
        }

        List<MarketPlayer> oldMarketPlayers = marketPlayerRepository.findByLeague(league);
        Set<String> previousMarketPlayerIds = oldMarketPlayers.stream()
            .map(mp -> mp.getPlayer().getId())
            .collect(java.util.stream.Collectors.toSet());
        marketPlayerRepository.deleteAll(oldMarketPlayers);

        List<Integer> teamIds = teamRepository.findByLeague(league).stream()
                .map(Team::getId).toList();

        Set<String> ownedPlayerIds = playerTeamRepository.findPlayerTeamsByTeamIdIn(teamIds).stream()
            .map(pt -> pt.getPlayer().getId())
            .collect(java.util.stream.Collectors.toSet());

        final int marketSize = 10;

        // Prefer players not owned and not present in the previous market.
        List<Player> freshCandidates = playerRepository.findAll().stream()
            .filter(p -> !ownedPlayerIds.contains(p.getId()))
            .filter(p -> !previousMarketPlayerIds.contains(p.getId()))
            .toList();

        List<Player> fallbackCandidates = playerRepository.findAll().stream()
            .filter(p -> !ownedPlayerIds.contains(p.getId()))
            .toList();

        List<Player> selectedPlayers = new ArrayList<>();
        List<Player> shuffledFresh = new ArrayList<>(freshCandidates);
        Collections.shuffle(shuffledFresh);
        selectedPlayers.addAll(shuffledFresh.subList(0, Math.min(marketSize, shuffledFresh.size())));

        if (selectedPlayers.size() < marketSize) {
            Set<String> selectedIds = selectedPlayers.stream()
                .map(Player::getId)
                .collect(java.util.stream.Collectors.toSet());

            List<Player> fallbackPool = fallbackCandidates.stream()
                .filter(p -> !selectedIds.contains(p.getId()))
                .toList();

            List<Player> shuffledFallback = new ArrayList<>(fallbackPool);
            Collections.shuffle(shuffledFallback);

            int remaining = marketSize - selectedPlayers.size();
            selectedPlayers.addAll(shuffledFallback.subList(0, Math.min(remaining, shuffledFallback.size())));
        }

        List<MarketPlayer> newMarketPlayers = new ArrayList<>();
        for (Player player : selectedPlayers) {
            MarketPlayer marketPlayer = new MarketPlayer();
            marketPlayer.setPlayer(player);
            marketPlayer.setLeague(league);
            marketPlayer.setCurrentBid(0L);
            marketPlayer.setStatus(StatusMarketPlayer.AVAILABLE);
            marketPlayer.setAuctionEndTime(LocalDateTime.now().plusHours(24));
            newMarketPlayers.add(marketPlayer);
        }
        marketPlayerRepository.saveAll(newMarketPlayers);

        logger.info("Mercado refrescado con {} jugadores", newMarketPlayers.size());
    }
    @Transactional
    public void cancelBid(Integer marketPlayerId, User user) {
        MarketPlayer marketPlayer = marketPlayerRepository.findById(marketPlayerId)
            .orElseThrow(() -> new RuntimeException("Jugador del mercado no encontrado"));
        
        Map<String, Object> userBid = marketPlayer.getUserBid(user.getId());
        if (userBid == null) {
            throw new RuntimeException("No tienes una puja activa en este jugador");
        }
        
        Long bidAmount = ((Number) userBid.get("amount")).longValue();
        
        Team team = teamRepository.findByLeagueAndUser(marketPlayer.getLeague(), user);
        team.setBudget(team.getBudget() + bidAmount.intValue());
        teamRepository.save(team);
        
        marketPlayer.removeBid(user.getId());
        
        Map<String, Object> newHighestBid = marketPlayer.getHighestBidInfo();
        if (newHighestBid != null) {
            marketPlayer.setCurrentBid(((Number) newHighestBid.get("amount")).longValue());
            Integer highestBidderId = (Integer) newHighestBid.get("userId");
            User highestBidderUser = userRepository.findById(highestBidderId)
                    .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
            marketPlayer.setHighestBidder(highestBidderUser);
        } else {
            marketPlayer.setCurrentBid(0L);
            marketPlayer.setHighestBidder(null);
        }
        
        marketPlayerRepository.save(marketPlayer);
    }

}
