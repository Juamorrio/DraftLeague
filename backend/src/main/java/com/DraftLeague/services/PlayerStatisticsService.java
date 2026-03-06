package com.DraftLeague.services;

import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerStatisticsService.class);

    private final PlayerStatisticRepository playerStatisticRepository;

    @Transactional
    public int recalculateAllFantasyPoints() {
        logger.info("[*] Recalculating fantasy points for all player statistics...");

        List<PlayerStatistic> allStats = playerStatisticRepository.findAll();
        List<PlayerStatistic> toSave = new ArrayList<>(allStats.size());
        int skipped = 0;

        for (PlayerStatistic stat : allStats) {
            try {
                stat.setTotalFantasyPoints(stat.calculateFantasyPoints());
                toSave.add(stat);
            } catch (Exception e) {
                logger.error("[!] Error calculating points for statistic ID {}: {}", stat.getId(), e.getMessage());
                skipped++;
            }
        }

        playerStatisticRepository.saveAll(toSave);

        logger.info("[+] Successfully updated {} player statistics ({} skipped)", toSave.size(), skipped);
        return toSave.size();
    }
}
