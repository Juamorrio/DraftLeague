package com.DraftLeague.models.Score;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PlayerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @NotNull @Min(0)
    @Column(name = "minutes_played", nullable = false)
    private Integer minutesPlayed;

    @NotNull @Min(0)
    @Column(name = "goals", nullable = false)
    private Integer goals;

    @NotNull @Min(0)
    @Column(name = "assists", nullable = false)
    private Integer assists;

    @Min(0)
    @Column(name = "shots")
    private Integer shots;

    @Min(0)
    @Column(name = "shots_on_target")
    private Integer shotsOnTarget;

    @Min(0)
    @Column(name = "key_passes")
    private Integer keyPasses;

    @Min(0)
    @Column(name = "dribbles_completed")
    private Integer dribblesCompleted;

    @Min(0)
    @Column(name = "fouls_drawn")
    private Integer foulsDrawn;

    @Min(0)
    @Column(name = "fouls_committed")
    private Integer foulsCommitted;

    @Min(0)
    @Column(name = "big_chances_created")
    private Integer bigChancesCreated;

    @Min(0)
    @Column(name = "duels_won")
    private Integer duelsWon;

    @Min(0)
    @Column(name = "tackles")
    private Integer tackles;

    @Min(0)
    @Column(name = "interceptions")
    private Integer interceptions;

    @Min(0)
    @Column(name = "clearances")
    private Integer clearances;

    @Min(0)
    @Column(name = "blocks")
    private Integer blocks;

    @Min(0)
    @Column(name = "own_goals")
    private Integer ownGoals;

    @Min(0)
    @Column(name = "penalties_saved")
    private Integer penaltiesSaved;

    @Min(0)
    @Column(name = "saves")
    private Integer saves;

    @Min(0)
    @Column(name = "goals_conceded")
    private Integer goalsConceded;

    @NotNull
    @Column(name = "clean_sheet", nullable = false)
    private Boolean cleanSheet;

    @NotNull @Min(0)
    @Column(name = "yellow_cards", nullable = false)
    private Integer yellowCards;

    @NotNull @Min(0)
    @Column(name = "red_cards", nullable = false)
    private Integer redCards;
}
