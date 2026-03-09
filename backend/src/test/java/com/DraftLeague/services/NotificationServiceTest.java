package com.DraftLeague.services;

import com.DraftLeague.models.Notification.Notification;
import com.DraftLeague.models.Notification.NotificationLeague;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.NotificationLeagueRepository;
import com.DraftLeague.repositories.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationLeagueRepository notificationLeagueRepository;
    @Mock private LeagueRepository leagueRepository;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationService notificationService;

    // ─── getNotificationsByLeague ─────────────────────────────────────────────────

    @Test
    @DisplayName("getNotificationsByLeague: devuelve todas las notificaciones de la liga ordenadas")
    void getNotificationsByLeague_returnsAll() {
        Notification n1 = buildNotification(1);
        Notification n2 = buildNotification(2);

        when(notificationRepository.findByLeagueIdOrderByCreatedAtDesc(1))
                .thenReturn(List.of(n2, n1)); // newest first

        List<Notification> result = notificationService.getNotificationsByLeague(1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2);
    }

    @Test
    @DisplayName("getNotificationsByLeague: liga sin notificaciones → lista vacía")
    void getNotificationsByLeague_noNotifications_returnsEmptyList() {
        when(notificationRepository.findByLeagueIdOrderByCreatedAtDesc(99))
                .thenReturn(List.of());

        List<Notification> result = notificationService.getNotificationsByLeague(99);

        assertThat(result).isEmpty();
    }

    // ─── getNewNotifications ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getNewNotifications: con lastId → devuelve solo notificaciones más nuevas")
    void getNewNotifications_withLastId_returnsOnlyNewer() {
        Notification n3 = buildNotification(3);

        when(notificationRepository.findNewNotificationsByLeagueId(1, 2))
                .thenReturn(List.of(n3));

        List<Notification> result = notificationService.getNewNotifications(1, 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(3);
    }

    @Test
    @DisplayName("getNewNotifications: sin lastId (null) → delega en repositorio con null")
    void getNewNotifications_noLastId_delegatesToRepository() {
        when(notificationRepository.findNewNotificationsByLeagueId(1, null))
                .thenReturn(List.of(buildNotification(1)));

        List<Notification> result = notificationService.getNewNotifications(1, null);

        assertThat(result).hasSize(1);
        verify(notificationRepository).findNewNotificationsByLeagueId(1, null);
    }

    // ─── createClauseNotification ────────────────────────────────────────────────

    @Test
    @DisplayName("createClauseNotification: crea una notificación de tipo CLAUSE y la persiste")
    void createClauseNotification_savesNotification() {
        NotificationLeague nl = new NotificationLeague();
        nl.setId(1);

        com.DraftLeague.models.user.User buyer = buildUser(1, "alice");
        com.DraftLeague.models.user.User seller = buildUser(2, "bob");
        com.DraftLeague.models.Player.Player player = buildPlayer("P1");

        when(notificationLeagueRepository.findByLeagueId(1)).thenReturn(Optional.of(nl));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.createClauseNotification(1, buyer, seller, player, 500_000);

        verify(notificationRepository).save(any(Notification.class));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private Notification buildNotification(int id) {
        Notification n = new Notification();
        n.setId(id);
        n.setCreatedAt(new Date());
        n.setPayload("{}");
        return n;
    }

    private com.DraftLeague.models.user.User buildUser(int id, String username) {
        com.DraftLeague.models.user.User u = new com.DraftLeague.models.user.User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@mail.com");
        u.setPassword("encoded");
        u.setDisplayName(username);
        u.setRole("USER");
        return u;
    }

    private com.DraftLeague.models.Player.Player buildPlayer(String id) {
        com.DraftLeague.models.Player.Player p = new com.DraftLeague.models.Player.Player();
        p.setId(id);
        p.setFullName("Player " + id);
        p.setPosition(com.DraftLeague.models.Player.Position.DEL);
        p.setMarketValue(1_000_000);
        return p;
    }
}
