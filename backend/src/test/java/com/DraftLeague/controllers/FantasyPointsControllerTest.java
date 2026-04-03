package com.DraftLeague.controllers;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Team.TeamGameweekPoints;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.*;
import com.DraftLeague.services.FantasyPointsService;
import com.DraftLeague.services.GameweekStateService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FantasyPointsController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FantasyPointsController — Web MVC slice tests")
class FantasyPointsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserRepository userRepository;
    @MockBean private JwtService jwtService;

    @MockBean private FantasyPointsService fantasyPointsService;
    @MockBean private GameweekStateService gameweekStateService;
    @MockBean private TeamRepository teamRepository;
    @MockBean private TeamGameweekPointsRepository gwPointsRepository;
    @MockBean private LeagueRepository leagueRepository;
    @MockBean private PlayerRepository playerRepository;
    @MockBean private PlayerStatisticRepository statisticRepository;
    @MockBean private MatchRepository matchRepository;
    @MockBean private TeamPlayerGameweekPointsRepository tpgwPointsRepository;


    @Test
    @DisplayName("GET /gameweek/status → 200 con activeGameweek y teamsLocked")
    void getGameweekStatus_returns200() throws Exception {
        when(gameweekStateService.getActiveGameweek()).thenReturn(5);
        when(gameweekStateService.isTeamsLocked()).thenReturn(false);

        mockMvc.perform(get("/api/v1/fantasy-points/gameweek/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeGameweek").value(5))
                .andExpect(jsonPath("$.teamsLocked").value(false));
    }


    @Test
    @DisplayName("GET /leagues/1/gameweek/3/ranking → 200 con lista vacía (sin equipos)")
    void getGameweekRanking_noTeams_returns200() throws Exception {
        League league = buildLeague(1L);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(teamRepository.findByLeague(any(League.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/fantasy-points/leagues/1/gameweek/3/ranking"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /teams/1/history → 200 con historial de puntos")
    void getTeamPointsHistory_returns200() throws Exception {
        Team team = buildTeamWithUser(1);
        when(teamRepository.findById(1)).thenReturn(Optional.of(team));
        when(gwPointsRepository.findByTeamOrderByGameweekAsc(any(Team.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/fantasy-points/teams/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(1));
    }


    @Test
    @DisplayName("GET /teams/1/gameweek/99/breakdown — jornada no encontrada → 404")
    void getPointsBreakdown_notFound_returns404() throws Exception {
        Team team = buildTeamWithUser(1);
        when(teamRepository.findById(1)).thenReturn(Optional.of(team));
        when(gwPointsRepository.findByTeamAndGameweek(any(Team.class), eq(99)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/fantasy-points/teams/1/gameweek/99/breakdown"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /teams/1/gameweek/3/breakdown — jornada existente → 200")
    void getPointsBreakdown_found_returns200() throws Exception {
        Team team = buildTeamWithUser(1);
        TeamGameweekPoints gw = buildGwPoints(team, 3, 65);

        when(teamRepository.findById(1)).thenReturn(Optional.of(team));
        when(gwPointsRepository.findByTeamAndGameweek(any(Team.class), eq(3)))
                .thenReturn(Optional.of(gw));
        when(playerRepository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/fantasy-points/teams/1/gameweek/3/breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(65));
    }


    @Test
    @DisplayName("POST /gameweek/3/recalculate — ADMIN → 200 con mensaje")
    void recalculateGameweek_admin_returns200() throws Exception {
        doNothing().when(fantasyPointsService).recalculateGameweekPoints(3);

        mockMvc.perform(post("/api/v1/fantasy-points/gameweek/3/recalculate")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Gameweek 3 recalculated successfully"));
    }


    private League buildLeague(Long id) {
        League l = new League();
        l.setId(Math.toIntExact(id));
        l.setName("Liga Test");
        return l;
    }

    private Team buildTeamWithUser(int id) {
        User u = new User();
        u.setId(1);
        u.setUsername("alice");
        u.setDisplayName("Alice");

        Team t = new Team();
        t.setId(id);
        t.setUser(u);
        t.setTotalPoints(100);
        t.setBudget(5_000_000);
        return t;
    }

    private TeamGameweekPoints buildGwPoints(Team team, int gameweek, int points) {
        TeamGameweekPoints gw = new TeamGameweekPoints();
        gw.setTeam(team);
        gw.setGameweek(gameweek);
        gw.setPoints(points);
        gw.setGoalkeeperPoints(10);
        gw.setDefenderPoints(20);
        gw.setMidfielderPoints(20);
        gw.setForwardPoints(15);
        gw.setCaptainBonus(0);
        return gw;
    }
}
