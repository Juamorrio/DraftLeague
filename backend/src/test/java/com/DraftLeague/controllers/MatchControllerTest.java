package com.DraftLeague.controllers;

import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.MatchService;
import com.DraftLeague.services.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MatchController — Web MVC slice tests")
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MatchService matchService;


    @Test
    @DisplayName("GET /api/v1/matches/played → 200 con mapa de jornadas")
    void getPlayedMatches_returns200() throws Exception {
        when(matchService.getPlayedMatchesFromDB()).thenReturn(Map.of("jornada_1", List.of()));

        mockMvc.perform(get("/api/v1/matches/played"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /api/v1/matches/upcoming → 200 con mapa de jornadas")
    void getUpcomingMatches_returns200() throws Exception {
        when(matchService.getUpcomingMatchesFromDB()).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/v1/matches/upcoming"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /api/v1/matches/all → 200 con lista de partidos")
    void getAllMatches_returns200() throws Exception {
        when(matchService.getAllMatches()).thenReturn(List.of(buildMatch(1)));

        mockMvc.perform(get("/api/v1/matches/all"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /api/v1/matches/12345 — partido encontrado → 200")
    void getMatchByFixtureId_found_returns200() throws Exception {
        Match match = buildMatch(1);
        when(matchService.getMatchByFixtureId(12345)).thenReturn(match);

        mockMvc.perform(get("/api/v1/matches/12345"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/matches/99999 — partido no encontrado → 404")
    void getMatchByFixtureId_notFound_returns404() throws Exception {
        when(matchService.getMatchByFixtureId(99999)).thenReturn(null);

        mockMvc.perform(get("/api/v1/matches/99999"))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("POST /api/v1/matches/import → 200 con mensaje de éxito")
    void importMatches_returns200() throws Exception {
        when(matchService.importMatchesFromJson()).thenReturn("Imported 10 matches successfully");

        mockMvc.perform(post("/api/v1/matches/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Imported 10 matches successfully"));
    }


    private Match buildMatch(int id) {
        Match m = new Match();
        m.setId(id);
        m.setRound(3);
        m.setHomeTeamId(541);
        m.setAwayTeamId(529);
        m.setHomeClub("Real Madrid");
        m.setAwayClub("Barcelona");
        m.setHomeGoals(2);
        m.setAwayGoals(1);
        m.setStatus(MatchStatus.FINISHED);
        return m;
    }
}
