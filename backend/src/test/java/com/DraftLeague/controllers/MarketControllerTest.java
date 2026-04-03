package com.DraftLeague.controllers;

import com.DraftLeague.dto.MarketPlayerDTO;
import com.DraftLeague.models.Market.StatusMarketPlayer;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.MarketService;
import com.DraftLeague.services.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MarketController — Web MVC slice tests")
class MarketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MarketService marketService;


    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("GET /api/v1/market?leagueId=1 — autenticado → 200 con lista de jugadores")
    void getMarketPlayers_authenticated_returns200() throws Exception {
        MarketPlayerDTO dto = new MarketPlayerDTO(1, null, null, StatusMarketPlayer.AVAILABLE, null, false);
        when(marketService.getAvailableMarketPlayersForUser(eq(1), eq("alice")))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/market").param("leagueId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/market?leagueId=1 — sin autenticación → 400")
    void getMarketPlayers_unauthenticated_returns400() throws Exception {
        // Sin principal, auth.getName() lanzaría NPE → el controlador devuelve badRequest
        mockMvc.perform(get("/api/v1/market").param("leagueId", "1"))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("POST /api/v1/market/bid — autenticado → 200 con mensaje")
    void placeBid_authenticated_returns200() throws Exception {
        doNothing().when(marketService).placeBid(eq(1), eq("alice"), eq(1_000_000L));

        mockMvc.perform(post("/api/v1/market/bid")
                        .param("marketPlayerId", "1")
                        .param("bidAmount", "1000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Puja realizada"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("POST /api/v1/market/bid — servicio lanza IllegalStateException → 400")
    void placeBid_serviceThrows_returns400() throws Exception {
        doThrow(new IllegalStateException("Fondos insuficientes"))
                .when(marketService).placeBid(anyInt(), anyString(), anyLong());

        mockMvc.perform(post("/api/v1/market/bid")
                        .param("marketPlayerId", "1")
                        .param("bidAmount", "999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fondos insuficientes"));
    }


    @Test
    @DisplayName("POST /api/v1/market/initialize?leagueId=1 → 200 con mensaje")
    void initializeMarket_returns200() throws Exception {
        doNothing().when(marketService).initializeMarket(1);

        mockMvc.perform(post("/api/v1/market/initialize").param("leagueId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mercado inicializado"));
    }


    @Test
    @DisplayName("POST /api/v1/market/refresh?leagueId=1 → 200 con mensaje")
    void refreshMarket_returns200() throws Exception {
        doNothing().when(marketService).refreshMarket(1);

        mockMvc.perform(post("/api/v1/market/refresh").param("leagueId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mercado refrescado exitosamente"));
    }


    @Test
    @DisplayName("POST /api/v1/market/finalize/1 → 200 con mensaje")
    void finalizeAuction_returns200() throws Exception {
        doNothing().when(marketService).finalizeAuction(1);

        mockMvc.perform(post("/api/v1/market/finalize/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subasta finalizada"));
    }


    @Test
    @DisplayName("DELETE /api/v1/market/cancel-bid?marketPlayerId=1 — autenticado → 200")
    void cancelBid_authenticated_returns200() throws Exception {
        User userEntity = new User();
        userEntity.setId(1);
        userEntity.setUsername("alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(userEntity));
        doNothing().when(marketService).cancelBid(eq(1), any(User.class));
        // cancelBid recibe Authentication como parámetro → setUserPrincipal en la request
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("alice", null, Collections.emptyList());

        mockMvc.perform(delete("/api/v1/market/cancel-bid")
                        .param("marketPlayerId", "1")
                        .with(request -> { request.setUserPrincipal(auth); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Puja cancelada correctamente"));
    }


    @Test
    @DisplayName("POST /api/v1/market/finalize-expired → 200 con mensaje")
    void finalizeExpiredAuctions_returns200() throws Exception {
        doNothing().when(marketService).finalizeExpiredAuctions();

        mockMvc.perform(post("/api/v1/market/finalize-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subastas expiradas finalizadas"));
    }
}
