package com.DraftLeague.repositories;

import com.DraftLeague.models.Player.PlayerMarketValueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerMarketValueHistoryRepository extends JpaRepository<PlayerMarketValueHistory, Integer> {

    List<PlayerMarketValueHistory> findByPlayerIdOrderByGameweekAsc(String playerId);

    Optional<PlayerMarketValueHistory> findByPlayerIdAndGameweek(String playerId, Integer gameweek);
}
