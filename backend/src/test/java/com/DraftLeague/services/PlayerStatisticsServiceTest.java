package com.DraftLeague.services;

import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;
import com.DraftLeague.models.Statistics.PlayerStatistic;
import com.DraftLeague.repositories.MatchRepository;
import com.DraftLeague.repositories.PlayerStatisticRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerStatisticsService Unit Tests")
class PlayerStatisticsServiceTest {

    @Mock
    private PlayerStatisticRepository playerStatisticRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private PlayerStatisticsService playerStatisticsService;


    @Test
    @DisplayName("recalculateAllFantasyPoints: devuelve el número de estadísticas procesadas")
    void recalculateAllFantasyPoints_returnsProcessedCount() {
        PlayerStatistic stat1 = buildStat(1, PlayerStatistic.PlayerType.MIDFIELDER);
        PlayerStatistic stat2 = buildStat(2, PlayerStatistic.PlayerType.FORWARD);
        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat1, stat2));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int result = playerStatisticsService.recalculateAllFantasyPoints();

        assertThat(result).isEqualTo(2);
        verify(playerStatisticRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("recalculateAllFantasyPoints: lista vacía → devuelve 0 y llama saveAll")
    void recalculateAllFantasyPoints_emptyList_returnsZero() {
        when(playerStatisticRepository.findAll()).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        int result = playerStatisticsService.recalculateAllFantasyPoints();

        assertThat(result).isEqualTo(0);
        verify(playerStatisticRepository).saveAll(Collections.emptyList());
    }


    @Test
    @DisplayName("recalculateCleanSheets: portero con cleanSheet nulo → lo establece a true (0 goles)")
    void recalculateCleanSheets_goalkeeperWithNullCleanSheet_setsCleanSheetTrue() {
        Match match = buildMatch(1, 2, 0); 
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(true);
        stat.setCleanSheet(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateCleanSheets();

        assertThat(stat.getCleanSheet()).isTrue();
    }

    @Test
    @DisplayName("recalculateCleanSheets: delantero → cleanSheet no se establece")
    void recalculateCleanSheets_forwardPlayer_cleanSheetNotSet() {
        Match match = buildMatch(1, 0, 1);
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.FORWARD);
        stat.setMatchId(1);
        stat.setIsHomeTeam(true);
        stat.setCleanSheet(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateCleanSheets();

        assertThat(stat.getCleanSheet()).isNull();
    }

    @Test
    @DisplayName("recalculateCleanSheets: stat sin matchId → se omite (no añadido a toSave)")
    void recalculateCleanSheets_statWithoutMatchId_isSkipped() {
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        int result = playerStatisticsService.recalculateCleanSheets();

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("recalculateCleanSheets: defensa con goalsConceded nulo → lo establece")
    void recalculateCleanSheets_defenderWithNullGoalsConceded_setsGoalsConceded() {
        Match match = buildMatch(1, 2, 1); 
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.DEFENDER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(false); 
        stat.setGoalsConceded(null);
        stat.setCleanSheet(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateCleanSheets();

        assertThat(stat.getGoalsConceded()).isEqualTo(2);
    }


    private PlayerStatistic buildStat(int id, PlayerStatistic.PlayerType type) {
        PlayerStatistic s = new PlayerStatistic();
        s.setId(id);
        s.setPlayerType(type);
        s.setMinutesPlayed(90);
        s.setGoals(0);
        s.setAssists(0);
        s.setYellowCards(0);
        s.setRedCards(0);
        return s;
    }

    private Match buildMatch(int id, int homeGoals, int awayGoals) {
        Match m = new Match();
        m.setId(id);
        m.setHomeGoals(homeGoals);
        m.setAwayGoals(awayGoals);
        m.setStatus(MatchStatus.FINISHED);
        return m;
    }

    // -----------------------------------------------------------------------
    // recalculateAllFantasyPoints – additional branches
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("recalculateAllFantasyPoints: excepción en un stat → skipped++ y proceso continúa")
    void recalculateAllFantasyPoints_exceptionOnOneStat_skipsAndContinues() {
        PlayerStatistic bad = new PlayerStatistic(); 
        bad.setId(1);
        PlayerStatistic good = buildStat(2, PlayerStatistic.PlayerType.MIDFIELDER);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(bad, good));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int result = playerStatisticsService.recalculateAllFantasyPoints();

        assertThat(result).isGreaterThanOrEqualTo(0);
        verify(playerStatisticRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("recalculateAllFantasyPoints: totalFantasyPoints actualizado en cada stat")
    void recalculateAllFantasyPoints_updatesFantasyPoints() {
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.MIDFIELDER);
        stat.setGoals(2);
        stat.setAssists(1);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateAllFantasyPoints();

        assertThat(stat.getTotalFantasyPoints()).isNotNull();
    }

    // -----------------------------------------------------------------------
    // recalculateCleanSheets – additional branches
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("recalculateCleanSheets: stat con isHomeTeam null → se omite")
    void recalculateCleanSheets_nullIsHomeTeam_statSkipped() {
        Match match = buildMatch(1, 0, 0);
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(null); 

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        int result = playerStatisticsService.recalculateCleanSheets();

        assertThat(result).isEqualTo(0);
        assertThat(stat.getCleanSheet()).isNull();
    }

    @Test
    @DisplayName("recalculateCleanSheets: portero visitante que encajó goles → cleanSheet false")
    void recalculateCleanSheets_goalkeeperAwayWithGoalsConceded_cleanSheetFalse() {
        Match match = buildMatch(1, 2, 0);
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(false); 
        stat.setCleanSheet(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateCleanSheets();

        assertThat(stat.getCleanSheet()).isFalse();
    }

    @Test
    @DisplayName("recalculateCleanSheets: cleanSheet ya establecido → no sobreescribe")
    void recalculateCleanSheets_cleanSheetAlreadySet_notOverwritten() {
        Match match = buildMatch(1, 0, 0);
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(true);
        stat.setCleanSheet(false); 

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateCleanSheets();

        assertThat(stat.getCleanSheet()).isFalse();
    }

    @Test
    @DisplayName("recalculateCleanSheets: centrocampista → goalsConceded no se establece")
    void recalculateCleanSheets_midfielder_goalsConcededNotSet() {
        Match match = buildMatch(1, 1, 2);
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.MIDFIELDER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(true);
        stat.setGoalsConceded(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateCleanSheets();

        assertThat(stat.getGoalsConceded()).isNull();
    }

    @Test
    @DisplayName("recalculateCleanSheets: match sin goals nulos → se omite el stat")
    void recalculateCleanSheets_matchWithNullGoals_statSkipped() {
        Match match = new Match();
        match.setId(1);
        match.setHomeGoals(null); 
        match.setAwayGoals(null);
        match.setStatus(MatchStatus.FINISHED);

        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(true);
        stat.setCleanSheet(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        int result = playerStatisticsService.recalculateCleanSheets();

        assertThat(result).isEqualTo(0);
        assertThat(stat.getCleanSheet()).isNull();
    }

    @Test
    @DisplayName("recalculateCleanSheets: matchId en stat no coincide con ningún match → se omite")
    void recalculateCleanSheets_matchIdNotFound_statSkipped() {
        Match match = buildMatch(999, 1, 0);
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(1); 

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        int result = playerStatisticsService.recalculateCleanSheets();

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("recalculateCleanSheets: portero con goalsConceded ya establecido → no sobreescribe")
    void recalculateCleanSheets_goalkeeperWithExistingGoalsConceded_notOverwritten() {
        Match match = buildMatch(1, 3, 0);
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(false); 
        stat.setGoalsConceded(1); 
        stat.setCleanSheet(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateCleanSheets();

        // Existing goalsConceded=1 not overwritten (condition: getGoalsConceded() == null)
        assertThat(stat.getGoalsConceded()).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // recalculateCleanSheets – multiple stats, some skipped
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("recalculateCleanSheets: mezcla de stats válidos y skipped → solo válidos guardados")
    void recalculateCleanSheets_mixedStats_onlyValidSaved() {
        Match match = buildMatch(1, 0, 0);

        PlayerStatistic valid = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        valid.setMatchId(1);
        valid.setIsHomeTeam(true);
        valid.setCleanSheet(null);

        PlayerStatistic noMatch = buildStat(2, PlayerStatistic.PlayerType.GOALKEEPER);
        noMatch.setMatchId(null); // will be skipped

        PlayerStatistic nullHome = buildStat(3, PlayerStatistic.PlayerType.MIDFIELDER);
        nullHome.setMatchId(1);
        nullHome.setIsHomeTeam(null); // will be skipped

        when(playerStatisticRepository.findAll()).thenReturn(List.of(valid, noMatch, nullHome));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int result = playerStatisticsService.recalculateCleanSheets();

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("recalculateCleanSheets: lista vacía de stats → devuelve 0")
    void recalculateCleanSheets_emptyStats_returnsZero() {
        when(playerStatisticRepository.findAll()).thenReturn(Collections.emptyList());
        when(matchRepository.findAll()).thenReturn(Collections.emptyList());
        when(playerStatisticRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        int result = playerStatisticsService.recalculateCleanSheets();

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("recalculateCleanSheets: portero local con cleanSheet ya false → totalFantasyPoints recalculado igualmente")
    void recalculateCleanSheets_goalkeeperWithCleanSheetFalseAlready_fantasyPointsStillRecalculated() {
        Match match = buildMatch(1, 2, 1);
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.GOALKEEPER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(true); 
        stat.setCleanSheet(false); 

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int result = playerStatisticsService.recalculateCleanSheets();

        assertThat(result).isEqualTo(1);
        assertThat(stat.getTotalFantasyPoints()).isNotNull();
    }

    @Test
    @DisplayName("recalculateCleanSheets: defensa local con cleanSheet nulo → goalsConceded = awayGoals")
    void recalculateCleanSheets_homeDefender_goalsConcededFromAwayGoals() {
        Match match = buildMatch(1, 1, 3); // awayGoals=3 → home defender conceded 3
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.DEFENDER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(true);
        stat.setCleanSheet(null);
        stat.setGoalsConceded(null);

        when(playerStatisticRepository.findAll()).thenReturn(List.of(stat));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(playerStatisticRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        playerStatisticsService.recalculateCleanSheets();

        assertThat(stat.getGoalsConceded()).isEqualTo(3);
        assertThat(stat.getCleanSheet()).isFalse(); 
    }
}
