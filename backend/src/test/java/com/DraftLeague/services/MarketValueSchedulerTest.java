package com.DraftLeague.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketValueScheduler Unit Tests")
class MarketValueSchedulerTest {

    @Mock
    private MarketValueUpdateService marketValueUpdateService;

    @Mock
    private GameweekStateService gameweekStateService;

    @InjectMocks
    private MarketValueScheduler marketValueScheduler;


    @Test
    @DisplayName("dailyMarketValueUpdate: jornada activa disponible → llama recalculate con la jornada")
    void dailyMarketValueUpdate_withActiveGameweek_callsRecalculate() {
        when(gameweekStateService.getActiveGameweek()).thenReturn(5);
        when(marketValueUpdateService.recalculateAllMarketValuesForGameweek(5))
                .thenReturn(Map.of("updatedCount", 20, "skippedCount", 0, "errorCount", 0));

        marketValueScheduler.dailyMarketValueUpdate();

        verify(marketValueUpdateService).recalculateAllMarketValuesForGameweek(5);
    }

    @Test
    @DisplayName("dailyMarketValueUpdate: jornada activa null → llama recalculate con null (no se omite)")
    void dailyMarketValueUpdate_nullGameweek_callsRecalculateWithNull() {
        when(gameweekStateService.getActiveGameweek()).thenReturn(null);
        when(marketValueUpdateService.recalculateAllMarketValuesForGameweek(null))
                .thenReturn(Map.of("updatedCount", 0, "skippedCount", 0, "errorCount", 0));

        marketValueScheduler.dailyMarketValueUpdate();

        verify(marketValueUpdateService).recalculateAllMarketValuesForGameweek(null);
    }

    @Test
    @DisplayName("dailyMarketValueUpdate: recalculate lanza excepción → no se propaga")
    void dailyMarketValueUpdate_exceptionThrown_doesNotPropagate() {
        when(gameweekStateService.getActiveGameweek()).thenReturn(3);
        when(marketValueUpdateService.recalculateAllMarketValuesForGameweek(3))
                .thenThrow(new RuntimeException("DB error"));

        // No debe lanzar excepción
        marketValueScheduler.dailyMarketValueUpdate();
    }

    @Test
    @DisplayName("postMatchdayMarketValueUpdate: llama recalculate con la jornada activa")
    void postMatchdayMarketValueUpdate_callsRecalculate() {
        when(gameweekStateService.getActiveGameweek()).thenReturn(7);
        when(marketValueUpdateService.recalculateAllMarketValuesForGameweek(7))
                .thenReturn(Map.of("updatedCount", 10, "skippedCount", 1, "errorCount", 0));

        marketValueScheduler.postMatchdayMarketValueUpdate();

        verify(marketValueUpdateService).recalculateAllMarketValuesForGameweek(7);
    }
}
