package com.DraftLeague.controllers;

import com.DraftLeague.dto.CreatePlayerRequest;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Player.Position;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.PlayerService;
import com.DraftLeague.services.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PlayerController — Web MVC slice tests")
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PlayerService playerService;


    @Test
    @DisplayName("GET /api/v1/players — sin leagueId → 200 con todos los jugadores")
    void getAllPlayers_noLeagueId_returns200() throws Exception {
        when(playerService.getAllPlayers()).thenReturn(List.of(buildPlayer("P1")));

        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("POST /api/v1/players → 200 con jugador creado")
    void createPlayer_returns200() throws Exception {
        Player player = buildPlayer("P1");
        when(playerService.createPlayer(any())).thenReturn(player);

        CreatePlayerRequest req = new CreatePlayerRequest();
        req.setId("P1");
        req.setFullName("Alice");
        req.setPosition(Position.MID);
        req.setClubId(541);
        req.setMarketValue(8_000_000);

        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /api/v1/players/P1 → 200 con jugador")
    void getPlayerById_returns200() throws Exception {
        when(playerService.getPlayerById("P1")).thenReturn(buildPlayer("P1"));

        mockMvc.perform(get("/api/v1/players/P1"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("DELETE /api/v1/players/P1 → 204")
    void deletePlayer_returns204() throws Exception {
        doNothing().when(playerService).deletePlayer("P1");

        mockMvc.perform(delete("/api/v1/players/P1"))
                .andExpect(status().isNoContent());
    }


    @Test
    @DisplayName("GET /api/v1/players/P1/market-value-history → 200 con historial")
    void getMarketValueHistory_returns200() throws Exception {
        when(playerService.getMarketValueHistory("P1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/players/P1/market-value-history"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/players/P1/market-value-history — excepción → 500")
    void getMarketValueHistory_exception_returns500() throws Exception {
        when(playerService.getMarketValueHistory("P1")).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/v1/players/P1/market-value-history"))
                .andExpect(status().isInternalServerError());
    }


    private Player buildPlayer(String id) {
        Player p = new Player();
        p.setId(id);
        p.setFullName("Player " + id);
        p.setPosition(Position.MID);
        p.setClubId(541);
        p.setMarketValue(8_000_000);
        p.setActive(true);
        p.setTotalPoints(0);
        return p;
    }
}
