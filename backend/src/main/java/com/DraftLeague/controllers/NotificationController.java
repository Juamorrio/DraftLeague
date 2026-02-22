package com.DraftLeague.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Notification.Notification;
import com.DraftLeague.services.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @GetMapping("/league/{leagueId}")
    public ResponseEntity<List<Notification>> getNotificationsByLeague(
            @PathVariable Integer leagueId,
            Authentication auth) {
        
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        List<Notification> notifications = notificationService.getNotificationsByLeague(leagueId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/league/{leagueId}/new")
    public ResponseEntity<List<Notification>> getNewNotifications(
            @PathVariable Integer leagueId,
            @RequestParam(required = false, defaultValue = "0") Integer lastId,
            Authentication auth) {
        
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        List<Notification> notifications = notificationService.getNewNotifications(leagueId, lastId);
        return ResponseEntity.ok(notifications);
    }
}
