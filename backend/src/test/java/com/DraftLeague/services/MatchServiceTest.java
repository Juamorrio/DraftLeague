package com.DraftLeague.services;

import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.scraping.FixtureSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService Unit Tests")
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private FixtureSyncService fixtureSyncService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MatchService matchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(matchService, "scriptsPath", "/tmp");
    }


    @Test
    @DisplayName("getAllMatches: delega en matchRepository.findAll()")
    void getAllMatches_delegatesToRepository() {
        List<Match> matches = List.of(buildMatch(1, 3, 541, 529, MatchStatus.FINISHED));
        when(matchRepository.findAll()).thenReturn(matches);

        List<Match> result = matchService.getAllMatches();

        assertThat(result).hasSize(1);
        verify(matchRepository).findAll();
    }


    @Test
    @DisplayName("getMatchById: partido existe → devuelve el partido")
    void getMatchById_existingMatch_returnsMatch() {
        Match match = buildMatch(1, 3, 541, 529, MatchStatus.FINISHED);
        when(matchRepository.findById(1)).thenReturn(Optional.of(match));

        Match result = matchService.getMatchById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getMatchById: partido no existe → devuelve null")
    void getMatchById_notFound_returnsNull() {
        when(matchRepository.findById(99)).thenReturn(Optional.empty());

        Match result = matchService.getMatchById(99);

        assertThat(result).isNull();
    }


    @Test
    @DisplayName("getMatchByFixtureId: fixture existe → devuelve el partido")
    void getMatchByFixtureId_found_returnsMatch() {
        Match match = buildMatch(1, 3, 541, 529, MatchStatus.FINISHED);
        match.setApiFootballFixtureId(999);
        when(matchRepository.findByApiFootballFixtureId(999)).thenReturn(Optional.of(match));

        Match result = matchService.getMatchByFixtureId(999);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getMatchByFixtureId: fixture no existe → devuelve null")
    void getMatchByFixtureId_notFound_returnsNull() {
        when(matchRepository.findByApiFootballFixtureId(999)).thenReturn(Optional.empty());

        Match result = matchService.getMatchByFixtureId(999);

        assertThat(result).isNull();
    }


    @Test
    @DisplayName("getNextRound: hay partidos UPCOMING → devuelve la ronda mínima")
    void getNextRound_withUpcomingMatches_returnsMinRound() {
        Match m5 = buildMatch(5, 5, 541, 529, MatchStatus.UPCOMING);
        Match m6 = buildMatch(6, 6, 529, 541, MatchStatus.UPCOMING);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(m5, m6));

        Integer result = matchService.getNextRound();

        assertThat(result).isEqualTo(5);
    }

    @Test
    @DisplayName("getNextRound: sin partidos UPCOMING → devuelve null")
    void getNextRound_noUpcomingMatches_returnsNull() {
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of());

        Integer result = matchService.getNextRound();

        assertThat(result).isNull();
    }


    @Test
    @DisplayName("getNextRoundMatches: devuelve partidos de la siguiente ronda")
    void getNextRoundMatches_returnsMatchesForNextRound() {
        Match m = buildMatch(1, 5, 541, 529, MatchStatus.UPCOMING);
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of(m));
        when(matchRepository.findByRound(5)).thenReturn(List.of(m));

        List<Match> result = matchService.getNextRoundMatches();

        assertThat(result).hasSize(1);
        verify(matchRepository).findByRound(5);
    }

    @Test
    @DisplayName("getNextRoundMatches: sin próxima ronda → lista vacía")
    void getNextRoundMatches_noNextRound_returnsEmptyList() {
        when(matchRepository.findByStatusOrderByRoundAsc(MatchStatus.UPCOMING)).thenReturn(List.of());

        List<Match> result = matchService.getNextRoundMatches();

        assertThat(result).isEmpty();
        verify(matchRepository, never()).findByRound(any());
    }


    @Test
    @DisplayName("getNextMatchForClub: partido como local con ronda menor → devuelve ese partido")
    void getNextMatchForClub_homeMatchHasLowerRound_returnsHomeMatch() {
        Match home = buildMatch(1, 4, 541, 529, MatchStatus.UPCOMING);
        Match away = buildMatch(2, 6, 529, 541, MatchStatus.UPCOMING);

        when(matchRepository.findFirstByStatusAndHomeTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.of(home));
        when(matchRepository.findFirstByStatusAndAwayTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.of(away));

        Match result = matchService.getNextMatchForClub(541);

        assertThat(result).isEqualTo(home);
    }

    @Test
    @DisplayName("getNextMatchForClub: clubId null → devuelve null")
    void getNextMatchForClub_nullClubId_returnsNull() {
        Match result = matchService.getNextMatchForClub(null);

        assertThat(result).isNull();
        verifyNoInteractions(matchRepository);
    }


    private Match buildMatch(int id, int round, int homeTeamId, int awayTeamId, MatchStatus status) {
        Match m = new Match();
        m.setId(id);
        m.setRound(round);
        m.setHomeTeamId(homeTeamId);
        m.setAwayTeamId(awayTeamId);
        m.setHomeGoals(1);
        m.setAwayGoals(0);
        m.setHomeClub("Home FC");
        m.setAwayClub("Away FC");
        m.setStatus(status);
        return m;
    }
}
