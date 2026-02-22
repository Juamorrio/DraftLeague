package com.DraftLeague.models.Notification;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.League.LeagueRepository;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private NotificationLeagueRepository notificationLeagueRepository;
    
    @Autowired
    private LeagueRepository leagueRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Transactional
    public void createClauseNotification(Integer leagueId, User buyer, User seller, Player player, int price) {
        League league = leagueRepository.findById(Long.valueOf(leagueId))
            .orElseThrow(() -> new RuntimeException("Liga no encontrada"));
        
        NotificationLeague notificationLeague = notificationLeagueRepository.findByLeagueId(leagueId)
            .orElseGet(() -> {
                NotificationLeague nl = new NotificationLeague();
                nl.setLeague(league);
                return notificationLeagueRepository.save(nl);
            });
        
        Notification notification = new Notification();
        notification.setType(NotificationType.CLAUSE);
        notification.setCreatedAt(new Date());
        notification.setNotificationLeague(notificationLeague);
        
        // Crear payload con información del clausulazo
        Map<String, Object> payload = new HashMap<>();
        payload.put("buyerUsername", buyer.getUsername());
        payload.put("buyerId", buyer.getId());
        payload.put("sellerUsername", seller.getUsername());
        payload.put("sellerId", seller.getId());
        payload.put("playerName", player.getFullName());
        payload.put("playerId", player.getId());
        payload.put("price", price);
        
        try {
            notification.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException("Error al crear payload de notificación", e);
        }
        
        notificationRepository.save(notification);
    }
    
    @Transactional
    public void createMarketBuyNotification(Integer leagueId, User buyer, Player player, long price) {
        League league = leagueRepository.findById(Long.valueOf(leagueId))
            .orElseThrow(() -> new RuntimeException("Liga no encontrada"));
        
        NotificationLeague notificationLeague = notificationLeagueRepository.findByLeagueId(leagueId)
            .orElseGet(() -> {
                NotificationLeague nl = new NotificationLeague();
                nl.setLeague(league);
                return notificationLeagueRepository.save(nl);
            });
        
        Notification notification = new Notification();
        notification.setType(NotificationType.BUY);
        notification.setCreatedAt(new Date());
        notification.setNotificationLeague(notificationLeague);
        
        // Crear payload con información de la compra
        Map<String, Object> payload = new HashMap<>();
        payload.put("buyerUsername", buyer.getUsername());
        payload.put("buyerId", buyer.getId());
        payload.put("playerName", player.getFullName());
        payload.put("playerId", player.getId());
        payload.put("price", price);
        
        try {
            notification.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException("Error al crear payload de notificación", e);
        }
        
        notificationRepository.save(notification);
    }
    
    public List<Notification> getNotificationsByLeague(Integer leagueId) {
        return notificationRepository.findByLeagueIdOrderByCreatedAtDesc(leagueId);
    }
    
    public List<Notification> getNewNotifications(Integer leagueId, Integer lastId) {
        return notificationRepository.findNewNotificationsByLeagueId(leagueId, lastId);
    }

    @Transactional
    public void createSellNotification(Integer leagueId, User seller, Player player, int price) {
        League league = leagueRepository.findById(Long.valueOf(leagueId))
            .orElseThrow(() -> new RuntimeException("Liga no encontrada"));

        NotificationLeague notificationLeague = notificationLeagueRepository.findByLeagueId(leagueId)
            .orElseGet(() -> {
                NotificationLeague nl = new NotificationLeague();
                nl.setLeague(league);
                return notificationLeagueRepository.save(nl);
            });

        Notification notification = new Notification();
        notification.setType(NotificationType.SELL);
        notification.setCreatedAt(new Date());
        notification.setNotificationLeague(notificationLeague);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sellerUsername", seller.getUsername());
        payload.put("sellerId", seller.getId());
        payload.put("playerName", player.getFullName());
        payload.put("playerId", player.getId());
        payload.put("price", price);

        try {
            notification.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException("Error al crear payload de notificación", e);
        }

        notificationRepository.save(notification);
    }
}
