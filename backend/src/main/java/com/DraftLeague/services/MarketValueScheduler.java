package com.DraftLeague.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Scheduled trigger for automatic market-value recalculation.
 *
 * <p>Two schedules are configured:</p>
 * <ol>
 *   <li><b>Daily maintenance run</b> – every day at 04:00 to keep prices
 *       generally up to date with form trends.</li>
 *   <li><b>Post-matchday run</b> – every day at 23:30, which will pick up any
 *       statistics recorded after evening kick-offs and immediately reflect them
 *       in market prices for the next morning's market window.</li>
 * </ol>
 *
 * <p>Both schedules are defined as standard cron expressions so they can be
 * overridden through {@code application.properties} without recompiling.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketValueScheduler {

    private final MarketValueUpdateService marketValueUpdateService;

    /**
     * Daily maintenance price update – runs every day at 04:00 server time.
     * <p>Cron: {@code 0 0 4 * * *}</p>
     */
    @Scheduled(cron = "${market.value.schedule.daily:0 0 4 * * *}")
    public void dailyMarketValueUpdate() {
        log.info("[Scheduler] Starting daily market value update (04:00)…");
        runUpdate("daily-04:00");
    }

    /**
     * Post-matchday price update – runs every day at 23:30 server time.
     * <p>Cron: {@code 0 30 23 * * *}</p>
     */
    @Scheduled(cron = "${market.value.schedule.postMatchday:0 30 23 * * *}")
    public void postMatchdayMarketValueUpdate() {
        log.info("[Scheduler] Starting post-matchday market value update (23:30)…");
        runUpdate("post-matchday-23:30");
    }

    // ── private ────────────────────────────────────────────────────────────────

    private void runUpdate(String label) {
        try {
            Map<String, Integer> result = marketValueUpdateService.recalculateAllMarketValues();
            log.info("[Scheduler][{}] Market values updated: updated={}, skipped={}, errors={}",
                    label,
                    result.getOrDefault("updatedCount", 0),
                    result.getOrDefault("skippedCount", 0),
                    result.getOrDefault("errorCount", 0));
        } catch (Exception e) {
            log.error("[Scheduler][{}] Market value update failed: {}", label, e.getMessage(), e);
        }
    }
}
