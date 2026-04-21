package com.DraftLeague.controllers;

import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.FantasyPointsService;
import com.DraftLeague.services.GameweekStateService;
import com.DraftLeague.services.LeagueService;
import com.DraftLeague.services.MarketService;
import com.DraftLeague.services.MarketValueUpdateService;
import com.DraftLeague.services.MatchService;
import com.DraftLeague.services.PlayerImportService;
import com.DraftLeague.services.PlayerStatisticsService;
import com.DraftLeague.services.UserService;
import com.DraftLeague.services.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice tests for AdminController.
 *
 * Strategy: addFilters=false removes the JWT filter chain. The controller's own
 * isAdmin() helper is tested directly by controlling the Authentication principal
 * via SecurityMockMvcRequestPostProcessors.user().
 *
 * The endpoint under test is GET /api/v1/admin/stats — chosen for simplicity;
 * all other endpoints use the same isAdmin() guard so the security behavior is
 * equivalent.
 */
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminController — Web MVC slice tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;


    @MockBean
    private LeagueRepository leagueRepository;

    @MockBean
    private PlayerRepository playerRepository;

    @MockBean
    private MarketService marketService;

    @MockBean
    private PlayerImportService importService;

    @MockBean
    private UserService userService;

    @MockBean
    private MatchService matchService;

    @MockBean
    private GameweekStateService gameweekStateService;

    @MockBean
    private FantasyPointsService fantasyPointsService;

    @MockBean
    private MarketValueUpdateService marketValueUpdateService;

    @MockBean
    private LeagueService leagueService;

    @MockBean
    private PlayerStatisticsService playerStatisticsService;


    @Test
    @DisplayName("sin autenticación → isAdmin() devuelve false → 403")
    void stats_noAuthentication_returns403() throws Exception {
        // No principal set → Authentication is null → isAdmin() returns false
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER → isAdmin() devuelve false → 403")
    void stats_roleUser_returns403() throws Exception {
        User regularUser = buildUser("regularuser", "USER");
        when(userRepository.findUserByUsername("regularuser"))
                .thenReturn(Optional.of(regularUser));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("regularuser", null, Collections.emptyList());

        mockMvc.perform(get("/api/v1/admin/stats")
                        .with(request -> { request.setUserPrincipal(auth); return request; }))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN → isAdmin() devuelve true → 200 con estadísticas")
    void stats_roleAdmin_returns200WithStats() throws Exception {
        User admin = buildUser("admin", "ADMIN");
        when(userRepository.findUserByUsername("admin"))
                .thenReturn(Optional.of(admin));
        when(userRepository.count()).thenReturn(5L);
        when(leagueRepository.count()).thenReturn(2L);
        when(playerRepository.count()).thenReturn(100L);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin", null, Collections.emptyList());

        mockMvc.perform(get("/api/v1/admin/stats")
                        .with(request -> { request.setUserPrincipal(auth); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(5))
                .andExpect(jsonPath("$.totalLeagues").value(2))
                .andExpect(jsonPath("$.totalPlayers").value(100));
    }


    private User buildUser(String username, String role) {
        User u = new User();
        u.setId(1);
        u.setUsername(username);
        u.setEmail(username + "@mail.com");
        u.setPassword("$2a$encoded");
        u.setDisplayName(username);
        u.setRole(role);
        return u;
    }
}
