package com.DraftLeague.models.Gameweek;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Singleton entity (id always = 1) that holds the global gameweek state.
 *
 * When the admin activates a gameweek:
 *   - activeGameweek is set to the chosen round
 *   - teamsLocked = true  → users cannot modify their team
 *
 * When the admin unlocks teams (end of scoring period):
 *   - teamsLocked = false  → users can buy/sell/swap players again
 */
@Getter
@Setter
@Entity
@Table(name = "gameweek_state")
public class GameweekState {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Column(name = "active_gameweek")
    private Integer activeGameweek;

    @Column(name = "teams_locked", nullable = false)
    private Boolean teamsLocked = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "locked_at")
    private Date lockedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "unlocked_at")
    private Date unlockedAt;
}
