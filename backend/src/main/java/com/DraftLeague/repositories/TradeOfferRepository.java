package com.DraftLeague.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Trade.TradeOffer;
import com.DraftLeague.models.Trade.TradeOfferStatus;

@Repository
public interface TradeOfferRepository extends JpaRepository<TradeOffer, Long> {

    List<TradeOffer> findByToTeamAndStatus(Team toTeam, TradeOfferStatus status);

    List<TradeOffer> findByFromTeamAndStatus(Team fromTeam, TradeOfferStatus status);

    List<TradeOffer> findByToTeam(Team toTeam);

    List<TradeOffer> findByFromTeam(Team fromTeam);

    List<TradeOffer> findByLeagueAndPlayerAndStatus(League league, Player player, TradeOfferStatus status);

    void deleteByLeague(League league);
}
