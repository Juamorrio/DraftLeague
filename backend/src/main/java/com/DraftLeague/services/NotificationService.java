package com.DraftLeague.services;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Notification.Notification;
import com.DraftLeague.models.Notification.NotificationLeague;
import com.DraftLeague.models.Notification.NotificationType;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.NotificationLeagueRepository;
import com.DraftLeague.repositories.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationLeagueRepository notificationLeagueRepository;
    private final LeagueRepository leagueRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                                NotificationLeagueRepository notificationLeagueRepository,
                                LeagueRepository leagueRepository,
                                ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationLeagueRepository = notificationLeagueRepository;
        this.leagueRepository = leagueRepository;
        this.objectMapper = objectMapper;
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private NotificationLeague findOrCreateNotificationLeague(Integer leagueId) {
        return notificationLeagueRepository.findByLeagueId(leagueId)
                .orElseGet(() -> {
                    League league = leagueRepository.findById(Long.valueOf(leagueId))
                            .orElseThrow(() -> new RuntimeException("Liga no encontrada"));
                    NotificationLeague nl = new NotificationLeague();
                    nl.setLeague(league);
                    return notificationLeagueRepository.save(nl);
                });
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear payload de notificación", e);
        }
    }

    // ─── Create methods ───────────────────────────────────────────────────────────

    @Transactional
    public void createClauseNotification(Integer leagueId, User buyer, User seller, Player player, int price) {
        NotificationLeague notificationLeague = findOrCreateNotificationLeague(leagueId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("buyerUsername", buyer.getUsername());
        payload.put("buyerId", buyer.getId());
        payload.put("sellerUsername", seller.getUsername());
        payload.put("sellerId", seller.getId());
        payload.put("playerName", player.getFullName());
        payload.put("playerId", player.getId());
        payload.put("price", price);

        Notification notification = new Notification();
        notification.setType(NotificationType.CLAUSE);
        notification.setCreatedAt(new Date());
        notification.setNotificationLeague(notificationLeague);
        notification.setPayload(toJson(payload));
        notificationRepository.save(notification);
    }

    @Transactional
    public void createMarketBuyNotification(Integer leagueId, User buyer, Player player, long price) {
        NotificationLeague notificationLeague = findOrCreateNotificationLeague(leagueId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("buyerUsername", buyer.getUsername());
        payload.put("buyerId", buyer.getId());
        payload.put("playerName", player.getFullName());
        payload.put("playerId", player.getId());
        payload.put("price", price);

        Notification notification = new Notification();
        notification.setType(NotificationType.BUY);
        notification.setCreatedAt(new Date());
        notification.setNotificationLeague(notificationLeague);
        notification.setPayload(toJson(payload));
        notificationRepository.save(notification);
    }

    @Transactional
    public void createTradeOfferNotification(Integer leagueId, User buyer, User seller, Player player, Integer offerPrice, Long offerId) {
        NotificationLeague notificationLeague = findOrCreateNotificationLeague(leagueId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("offerId", offerId);
        payload.put("buyerUsername", buyer.getUsername());
        payload.put("buyerId", buyer.getId());
        payload.put("sellerUsername", seller.getUsername());
        payload.put("sellerId", seller.getId());
        payload.put("playerName", player.getFullName());
        payload.put("playerId", player.getId());
        payload.put("price", offerPrice);

        Notification notification = new Notification();
        notification.setType(NotificationType.TRADE_OFFER);
        notification.setCreatedAt(new Date());
        notification.setNotificationLeague(notificationLeague);
        notification.setPayload(toJson(payload));
        notificationRepository.save(notification);
    }

    @Transactional
    public void createTradeResultNotification(Integer leagueId, User buyer, User seller, Player player, Integer price, Long offerId, boolean accepted) {
        NotificationLeague notificationLeague = findOrCreateNotificationLeague(leagueId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("offerId", offerId);
        payload.put("buyerUsername", buyer.getUsername());
        payload.put("buyerId", buyer.getId());
        payload.put("sellerUsername", seller.getUsername());
        payload.put("sellerId", seller.getId());
        payload.put("playerName", player.getFullName());
        payload.put("playerId", player.getId());
        payload.put("price", price);

        Notification notification = new Notification();
        notification.setType(accepted ? NotificationType.TRADE_ACCEPTED : NotificationType.TRADE_REJECTED);
        notification.setCreatedAt(new Date());
        notification.setNotificationLeague(notificationLeague);
        notification.setPayload(toJson(payload));
        notificationRepository.save(notification);
    }

    @Transactional
    public void createSellNotification(Integer leagueId, User seller, Player player, int price) {
        NotificationLeague notificationLeague = findOrCreateNotificationLeague(leagueId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sellerUsername", seller.getUsername());
        payload.put("sellerId", seller.getId());
        payload.put("playerName", player.getFullName());
        payload.put("playerId", player.getId());
        payload.put("price", price);

        Notification notification = new Notification();
        notification.setType(NotificationType.SELL);
        notification.setCreatedAt(new Date());
        notification.setNotificationLeague(notificationLeague);
        notification.setPayload(toJson(payload));
        notificationRepository.save(notification);
    }

    // ─── Query methods ────────────────────────────────────────────────────────────

    public List<Notification> getNotificationsByLeague(Integer leagueId) {
        return notificationRepository.findByLeagueIdOrderByCreatedAtDesc(leagueId);
    }

    public List<Notification> getNewNotifications(Integer leagueId, Integer lastId) {
        return notificationRepository.findNewNotificationsByLeagueId(leagueId, lastId);
    }
}
