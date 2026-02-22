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
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Match.MatchStatus;

@Getter
@Setter
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @Column(name = "api_football_fixture_id", unique = true)
    private Integer apiFootballFixtureId;

    @Column(name = "round")
    private Integer round;

    @Column(name = "home_team_id")
    private Integer homeTeamId;

    @Column(name = "away_team_id")
    private Integer awayTeamId;

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
    @Column(name = "home_goals")
    private Integer homeGoals;

    @Min(0)
    @Column(name = "away_goals")
    private Integer awayGoals;

    @Column(name = "home_xg")
    private Double homeXg;

    @Column(name = "away_xg")
    private Double awayXg;

    @Column(name = "match_date")
    private String matchDate;

}
