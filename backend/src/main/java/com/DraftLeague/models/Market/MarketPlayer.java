package com.DraftLeague.models.Market;
import com.DraftLeague.models.Market.StatusMarketPlayer;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Market.MarketPlayer;

@Entity
@Getter
@Setter
@Table(name = "market_player")
public class MarketPlayer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(name = "current_bid")
    private Long currentBid = 0L;

    @ManyToOne
    @JoinColumn(name = "highest_bidder_id")
    private User highestBidder;

    @Column(name = "auction_end_time")
    private LocalDateTime auctionEndTime;

    @Column(name = "status")
    private StatusMarketPlayer status;
    
    @Column(name = "bids", columnDefinition = "TEXT")
    private String bids; 
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<Map<String, Object>> getBidsList() {
        if (bids == null || bids.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(bids, new TypeReference<List<Map<String, Object>>>(){});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
    
    public void addBid(Integer userId, Long amount) {
        List<Map<String, Object>> bidsList = getBidsList();
        Map<String, Object> newBid = new HashMap<>();
        newBid.put("userId", userId);
        newBid.put("amount", amount);
        newBid.put("timestamp", LocalDateTime.now().toString());
        bidsList.add(newBid);
        
        try {
            this.bids = objectMapper.writeValueAsString(bidsList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar pujas", e);
        }
    }
    
    public Map<String, Object> getUserBid(Integer userId) {
        List<Map<String, Object>> bidsList = getBidsList();
        return bidsList.stream()
            .filter(bid -> {
                Object storedId = bid.get("userId");
                return storedId instanceof Number && ((Number) storedId).intValue() == userId;
            })
            .reduce((first, second) -> second)
            .orElse(null);
    }
    
    public void removeBid(Integer userId) {
        List<Map<String, Object>> bidsList = getBidsList();
        bidsList.removeIf(bid -> {
            Object storedId = bid.get("userId");
            return storedId instanceof Number && ((Number) storedId).intValue() == userId;
        });
        
        try {
            this.bids = objectMapper.writeValueAsString(bidsList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar pujas", e);
        }
    }
    
    public Map<String, Object> getHighestBidInfo() {
        List<Map<String, Object>> bidsList = getBidsList();
        return bidsList.stream()
            .max((bid1, bid2) -> {
                Long amount1 = ((Number) bid1.get("amount")).longValue();
                Long amount2 = ((Number) bid2.get("amount")).longValue();
                return Long.compare(amount1, amount2);
            })
            .orElse(null);
    }
}
