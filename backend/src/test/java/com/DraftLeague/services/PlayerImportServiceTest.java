package com.DraftLeague.services;

import com.DraftLeague.dto.PlayerImportDTO;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.repositories.PlayerRepository;
import com.DraftLeague.api.GameweekStatsSyncService;
import com.DraftLeague.api.PlayerSquadSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerImportService Unit Tests")
class PlayerImportServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerStatisticService playerStatisticService;

    @Mock
    private PlayerSquadSyncService playerSquadSyncService;

    @Mock
    private GameweekStatsSyncService gameweekStatsSyncService;

    @Mock
    private EntityManager entityManager;

    private PlayerImportService playerImportService;

    @BeforeEach
    void setUp() {
        // Use a real ObjectMapper so ClassPathResource JSON is deserialized properly
        ObjectMapper realMapper = new ObjectMapper();
        playerImportService = new PlayerImportService(
                playerRepository, realMapper, playerStatisticService,
                playerSquadSyncService, gameweekStatsSyncService);
        ReflectionTestUtils.setField(playerImportService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("syncGameweekStats: delega en gameweekStatsSyncService y luego en playerStatisticService")
    void syncGameweekStats_delegatesToBothServices() throws Exception {
        List<Map<String, Object>> statsData = List.of(
                Map.of("playerId", "P1", "playerType", "MIDFIELDER"),
                Map.of("playerId", "P2", "playerType", "FORWARD"),
                Map.of("playerId", "P3", "playerType", "DEFENDER")
        );
        when(gameweekStatsSyncService.fetchStats(5)).thenReturn(statsData);
        when(playerStatisticService.saveBulkFromJson(statsData)).thenReturn(Collections.emptyList());

        int result = playerImportService.syncGameweekStats(5);

        verify(gameweekStatsSyncService).fetchStats(5);
        verify(playerStatisticService).saveBulkFromJson(statsData);
        assertThat(result).isEqualTo(3);
    }

    @Test
    @DisplayName("syncGameweekStats: retorna el número de registros obtenidos del servicio de sync")
    void syncGameweekStats_returnsStatsCount() throws Exception {
        when(gameweekStatsSyncService.fetchStats(3)).thenReturn(Collections.emptyList());
        when(playerStatisticService.saveBulkFromJson(anyList())).thenReturn(Collections.emptyList());

        int result = playerImportService.syncGameweekStats(3);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("importNewPlayersOnly: jugadores existentes → no se crean (devuelve 0)")
    void importNewPlayersOnly_allPlayersExist_returnsZero() throws Exception {
        // Test JSON at src/test/resources/data/players_data.json contains P1 and P2
        when(playerRepository.existsById("P1")).thenReturn(true);
        when(playerRepository.existsById("P2")).thenReturn(true);

        int result = playerImportService.importNewPlayersOnly();

        assertThat(result).isEqualTo(0);
        verify(playerRepository, never()).saveAll(argThat(list -> !((List<?>) list).isEmpty()));
    }

    @Test
    @DisplayName("importNewPlayersOnly: jugadores nuevos → se crean y se guardan")
    void importNewPlayersOnly_newPlayers_savesAll() throws Exception {
        // P1 exists, P2 is new
        when(playerRepository.existsById("P1")).thenReturn(true);
        when(playerRepository.existsById("P2")).thenReturn(false);
        when(playerRepository.saveAll(anyList())).thenReturn(List.of(new Player()));

        int result = playerImportService.importNewPlayersOnly();

        assertThat(result).isEqualTo(1);
        verify(playerRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }
}
