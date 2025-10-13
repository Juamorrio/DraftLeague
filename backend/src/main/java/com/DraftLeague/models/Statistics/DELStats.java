package com.DraftLeague.models.Statistics;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class DELStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

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
    @Column(name = "offsides")
    private Integer offsides;
}