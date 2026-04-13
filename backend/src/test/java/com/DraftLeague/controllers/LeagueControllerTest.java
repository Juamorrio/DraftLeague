package com.DraftLeague.controllers;

import com.DraftLeague.dto.CreateLeagueRequest;
import com.DraftLeague.models.League.League;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.LeagueService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeagueController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LeagueController — Web MVC slice tests")
class LeagueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private LeagueService leagueService;


    @Test
    @DisplayName("POST /api/v1/leagues — petición válida → 200 con liga creada")
    void createLeague_validRequest_returns200() throws Exception {
        League league = buildLeague(1, "Liga Test", "ABC123");
        when(leagueService.createLeague(any())).thenReturn(league);

        CreateLeagueRequest req = new CreateLeagueRequest();
        req.setName("Liga Test");
        req.setMaxTeams(8);
        req.setInitialBudget(50_000_000);
        req.setMarketEndHour("20:00");
        req.setCaptainEnable(true);

        mockMvc.perform(post("/api/v1/leagues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Liga Test"));
    }


    @Test
    @DisplayName("GET /api/v1/leagues/1 → 200 con liga")
    void getLeagueById_returns200() throws Exception {
        League league = buildLeague(1, "Liga Test", "ABC123");
        when(leagueService.getLeagueById(1L)).thenReturn(league);

        mockMvc.perform(get("/api/v1/leagues/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Liga Test"));
    }


    @Test
    @DisplayName("GET /api/v1/leagues → 200 con lista de ligas")
    void getAllLeagues_returns200() throws Exception {
        when(leagueService.getAllLeagues()).thenReturn(List.of(buildLeague(1, "Liga", "CODE")));

        mockMvc.perform(get("/api/v1/leagues"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("PUT /api/v1/leagues/1 → 200 con liga actualizada")
    void updateLeague_returns200() throws Exception {
        League league = buildLeague(1, "Liga Actualizada", "ABC123");
        when(leagueService.updateLeague(eq(1L), any())).thenReturn(league);

        mockMvc.perform(put("/api/v1/leagues/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Liga Actualizada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Liga Actualizada"));
    }


    @Test
    @DisplayName("DELETE /api/v1/leagues/1 → 204")
    void deleteLeague_returns204() throws Exception {
        doNothing().when(leagueService).deleteLeague(1L);

        mockMvc.perform(delete("/api/v1/leagues/1"))
                .andExpect(status().isNoContent());
    }


    @Test
    @DisplayName("GET /api/v1/leagues/1/ranking → 200 con lista de ranking")
    void getRanking_returns200() throws Exception {
        when(leagueService.getRanking(1L)).thenReturn(List.of(
                Map.of("teamId", 1, "points", 120)
        ));

        mockMvc.perform(get("/api/v1/leagues/1/ranking"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("POST /api/v1/leagues/join — código válido → 200 con datos de la liga")
    void joinLeague_validCode_returns200() throws Exception {
        League league = buildLeague(1, "Liga Test", "ABC123");
        when(leagueService.joinLeagueByCode("ABC123")).thenReturn(league);

        mockMvc.perform(post("/api/v1/leagues/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABC123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABC123"));
    }

    @Test
    @DisplayName("POST /api/v1/leagues/join — código inválido → 400 con error")
    void joinLeague_invalidCode_returns400() throws Exception {
        when(leagueService.joinLeagueByCode("INVALID")).thenThrow(new RuntimeException("Liga no encontrada"));

        mockMvc.perform(post("/api/v1/leagues/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Liga no encontrada"));
    }


    private League buildLeague(int id, String name, String code) {
        League l = new League();
        l.setId(id);
        l.setName(name);
        l.setCode(code);
        return l;
    }
}
