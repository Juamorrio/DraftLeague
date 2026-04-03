package com.DraftLeague.controllers;

import com.DraftLeague.models.Trade.TradeOffer;
import com.DraftLeague.models.Trade.TradeOfferStatus;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.TradeOfferService;
import com.DraftLeague.services.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeOfferController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TradeOfferController — Web MVC slice tests")
class TradeOfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TradeOfferService tradeOfferService;


    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("POST /api/v1/trade-offers — autenticado → 200 con offerId y status")
    void createOffer_authenticated_returns200() throws Exception {
        User buyer = buildUser(1, "alice");
        TradeOffer offer = buildOffer(10L, TradeOfferStatus.PENDING);

        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(buyer));
        when(tradeOfferService.createOffer(any(), eq(2), eq("P1"), eq(5_000_000), eq(1))).thenReturn(offer);

        String body = objectMapper.writeValueAsString(
                Map.of("toTeamId", 2, "playerId", "P1", "offerPrice", 5_000_000, "leagueId", 1));

        mockMvc.perform(post("/api/v1/trade-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offerId").value(10));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("POST /api/v1/trade-offers — parámetros incompletos → 400")
    void createOffer_missingParams_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("playerId", "P1"));

        mockMvc.perform(post("/api/v1/trade-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("PUT /api/v1/trade-offers/10/accept → 200 con status ACCEPTED")
    void acceptOffer_returns200() throws Exception {
        User seller = buildUser(2, "bob");
        TradeOffer offer = buildOffer(10L, TradeOfferStatus.ACCEPTED);

        when(userRepository.findUserByUsername("bob")).thenReturn(Optional.of(seller));
        when(tradeOfferService.acceptOffer(eq(10L), any())).thenReturn(offer);

        mockMvc.perform(put("/api/v1/trade-offers/10/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }


    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("PUT /api/v1/trade-offers/10/reject → 200 con status REJECTED")
    void rejectOffer_returns200() throws Exception {
        User seller = buildUser(2, "bob");
        TradeOffer offer = buildOffer(10L, TradeOfferStatus.REJECTED);

        when(userRepository.findUserByUsername("bob")).thenReturn(Optional.of(seller));
        when(tradeOfferService.rejectOffer(eq(10L), any())).thenReturn(offer);

        mockMvc.perform(put("/api/v1/trade-offers/10/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }


    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("PUT /api/v1/trade-offers/10/cancel → 200 con status CANCELLED")
    void cancelOffer_returns200() throws Exception {
        User buyer = buildUser(1, "alice");
        TradeOffer offer = buildOffer(10L, TradeOfferStatus.CANCELLED);

        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(buyer));
        when(tradeOfferService.cancelOffer(eq(10L), any())).thenReturn(offer);

        mockMvc.perform(put("/api/v1/trade-offers/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }


    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("GET /api/v1/trade-offers/league/1/incoming → 200 con lista")
    void getIncomingOffers_returns200() throws Exception {
        User user = buildUser(1, "alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(tradeOfferService.getIncomingOffers(any(), eq(1))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/trade-offers/league/1/incoming"))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("GET /api/v1/trade-offers/league/1/outgoing → 200 con lista")
    void getOutgoingOffers_returns200() throws Exception {
        User user = buildUser(1, "alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(tradeOfferService.getOutgoingOffers(any(), eq(1))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/trade-offers/league/1/outgoing"))
                .andExpect(status().isOk());
    }


    private User buildUser(int id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private TradeOffer buildOffer(Long id, TradeOfferStatus status) {
        TradeOffer offer = new TradeOffer();
        offer.setId(id);
        offer.setStatus(status);
        return offer;
    }
}
