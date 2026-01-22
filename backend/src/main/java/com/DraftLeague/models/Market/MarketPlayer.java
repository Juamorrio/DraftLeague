package com.DraftLeague.models.Market;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.user.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
}
