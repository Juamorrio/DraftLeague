package com.DraftLeague.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.DraftLeague.models.Notification.Notification;
import com.DraftLeague.models.Notification.NotificationType;
import com.DraftLeague.repositories.NotificationRepository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    @Query("SELECT n FROM Notification n WHERE n.notificationLeague.league.id = :leagueId ORDER BY n.createdAt DESC")
    List<Notification> findByLeagueIdOrderByCreatedAtDesc(@Param("leagueId") Integer leagueId);
    
    @Query("SELECT n FROM Notification n WHERE n.notificationLeague.league.id = :leagueId AND n.id > :lastId ORDER BY n.createdAt DESC")
    List<Notification> findNewNotificationsByLeagueId(@Param("leagueId") Integer leagueId, @Param("lastId") Integer lastId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.notificationLeague.league.id = :leagueId AND n.type = :type AND n.payload LIKE %:offerToken%")
    int deleteTradeOfferNotification(@Param("leagueId") Integer leagueId,
                                     @Param("type") NotificationType type,
                                     @Param("offerToken") String offerToken);
}
