package com.DraftLeague.repositories;
import com.DraftLeague.models.Notification.NotificationLeague;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Notification.Notification;
import com.DraftLeague.repositories.NotificationRepository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    @Query("SELECT n FROM Notification n WHERE n.notificationLeague.league.id = :leagueId ORDER BY n.createdAt DESC")
    List<Notification> findByLeagueIdOrderByCreatedAtDesc(@Param("leagueId") Integer leagueId);
    
    @Query("SELECT n FROM Notification n WHERE n.notificationLeague.league.id = :leagueId AND n.id > :lastId ORDER BY n.createdAt DESC")
    List<Notification> findNewNotificationsByLeagueId(@Param("leagueId") Integer leagueId, @Param("lastId") Integer lastId);
}
