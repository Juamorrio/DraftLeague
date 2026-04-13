package com.DraftLeague.services;

import com.DraftLeague.models.League.League;
import com.DraftLeague.repositories.LeagueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketScheduler Unit Tests")
class MarketSchedulerTest {

    @Mock
    private MarketService marketService;

    @Mock
    private LeagueRepository leagueRepository;

    @InjectMocks
    private MarketScheduler marketScheduler;


    @Test
    @DisplayName("finalizeExpiredAuctions: llama marketService.finalizeExpiredAuctions()")
    void finalizeExpiredAuctions_delegatesToService() {
        marketScheduler.finalizeExpiredAuctions();

        verify(marketService).finalizeExpiredAuctions();
    }


    @Test
    @DisplayName("finalizeAndRefreshMarkets: una liga → llama refreshMarket con su id")
    void finalizeAndRefreshMarkets_oneLeague_callsRefresh() {
        League league = new League();
        league.setId(1);
        when(leagueRepository.findAll()).thenReturn(List.of(league));

        marketScheduler.finalizeAndRefreshMarkets();

        verify(marketService).refreshMarket(1);
    }

    @Test
    @DisplayName("finalizeAndRefreshMarkets: sin ligas → refreshMarket no se llama")
    void finalizeAndRefreshMarkets_noLeagues_refreshNeverCalled() {
        when(leagueRepository.findAll()).thenReturn(Collections.emptyList());

        marketScheduler.finalizeAndRefreshMarkets();

        verify(marketService, never()).refreshMarket(anyInt());
    }
}
