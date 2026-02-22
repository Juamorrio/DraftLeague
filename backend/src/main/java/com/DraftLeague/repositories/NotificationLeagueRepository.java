package com.DraftLeague.repositories;
import com.DraftLeague.models.Notification.NotificationLeague;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.DraftLeague.repositories.NotificationLeagueRepository;

@Repository
public interface NotificationLeagueRepository extends JpaRepository<NotificationLeague, Integer> {
    Optional<NotificationLeague> findByLeagueId(Integer leagueId);
}
