package com.DraftLeague.controllers;

import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.NotificationService;
import com.DraftLeague.services.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationController — Web MVC slice tests")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private NotificationService notificationService;


    @Test
    @DisplayName("GET /api/v1/notifications/league/1 — autenticado → 200 con lista")
    void getNotificationsByLeague_authenticated_returns200() throws Exception {
        when(notificationService.getNotificationsByLeague(1)).thenReturn(Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("alice", null, Collections.emptyList());

        mockMvc.perform(get("/api/v1/notifications/league/1")
                        .with(request -> { request.setUserPrincipal(auth); return request; }))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/league/1 — sin autenticación → 401")
    void getNotificationsByLeague_unauthenticated_returns401() throws Exception {
        // Sin principal, auth == null → el controlador devuelve 401
        mockMvc.perform(get("/api/v1/notifications/league/1"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("GET /api/v1/notifications/league/1/new — autenticado → 200")
    void getNewNotifications_authenticated_returns200() throws Exception {
        when(notificationService.getNewNotifications(1, 0)).thenReturn(Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("alice", null, Collections.emptyList());

        mockMvc.perform(get("/api/v1/notifications/league/1/new")
                        .with(request -> { request.setUserPrincipal(auth); return request; }))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/league/1/new — sin autenticación → 401")
    void getNewNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/league/1/new"))
                .andExpect(status().isUnauthorized());
    }
}
