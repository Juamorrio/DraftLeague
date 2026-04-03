package com.DraftLeague.controllers;

import com.DraftLeague.dto.CreatePlayerStatisticRequest;
import com.DraftLeague.dto.PlayerStatisticsSummaryDTO;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.PlayerStatisticService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerStatisticController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PlayerStatisticController — Web MVC slice tests")
class PlayerStatisticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PlayerStatisticService playerStatisticService;


    @Test
    @DisplayName("GET /api/statistics/player/P1 → 200 con lista de estadísticas")
    void getPlayerStatistics_returns200() throws Exception {
        when(playerStatisticService.getPlayerStatistics("P1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/statistics/player/P1"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /api/statistics/match/1 → 200 con estadísticas del partido")
    void getMatchStatistics_returns200() throws Exception {
        when(playerStatisticService.getMatchStatistics(1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/statistics/match/1"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /api/statistics/player/P1/summary — resumen existente → 200")
    void getPlayerStatisticsSummary_found_returns200() throws Exception {
        PlayerStatisticsSummaryDTO summary = new PlayerStatisticsSummaryDTO();
        summary.setPlayerId("P1");
        summary.setMatchesPlayed(5);
        when(playerStatisticService.getPlayerStatisticsSummary("P1")).thenReturn(summary);

        mockMvc.perform(get("/api/statistics/player/P1/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/statistics/player/P1/summary — resumen nulo → 404")
    void getPlayerStatisticsSummary_notFound_returns404() throws Exception {
        when(playerStatisticService.getPlayerStatisticsSummary("P1")).thenReturn(null);

        mockMvc.perform(get("/api/statistics/player/P1/summary"))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("POST /api/statistics — body válido → 201 con estadística creada")
    void createStatistic_returns201() throws Exception {
        PlayerStatistic stat = new PlayerStatistic();
        stat.setPlayerId("P1");
        when(playerStatisticService.saveStatistic(any())).thenReturn(stat);

        CreatePlayerStatisticRequest req = new CreatePlayerStatisticRequest();
        req.setPlayerId("P1");
        req.setMatchId(1);
        req.setPlayerType(PlayerStatistic.PlayerType.MIDFIELDER);
        req.setMinutesPlayed(90);

        mockMvc.perform(post("/api/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }


    @Test
    @DisplayName("GET /api/statistics/player/P1/matches → 200 con lista de jornadas")
    void getPlayerMatchesSummary_returns200() throws Exception {
        when(playerStatisticService.getPlayerMatchesSummary("P1", null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/statistics/player/P1/matches"))
                .andExpect(status().isOk());
    }
}
