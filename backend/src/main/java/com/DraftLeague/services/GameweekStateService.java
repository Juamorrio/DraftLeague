package com.DraftLeague.services;

import com.DraftLeague.models.Gameweek.GameweekState;
import com.DraftLeague.repositories.GameweekStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class GameweekStateService {

    private static final int STATE_ID = 1;

    private final GameweekStateRepository gameweekStateRepository;

    /**
     * Returns the current singleton state. Creates it (unlocked, no active gameweek) if it does not exist yet.
     */
    @Transactional(readOnly = true)
    public GameweekState getState() {
        return gameweekStateRepository.findById(STATE_ID)
                .orElseGet(() -> {
                    GameweekState state = new GameweekState();
                    state.setId(STATE_ID);
                    state.setTeamsLocked(false);
                    return state;
                });
    }

    /**
     * Activates scoring for the given gameweek and locks all team modifications.
     * Called by the admin via the "Activar jornada" button.
     */
    @Transactional
    public GameweekState activateGameweek(Integer gameweek) {
        GameweekState state = gameweekStateRepository.findById(STATE_ID)
                .orElseGet(() -> {
                    GameweekState s = new GameweekState();
                    s.setId(STATE_ID);
                    return s;
                });

        state.setActiveGameweek(gameweek);
        state.setTeamsLocked(true);
        state.setLockedAt(new Date());
        state.setUnlockedAt(null);

        return gameweekStateRepository.save(state);
    }

    /**
     * Unlocks team modifications without changing the active gameweek.
     * Called by the admin when the scoring period has ended.
     */
    @Transactional
    public GameweekState unlockTeams() {
        GameweekState state = gameweekStateRepository.findById(STATE_ID)
                .orElseGet(() -> {
                    GameweekState s = new GameweekState();
                    s.setId(STATE_ID);
                    return s;
                });

        state.setTeamsLocked(false);
        state.setUnlockedAt(new Date());

        return gameweekStateRepository.save(state);
    }

    /**
     * Convenience method used by TeamService / MarketService to enforce the lock.
     */
    public boolean isTeamsLocked() {
        return Boolean.TRUE.equals(getState().getTeamsLocked());
    }

    /**
     * Returns the active gameweek or null if none has been activated.
     */
    public Integer getActiveGameweek() {
        return getState().getActiveGameweek();
    }
}
