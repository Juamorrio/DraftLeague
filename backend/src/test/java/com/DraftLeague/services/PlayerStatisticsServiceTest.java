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
        Match match = buildMatch(1, 2, 0); // home marcó 2, away marcó 0 → portero local no encajó goles
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
        Match match = buildMatch(1, 2, 1); // homeGoals=2 (rival marcó 2 → away team conceded 2)
        PlayerStatistic stat = buildStat(1, PlayerStatistic.PlayerType.DEFENDER);
        stat.setMatchId(1);
        stat.setIsHomeTeam(false); // equipo visitante → concede los homeGoals
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
}
