package com.DraftLeague.services;

import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.MatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchAnalyticsService Unit Tests")
class MatchAnalyticsServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchAnalyticsService matchAnalyticsService;


    @Test
    @DisplayName("calculateMatchStats: sin histórico → devuelve MatchStats no nulo con probabilidades por defecto")
    void calculateMatchStats_noHistory_returnsDefaultProbs() {
        when(matchRepository.findAll()).thenReturn(Collections.emptyList());

        MatchAnalyticsService.MatchStats stats = matchAnalyticsService.calculateMatchStats(541, 529, 10);

        assertThat(stats).isNotNull();
        assertThat(stats.getWinProb()).isEqualTo(0.33);
        assertThat(stats.getDrawProb()).isEqualTo(0.33);
        assertThat(stats.getLossProb()).isEqualTo(0.34);
    }

    @Test
    @DisplayName("calculateMatchStats: con histórico de partidos → devuelve probs calculadas, no nulas")
    void calculateMatchStats_withHistory_returnsCalculatedProbs() {
        List<Match> history = buildHistory();
        when(matchRepository.findAll()).thenReturn(history);

        MatchAnalyticsService.MatchStats stats = matchAnalyticsService.calculateMatchStats(541, 529, 10);

        assertThat(stats).isNotNull();
        assertThat(stats.getWinProb()).isGreaterThanOrEqualTo(0.0);
        assertThat(stats.getDrawProb()).isGreaterThanOrEqualTo(0.0);
        assertThat(stats.getLossProb()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateMatchStats: con histórico → la suma de probabilidades es aproximadamente 1.0")
    void calculateMatchStats_probsSumToOne() {
        when(matchRepository.findAll()).thenReturn(buildHistory());

        MatchAnalyticsService.MatchStats stats = matchAnalyticsService.calculateMatchStats(541, 529, 10);

        double sum = stats.getWinProb() + stats.getDrawProb() + stats.getLossProb();
        assertThat(sum).isBetween(0.9, 1.1);
    }

    @Test
    @DisplayName("calculateMatchStats: equipo que siempre gana → teamElo > 1500 (ELO inicial)")
    void calculateMatchStats_teamAlwaysWins_teamEloAboveInitial() {
        // equipo 541 gana todos los partidos contra equipo 529
        List<Match> history = List.of(
                buildFinished(1, 541, 529, 3, 0),
                buildFinished(2, 541, 529, 2, 0),
                buildFinished(3, 541, 529, 1, 0)
        );
        when(matchRepository.findAll()).thenReturn(history);

        MatchAnalyticsService.MatchStats stats = matchAnalyticsService.calculateMatchStats(541, 529, 10);

        assertThat(stats.getTeamElo()).isGreaterThan(1500.0);
    }

    @Test
    @DisplayName("calculateMatchStats: todos los campos del MatchStats son no nulos")
    void calculateMatchStats_allFieldsAreNonNullValues() {
        when(matchRepository.findAll()).thenReturn(buildHistory());

        MatchAnalyticsService.MatchStats stats = matchAnalyticsService.calculateMatchStats(541, 529, 10);

        assertThat(stats.getTeamElo()).isNotNull();
        assertThat(stats.getOpponentElo()).isNotNull();
        assertThat(stats.getWinProb()).isNotNull();
        assertThat(stats.getDrawProb()).isNotNull();
        assertThat(stats.getLossProb()).isNotNull();
    }


    private List<Match> buildHistory() {
        return List.of(
                buildFinished(1, 541, 529, 2, 1),
                buildFinished(2, 529, 541, 0, 1),
                buildFinished(3, 541, 529, 1, 1),
                buildFinished(4, 529, 541, 2, 0)
        );
    }

    private Match buildFinished(int round, int homeId, int awayId, int homeGoals, int awayGoals) {
        Match m = new Match();
        m.setId(round);
        m.setRound(round);
        m.setHomeTeamId(homeId);
        m.setAwayTeamId(awayId);
        m.setHomeGoals(homeGoals);
        m.setAwayGoals(awayGoals);
        m.setStatus(MatchStatus.FINISHED);
        m.setHomeClub("Home FC");
        m.setAwayClub("Away FC");
        return m;
    }
}
