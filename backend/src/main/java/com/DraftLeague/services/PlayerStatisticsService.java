package com.DraftLeague.services;

import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlayerStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerStatisticsService.class);

    private final PlayerStatisticRepository playerStatisticRepository;
    private final MatchRepository matchRepository;

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

 
    @Transactional
    public int recalculateCleanSheets() {
        logger.info("[*] Backfilling clean-sheet data for all player statistics...");

        List<PlayerStatistic> allStats = playerStatisticRepository.findAll();

        Map<Integer, Match> matchCache = new HashMap<>();
        matchRepository.findAll().forEach(m -> matchCache.put(m.getId(), m));

        List<PlayerStatistic> toSave = new ArrayList<>(allStats.size());
        int updated = 0;

        for (PlayerStatistic stat : allStats) {
            try {
                Match match = stat.getMatchId() != null ? matchCache.get(stat.getMatchId()) : null;
                if (match == null) continue;

                Boolean isHome = stat.getIsHomeTeam();
                if (isHome == null) continue;

                Integer conceded = isHome ? match.getAwayGoals() : match.getHomeGoals();
                if (conceded == null) continue;

                PlayerStatistic.PlayerType type = stat.getPlayerType();
                boolean relevant = type == PlayerStatistic.PlayerType.GOALKEEPER
                        || type == PlayerStatistic.PlayerType.DEFENDER
                        || type == PlayerStatistic.PlayerType.MIDFIELDER;

                if (relevant && stat.getCleanSheet() == null) {
                    stat.setCleanSheet(conceded == 0);
                    updated++;
                }

                if (stat.getGoalsConceded() == null &&
                        (type == PlayerStatistic.PlayerType.GOALKEEPER
                                || type == PlayerStatistic.PlayerType.DEFENDER)) {
                    stat.setGoalsConceded(conceded);
                    updated++;
                }

                stat.setTotalFantasyPoints(stat.calculateFantasyPoints());
                toSave.add(stat);
            } catch (Exception e) {
                logger.error("[!] Error backfilling stat ID {}: {}", stat.getId(), e.getMessage());
            }
        }

        playerStatisticRepository.saveAll(toSave);
        logger.info("[+] Backfilled clean-sheet data, {} field updates applied across {} stats", updated, toSave.size());
        return toSave.size();
    }
}
