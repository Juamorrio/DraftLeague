package com.DraftLeague.services;

import com.DraftLeague.dto.MatchDTO;
import com.DraftLeague.dto.UpcomingMatchDTO;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.api.FixtureSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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


    // -------------------------------------------------------------------------
    // importMatchesFromData
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("importMatchesFromData: mapas vacíos → no guarda partidos")
    void importMatchesFromData_emptyMaps_savesNothing() {
        when(matchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = matchService.importMatchesFromData(Collections.emptyMap(), Collections.emptyMap());

        assertThat(result).contains("0");
        verify(matchRepository).saveAll(Collections.emptyList());
    }

    @Test
    @DisplayName("importMatchesFromData: partido ya existente por fixtureId → lo omite")
    void importMatchesFromData_duplicateFixtureId_skipsExisting() {
        MatchDTO dto = new MatchDTO(999, 541, 529, 2, 1, 1.5, 0.8, "Real Madrid", "Barcelona");
        Map<String, List<MatchDTO>> played = Map.of("jornada_5", List.of(dto));

        when(matchRepository.findByApiFootballFixtureId(999)).thenReturn(Optional.of(buildMatch(1, 5, 541, 529, MatchStatus.FINISHED)));
        when(matchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = matchService.importMatchesFromData(played, Collections.emptyMap());

        assertThat(result).contains("0");
    }

    @Test
    @DisplayName("importMatchesFromData: partido nuevo → lo importa con status FINISHED")
    void importMatchesFromData_newMatch_importsWithFinishedStatus() {
        MatchDTO dto = new MatchDTO(888, 541, 529, 2, 1, 1.5, 0.8, "Real Madrid", "Barcelona");
        Map<String, List<MatchDTO>> played = Map.of("jornada_3", List.of(dto));

        when(matchRepository.findByApiFootballFixtureId(888)).thenReturn(Optional.empty());
        when(matchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = matchService.importMatchesFromData(played, Collections.emptyMap());

        assertThat(result).contains("1");
        verify(matchRepository).saveAll(argThat(list -> {
            List<Match> matches = new ArrayList<>((List<Match>) list);
            return matches.size() == 1 && matches.get(0).getStatus() == MatchStatus.FINISHED;
        }));
    }

    @Test
    @DisplayName("importMatchesFromData: partido upcoming nuevo → lo importa con status UPCOMING")
    void importMatchesFromData_newUpcoming_importsWithUpcomingStatus() {
        UpcomingMatchDTO dto = new UpcomingMatchDTO(null, 541, 529, "2026-05-01", "Real Madrid", "Barcelona");
        Map<String, List<UpcomingMatchDTO>> upcoming = Map.of("jornada_38", List.of(dto));

        when(matchRepository.findByRoundAndHomeTeamIdAndAwayTeamId(38, 541, 529)).thenReturn(Optional.empty());
        when(matchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = matchService.importMatchesFromData(Collections.emptyMap(), upcoming);

        assertThat(result).contains("1");
        verify(matchRepository).saveAll(argThat(list -> {
            List<Match> matches = new ArrayList<>((List<Match>) list);
            return matches.size() == 1 && matches.get(0).getStatus() == MatchStatus.UPCOMING;
        }));
    }

    @Test
    @DisplayName("importMatchesFromData: upcoming ya existente → lo omite")
    void importMatchesFromData_duplicateUpcoming_skipsExisting() {
        UpcomingMatchDTO dto = new UpcomingMatchDTO(null, 541, 529, "2026-05-01", "Real Madrid", "Barcelona");
        Map<String, List<UpcomingMatchDTO>> upcoming = Map.of("jornada_38", List.of(dto));

        when(matchRepository.findByRoundAndHomeTeamIdAndAwayTeamId(38, 541, 529))
                .thenReturn(Optional.of(buildMatch(5, 38, 541, 529, MatchStatus.UPCOMING)));
        when(matchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = matchService.importMatchesFromData(Collections.emptyMap(), upcoming);

        assertThat(result).contains("0");
    }

    @Test
    @DisplayName("importMatchesFromData: homeTeamName null → usa cadena vacía")
    void importMatchesFromData_nullTeamName_usesEmptyString() {
        MatchDTO dto = new MatchDTO(777, 541, 529, 0, 0, null, null, null, null);
        Map<String, List<MatchDTO>> played = Map.of("jornada_1", List.of(dto));

        when(matchRepository.findByApiFootballFixtureId(777)).thenReturn(Optional.empty());
        when(matchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = matchService.importMatchesFromData(played, Collections.emptyMap());

        assertThat(result).contains("1");
        verify(matchRepository).saveAll(argThat(list -> {
            List<Match> matches = new ArrayList<>((List<Match>) list);
            Match m = matches.get(0);
            return "".equals(m.getHomeClub()) && "".equals(m.getAwayClub());
        }));
    }

    // -------------------------------------------------------------------------
    // getPlayedMatchesFromDB
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getPlayedMatchesFromDB: partidos FINISHED → agrupa por jornada")
    void getPlayedMatchesFromDB_finishedMatches_groupsByRound() {
        Match m1 = buildMatch(1, 3, 541, 529, MatchStatus.FINISHED);
        Match m2 = buildMatch(2, 3, 530, 540, MatchStatus.FINISHED);
        Match m3 = buildMatch(3, 4, 529, 541, MatchStatus.FINISHED);

        when(matchRepository.findByStatus(MatchStatus.FINISHED)).thenReturn(List.of(m1, m2, m3));

        Map<String, List<MatchDTO>> result = matchService.getPlayedMatchesFromDB();

        assertThat(result).containsKeys("jornada_3", "jornada_4");
        assertThat(result.get("jornada_3")).hasSize(2);
        assertThat(result.get("jornada_4")).hasSize(1);
    }

    @Test
    @DisplayName("getPlayedMatchesFromDB: sin partidos FINISHED → mapa vacío")
    void getPlayedMatchesFromDB_noFinished_returnsEmptyMap() {
        when(matchRepository.findByStatus(MatchStatus.FINISHED)).thenReturn(Collections.emptyList());

        Map<String, List<MatchDTO>> result = matchService.getPlayedMatchesFromDB();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPlayedMatchesFromDB: DTO tiene todos los campos mapeados correctamente")
    void getPlayedMatchesFromDB_fieldMapping_isCorrect() {
        Match m = buildMatch(1, 5, 541, 529, MatchStatus.FINISHED);
        m.setApiFootballFixtureId(1234);
        m.setHomeXg(1.5);
        m.setAwayXg(0.7);

        when(matchRepository.findByStatus(MatchStatus.FINISHED)).thenReturn(List.of(m));

        Map<String, List<MatchDTO>> result = matchService.getPlayedMatchesFromDB();

        MatchDTO dto = result.get("jornada_5").get(0);
        assertThat(dto.getFixtureId()).isEqualTo(1234);
        assertThat(dto.getHomeTeamId()).isEqualTo(541);
        assertThat(dto.getAwayTeamId()).isEqualTo(529);
        assertThat(dto.getHomeScore()).isEqualTo(1);
        assertThat(dto.getAwayScore()).isEqualTo(0);
        assertThat(dto.getHomeXg()).isEqualTo(1.5);
        assertThat(dto.getAwayXg()).isEqualTo(0.7);
        assertThat(dto.getHomeTeamName()).isEqualTo("Home FC");
        assertThat(dto.getAwayTeamName()).isEqualTo("Away FC");
    }

    // -------------------------------------------------------------------------
    // getUpcomingMatchesFromDB
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUpcomingMatchesFromDB: partidos UPCOMING → agrupa por jornada")
    void getUpcomingMatchesFromDB_upcomingMatches_groupsByRound() {
        Match m1 = buildMatch(1, 10, 541, 529, MatchStatus.UPCOMING);
        Match m2 = buildMatch(2, 11, 529, 541, MatchStatus.UPCOMING);

        when(matchRepository.findByStatus(MatchStatus.UPCOMING)).thenReturn(List.of(m1, m2));

        Map<String, List<UpcomingMatchDTO>> result = matchService.getUpcomingMatchesFromDB();

        assertThat(result).containsKeys("jornada_10", "jornada_11");
        assertThat(result.get("jornada_10")).hasSize(1);
    }

    @Test
    @DisplayName("getUpcomingMatchesFromDB: sin partidos UPCOMING → mapa vacío")
    void getUpcomingMatchesFromDB_noUpcoming_returnsEmptyMap() {
        when(matchRepository.findByStatus(MatchStatus.UPCOMING)).thenReturn(Collections.emptyList());

        Map<String, List<UpcomingMatchDTO>> result = matchService.getUpcomingMatchesFromDB();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUpcomingMatchesFromDB: DTO tiene todos los campos mapeados correctamente")
    void getUpcomingMatchesFromDB_fieldMapping_isCorrect() {
        Match m = buildMatch(1, 7, 541, 529, MatchStatus.UPCOMING);
        m.setApiFootballFixtureId(5678);
        m.setMatchDate("2026-06-15T18:00:00");

        when(matchRepository.findByStatus(MatchStatus.UPCOMING)).thenReturn(List.of(m));

        Map<String, List<UpcomingMatchDTO>> result = matchService.getUpcomingMatchesFromDB();

        UpcomingMatchDTO dto = result.get("jornada_7").get(0);
        assertThat(dto.getFixtureId()).isEqualTo(5678);
        assertThat(dto.getHomeTeamId()).isEqualTo(541);
        assertThat(dto.getAwayTeamId()).isEqualTo(529);
        assertThat(dto.getMatchDate()).isEqualTo("2026-06-15T18:00:00");
        assertThat(dto.getHomeTeamName()).isEqualTo("Home FC");
        assertThat(dto.getAwayTeamName()).isEqualTo("Away FC");
    }

    // -------------------------------------------------------------------------
    // getNameTeamMatch
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getNameTeamMatch: partido existe → devuelve [homeClub, awayClub]")
    void getNameTeamMatch_found_returnsTeamNames() {
        Match m = buildMatch(1, 3, 541, 529, MatchStatus.FINISHED);

        when(matchRepository.findById(1)).thenReturn(Optional.of(m));

        List<String> names = matchService.getNameTeamMatch(1);

        assertThat(names).containsExactly("Home FC", "Away FC");
    }

    @Test
    @DisplayName("getNameTeamMatch: partido no existe → RuntimeException")
    void getNameTeamMatch_notFound_throwsException() {
        when(matchRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.getNameTeamMatch(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Partido no encontrado");
    }

    // -------------------------------------------------------------------------
    // getNextMatchForClub — edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getNextMatchForClub: solo partido como visitante → devuelve ese partido")
    void getNextMatchForClub_onlyAwayMatch_returnsAwayMatch() {
        Match away = buildMatch(1, 5, 529, 541, MatchStatus.UPCOMING);

        when(matchRepository.findFirstByStatusAndHomeTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.empty());
        when(matchRepository.findFirstByStatusAndAwayTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.of(away));

        Match result = matchService.getNextMatchForClub(541);

        assertThat(result).isEqualTo(away);
    }

    @Test
    @DisplayName("getNextMatchForClub: solo partido como local → devuelve ese partido")
    void getNextMatchForClub_onlyHomeMatch_returnsHomeMatch() {
        Match home = buildMatch(1, 4, 541, 529, MatchStatus.UPCOMING);

        when(matchRepository.findFirstByStatusAndHomeTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.of(home));
        when(matchRepository.findFirstByStatusAndAwayTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.empty());

        Match result = matchService.getNextMatchForClub(541);

        assertThat(result).isEqualTo(home);
    }

    @Test
    @DisplayName("getNextMatchForClub: partido visitante con ronda menor → devuelve visitante")
    void getNextMatchForClub_awayMatchHasLowerRound_returnsAwayMatch() {
        Match home = buildMatch(1, 8, 541, 529, MatchStatus.UPCOMING);
        Match away = buildMatch(2, 6, 529, 541, MatchStatus.UPCOMING);

        when(matchRepository.findFirstByStatusAndHomeTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.of(home));
        when(matchRepository.findFirstByStatusAndAwayTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.of(away));

        Match result = matchService.getNextMatchForClub(541);

        assertThat(result).isEqualTo(away);
    }

    @Test
    @DisplayName("getNextMatchForClub: sin partidos → devuelve null")
    void getNextMatchForClub_noMatches_returnsNull() {
        when(matchRepository.findFirstByStatusAndHomeTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.empty());
        when(matchRepository.findFirstByStatusAndAwayTeamIdOrderByRoundAsc(MatchStatus.UPCOMING, 541))
                .thenReturn(Optional.empty());

        Match result = matchService.getNextMatchForClub(541);

        assertThat(result).isNull();
    }

    // -------------------------------------------------------------------------
    // importMatchesFromJson
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("importMatchesFromJson: JSON desde classpath → importa correctamente")
    void importMatchesFromJson_classpathResource() {
        // getPlayedMatches reads from classpath data/matches.json (test resources)
        // which may not exist — returns empty map, importMatchesFromData succeeds with 0
        when(matchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = matchService.importMatchesFromJson();

        assertThat(result).contains("0");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
