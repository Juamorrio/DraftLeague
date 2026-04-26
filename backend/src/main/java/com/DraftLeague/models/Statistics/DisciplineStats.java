package com.DraftLeague.models.Statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class DisciplineStats {

    @Min(0)
    @Column(name = "yellow_cards")
    private Integer yellowCards = 0;

    @Min(0)
    @Column(name = "red_cards")
    private Integer redCards = 0;

    @Min(0)
    @Column(name = "fouls_committed")
    private Integer foulsCommitted = 0;

    @Min(0)
    @Column(name = "was_fouled")
    private Integer wasFouled = 0;

    @Min(0)
    @Column(name = "penalties_won")
    private Integer penaltiesWon = 0;

    @Min(0)
    @Column(name = "penalty_scored")
    private Integer penaltyScored = 0;

    @Min(0)
    @Column(name = "penalty_missed")
    private Integer penaltyMissed = 0;

    @Min(0)
    @Column(name = "penalty_committed")
    private Integer penaltyCommitted = 0;
}
