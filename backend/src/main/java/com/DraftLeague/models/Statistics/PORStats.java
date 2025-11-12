package com.DraftLeague.models.Statistics;


import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PORStats {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @Min(0)
    @Column(name = "penalties_saved")
    private Integer penaltiesSaved;

    @Min(0)
    @Column(name = "saves")
    private Integer saves;

    @Min(0)
    @Column(name = "goals_conceded")
    private Integer goalsConceded;

    @Column(name = "clean_sheet", nullable = false)
    private Boolean cleanSheet;

    @Valid
    @OneToOne(optional = true)
    private PlayerStatistic playerStatistic;
}