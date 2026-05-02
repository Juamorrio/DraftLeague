package com.DraftLeague.models.Gameweek;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

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
