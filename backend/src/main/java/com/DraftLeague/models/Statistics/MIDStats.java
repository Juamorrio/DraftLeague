package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mid_stats")
public class MIDStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @Min(0)
    @Column(name = "key_passes")
    private Integer keyPasses;

    @Min(0)
    @Column(name = "dribbles_completed")
    private Integer dribblesCompleted;

    @Min(0)
    @Column(name = "big_chances_created")
    private Integer bigChancesCreated;

    @Min(0)
    @Column(name = "big_chances_missed")
    private Integer bigChancesMissed;

    @Min(0)
    @Column(name = "duels_won")
    private Integer duelsWon;

    @Min(0)
    @Column(name = "duels_lost")
    private Integer duelsLost;

    @Valid
    @OneToOne(optional = true)
    private PlayerStatistic playerStatistic;
}
