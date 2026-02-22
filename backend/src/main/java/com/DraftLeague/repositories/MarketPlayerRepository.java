package com.DraftLeague.repositories;
import com.DraftLeague.models.Market.StatusMarketPlayer;

import com.DraftLeague.models.League.League;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Market.MarketPlayer;
import com.DraftLeague.repositories.MarketPlayerRepository;

@Repository
public interface MarketPlayerRepository extends JpaRepository<MarketPlayer, Integer> {
    List<MarketPlayer> findByLeagueAndStatus(League league, StatusMarketPlayer status);
    List<MarketPlayer> findByStatusAndAuctionEndTimeBefore(StatusMarketPlayer status, LocalDateTime dateTime);
    List<MarketPlayer> findByLeagueAndStatusAndAuctionEndTimeBefore(League league, StatusMarketPlayer available,
            LocalDateTime now);
    List<MarketPlayer> findByLeague(League league);
}
