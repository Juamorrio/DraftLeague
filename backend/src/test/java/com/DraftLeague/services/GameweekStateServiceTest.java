package com.DraftLeague.services;

import com.DraftLeague.models.Gameweek.GameweekState;
import com.DraftLeague.repositories.GameweekStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameweekStateService Unit Tests")
class GameweekStateServiceTest {

    @Mock
    private GameweekStateRepository gameweekStateRepository;

    @InjectMocks
    private GameweekStateService gameweekStateService;

    // ─── isTeamsLocked ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTeamsLocked: estado con teamsLocked=true → devuelve true")
    void isTeamsLocked_whenLocked_returnsTrue() {
        GameweekState state = buildState(5, true);
        when(gameweekStateRepository.findById(1)).thenReturn(Optional.of(state));

        boolean result = gameweekStateService.isTeamsLocked();

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isTeamsLocked: estado con teamsLocked=false → devuelve false")
    void isTeamsLocked_whenUnlocked_returnsFalse() {
        GameweekState state = buildState(5, false);
        when(gameweekStateRepository.findById(1)).thenReturn(Optional.of(state));

        boolean result = gameweekStateService.isTeamsLocked();

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isTeamsLocked: estado no inicializado → devuelve false (default)")
    void isTeamsLocked_noState_returnsFalseByDefault() {
        when(gameweekStateRepository.findById(1)).thenReturn(Optional.empty());

        boolean result = gameweekStateService.isTeamsLocked();

        assertThat(result).isFalse();
    }

    // ─── getActiveGameweek ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getActiveGameweek: jornada activa configurada → devuelve el número correcto")
    void getActiveGameweek_configured_returnsActiveGameweek() {
        GameweekState state = buildState(7, false);
        when(gameweekStateRepository.findById(1)).thenReturn(Optional.of(state));

        Integer result = gameweekStateService.getActiveGameweek();

        assertThat(result).isEqualTo(7);
    }

    @Test
    @DisplayName("getActiveGameweek: sin estado → devuelve null")
    void getActiveGameweek_noState_returnsNull() {
        when(gameweekStateRepository.findById(1)).thenReturn(Optional.empty());

        Integer result = gameweekStateService.getActiveGameweek();

        assertThat(result).isNull();
    }

    // ─── activateGameweek ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("activateGameweek: activa jornada → bloquea equipos y guarda estado")
    void activateGameweek_savesLockedState() {
        GameweekState existing = buildState(null, false);
        GameweekState saved = buildState(8, true);

        when(gameweekStateRepository.findById(1)).thenReturn(Optional.of(existing));
        when(gameweekStateRepository.save(any(GameweekState.class))).thenReturn(saved);

        GameweekState result = gameweekStateService.activateGameweek(8);

        verify(gameweekStateRepository).save(any(GameweekState.class));
        assertThat(result.getTeamsLocked()).isTrue();
        assertThat(result.getActiveGameweek()).isEqualTo(8);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private GameweekState buildState(Integer activeGameweek, boolean locked) {
        GameweekState state = new GameweekState();
        state.setId(1);
        state.setActiveGameweek(activeGameweek);
        state.setTeamsLocked(locked);
        return state;
    }
}
