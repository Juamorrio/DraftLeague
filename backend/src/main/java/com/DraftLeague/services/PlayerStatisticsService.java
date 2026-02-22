package com.DraftLeague.services;

import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.PlayerStatisticRepository;


@Service
@RequiredArgsConstructor
public class PlayerStatisticsService {

    private final PlayerStatisticRepository playerStatisticRepository;

 
    public int recalculateAllFantasyPoints() {
        System.out.println("[*] Recalculating fantasy points for all player statistics...");

        List<PlayerStatistic> allStats = playerStatisticRepository.findAll();
        int updatedCount = 0;

        for (PlayerStatistic stat : allStats) {
            try {
                int calculatedPoints = stat.calculateFantasyPoints();
                stat.setTotalFantasyPoints(calculatedPoints);
                playerStatisticRepository.save(stat);
                updatedCount++;

                if (updatedCount % 100 == 0) {
                    System.out.println("[*] Updated " + updatedCount + " statistics...");
                }
            } catch (Exception e) {
                System.err.println("[!] Error calculating points for statistic ID " + stat.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("[+] Successfully updated " + updatedCount + " player statistics");
        return updatedCount;
    }
}
