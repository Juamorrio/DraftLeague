package com.DraftLeague.controllers;

import com.DraftLeague.dto.PlayerPredictionDTO;
import com.DraftLeague.dto.TeamPredictionDTO;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.FantasyPointsService;
import com.DraftLeague.services.PlayerPredictionService;
import com.DraftLeague.services.XGBoostClient;
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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MLController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MLController — Web MVC slice tests")
class MLControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserRepository userRepository;
    @MockBean private JwtService jwtService;

    @MockBean private PlayerPredictionService playerPredictionService;
    @MockBean private FantasyPointsService fantasyPointsService;
    @MockBean private MatchRepository matchRepository;
    @MockBean private XGBoostClient xgBoostClient;


    @Test
    @DisplayName("GET /api/ml/predict/player/P1 → 200 con PlayerPredictionDTO")
    void predictPlayer_returns200() throws Exception {
        PlayerPredictionDTO dto = PlayerPredictionDTO.builder()
                .playerId("P1")
                .playerName("Alice")
                .position("MID")
                .playerType("MIDFIELDER")
                .predictedPoints(7.5)
                .confidenceInterval(List.of(5, 10))
                .featuresImportance(Collections.emptyMap())
                .build();
        when(playerPredictionService.predictForPlayer("P1")).thenReturn(dto);

        mockMvc.perform(get("/api/ml/predict/player/P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("P1"))
                .andExpect(jsonPath("$.predictedPoints").value(7.5));
    }


    @Test
    @DisplayName("GET /api/ml/predict/team/1 → 200 con TeamPredictionDTO")
    void predictTeam_returns200() throws Exception {
        TeamPredictionDTO dto = TeamPredictionDTO.builder()
                .teamId(1)
                .teamName("Alice's Team")
                .totalPredictedPoints(60.0)
                .confidenceInterval(List.of(42, 78))
                .players(Collections.emptyList())
                .build();
        when(playerPredictionService.predictForTeam(1)).thenReturn(dto);

        mockMvc.perform(get("/api/ml/predict/team/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(1))
                .andExpect(jsonPath("$.totalPredictedPoints").value(60.0));
    }


    @Test
    @DisplayName("GET /api/ml/next-round — con partidos → 200 con round y matches")
    void getNextRound_withMatches_returns200() throws Exception {
        Match m = buildMatch(1, 5, "Real Madrid", "Barcelona");
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(m));

        mockMvc.perform(get("/api/ml/next-round"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value(5));
    }

    @Test
    @DisplayName("GET /api/ml/next-round — sin partidos → 200 con round=0 y matches vacío")
    void getNextRound_noMatches_returns200WithDefaults() throws Exception {
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/ml/next-round"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value(0));
    }


    @Test
    @DisplayName("POST /api/ml/train — ADMIN → 200 con status training_triggered")
    void triggerRetraining_admin_returns200() throws Exception {
        doNothing().when(xgBoostClient).triggerRetraining(anyInt());
        doNothing().when(playerPredictionService).invalidateCache();

        mockMvc.perform(post("/api/ml/train")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("training_triggered"));
    }


    @Test
    @DisplayName("POST /api/ml/recalculate-fantasy-points → 200 con status recalculated")
    void recalculateFantasyPoints_returns200() throws Exception {
        doNothing().when(fantasyPointsService).updateAllPlayerPoints();
        doNothing().when(playerPredictionService).invalidateCache();

        mockMvc.perform(post("/api/ml/recalculate-fantasy-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("recalculated"));
    }


    private Match buildMatch(int id, int round, String homeClub, String awayClub) {
        Match m = new Match();
        m.setId(id);
        m.setRound(round);
        m.setHomeClub(homeClub);
        m.setAwayClub(awayClub);
        m.setHomeTeamId(541);
        m.setAwayTeamId(529);
        m.setHomeGoals(0);
        m.setAwayGoals(0);
        m.setStatus(MatchStatus.UPCOMING);
        return m;
    }
}
