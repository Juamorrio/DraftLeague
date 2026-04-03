package com.DraftLeague.controllers;

import com.DraftLeague.dto.CreateTeamRequest;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.TeamService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TeamController — Web MVC slice tests")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TeamService teamService;


    @Test
    @DisplayName("GET /api/v1/teams/1 → 200 con equipo")
    void getTeamById_returns200() throws Exception {
        when(teamService.getTeamById(1)).thenReturn(buildTeam(1, 10_000_000L));

        mockMvc.perform(get("/api/v1/teams/1"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("POST /api/v1/teams → 200 con equipo creado")
    void createTeam_returns200() throws Exception {
        when(teamService.postTeam(any())).thenReturn(buildTeam(1, 10_000_000L));

        CreateTeamRequest req = new CreateTeamRequest();
        req.setUserId(1);
        req.setLeagueId(1L);
        req.setBudget(5_000_000);
        req.setWildcardUsed(false);

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("POST /api/v1/teams/1/update → 200 con equipo actualizado")
    void updateTeam_returns200() throws Exception {
        when(teamService.updateTeam(any(), eq(1))).thenReturn(buildTeam(1, 9_000_000L));

        mockMvc.perform(post("/api/v1/teams/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /api/v1/teams/league/1/1 — equipo encontrado → 200")
    void getTeamByLeague_found_returns200() throws Exception {
        when(teamService.getTeamByUserAndLeague(1, 1)).thenReturn(buildTeam(1, 10_000_000L));

        mockMvc.perform(get("/api/v1/teams/league/1/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/teams/league/1/99 — equipo no encontrado → 404")
    void getTeamByLeague_notFound_returns404() throws Exception {
        when(teamService.getTeamByUserAndLeague(1, 99)).thenThrow(new RuntimeException("Equipo no encontrado"));

        mockMvc.perform(get("/api/v1/teams/league/1/99"))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("POST /api/v1/teams/league/1/buyout — parámetros válidos → 200")
    void buyoutPlayer_validParams_returns200() throws Exception {
        when(teamService.buyoutPlayer(1, 2, "P1")).thenReturn(buildTeam(5, 8_000_000L));

        String body = objectMapper.writeValueAsString(Map.of("sellerUserId", 2, "playerId", "P1"));

        mockMvc.perform(post("/api/v1/teams/league/1/buyout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget").value(8000000));
    }

    @Test
    @DisplayName("POST /api/v1/teams/league/1/buyout — parámetros inválidos (sin playerId) → 400")
    void buyoutPlayer_missingPlayerId_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("sellerUserId", 2));

        mockMvc.perform(post("/api/v1/teams/league/1/buyout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("POST /api/v1/teams/league/1/1/chip — chip válido → 200")
    void activateChip_validChip_returns200() throws Exception {
        Team team = buildTeam(1, 10_000_000L);
        team.setActiveChip("TRIPLE_CAP");
        when(teamService.activateChip(1, 1, "TRIPLE_CAP")).thenReturn(team);

        String body = objectMapper.writeValueAsString(Map.of("chip", "TRIPLE_CAP"));

        mockMvc.perform(post("/api/v1/teams/league/1/1/chip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeChip").value("TRIPLE_CAP"));
    }


    private Team buildTeam(int id, long budget) {
        Team t = new Team();
        t.setId(id);
        t.setBudget((int) budget);
        User u = new User();
        u.setId(1);
        u.setDisplayName("TestUser");
        t.setUser(u);
        t.setTotalPoints(0);
        return t;
    }
}
