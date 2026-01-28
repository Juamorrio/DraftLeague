package com.DraftLeague.models.Market;

import com.DraftLeague.models.Player.Player;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MarketPlayerDTO {
    private Integer id;
    private Player player;
    private LocalDateTime auctionEndTime;
    private StatusMarketPlayer status;
    private Long myBid; 
    private Boolean hasBid; 
    
    public MarketPlayerDTO(Integer id, Player player, LocalDateTime auctionEndTime, 
                          StatusMarketPlayer status, Long myBid, Boolean hasBid) {
        this.id = id;
        this.player = player;
        this.auctionEndTime = auctionEndTime;
        this.status = status;
        this.myBid = myBid;
        this.hasBid = hasBid;
    }
}
