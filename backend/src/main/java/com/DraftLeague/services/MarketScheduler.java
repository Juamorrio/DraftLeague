package com.DraftLeague.services;

import com.DraftLeague.models.League.League;
import com.DraftLeague.repositories.LeagueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketScheduler.class);
    
    private final MarketService marketService;
    private final LeagueRepository leagueRepository;

    public MarketScheduler(MarketService marketService, LeagueRepository leagueRepository) {
        this.marketService = marketService;
        this.leagueRepository = leagueRepository;
    }

    // Runs daily at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void finalizeAndRefreshMarkets() {
        try {
            List<League> leagues = leagueRepository.findAll();
            
            for (League league : leagues) {
                try {
                    marketService.refreshMarket(league.getId().intValue());
                } catch (Exception e) {
                    logger.error("Error al refrescar mercado para la liga {}: {}", league.getId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar mercados: {}", e.getMessage(), e);
        }
    }
}
