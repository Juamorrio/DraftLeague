package com.DraftLeague.models.Market;

import com.DraftLeague.models.League.League;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MarketPlayerRepository extends JpaRepository<MarketPlayer, Integer> {
    List<MarketPlayer> findByLeagueAndStatus(League league, StatusMarketPlayer status);
    List<MarketPlayer> findByStatusAndAuctionEndTimeBefore(StatusMarketPlayer status, LocalDateTime dateTime);
    List<MarketPlayer> findByLeagueAndStatusAndAuctionEndTimeBefore(League league, StatusMarketPlayer available,
            LocalDateTime now);
    List<MarketPlayer> findByLeague(League league);
}
