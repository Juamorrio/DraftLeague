package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "def_stats")
public class DEFStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

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
    @Column(name = "duels_won")
    private Integer duelsWon;

    @Min(0)
    @Column(name = "duels_lost")
    private Integer duelsLost;
}
