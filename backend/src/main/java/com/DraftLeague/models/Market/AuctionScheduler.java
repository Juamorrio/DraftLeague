package com.DraftLeague.models.Market;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionScheduler {

    private final MarketService marketService;

    public AuctionScheduler(MarketService marketService) {
        this.marketService = marketService;
    }

    @Scheduled(fixedRate = 300000)
    public void finalizeExpiredAuctions() {
        marketService.finalizeExpiredAuctions();
    }
}
