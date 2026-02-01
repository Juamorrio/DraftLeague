package com.DraftLeague.models.Notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationLeagueRepository extends JpaRepository<NotificationLeague, Integer> {
    Optional<NotificationLeague> findByLeagueId(Integer leagueId);
}
