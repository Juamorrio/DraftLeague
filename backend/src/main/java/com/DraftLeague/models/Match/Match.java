package com.DraftLeague.models.Match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    
    @NotNull
    @Column(name = "home_club", nullable = false)
    private String homeClub;

    @NotNull
    @Column(name = "away_club", nullable = false)
    private String awayClub;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "status", nullable = false)
    private MatchStatus status;

    @Min(0)
    @Column(name = "home_goals", nullable = false)
    private Integer homeGoals;

    @Min(0)
    @Column(name = "away_goals", nullable = false)
    private Integer awayGoals;

}
