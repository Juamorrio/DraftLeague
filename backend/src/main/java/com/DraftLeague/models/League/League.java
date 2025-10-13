package com.DraftLeague.models.League;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "leagues")
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @NotNull
    @Pattern(regexp = "^[A-Z0-9]{6}$")
    @Column(name = "code", nullable = false, unique = true, length = 6)
    private String code;

    @NotNull
    @Size(max = 60)
    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @NotNull
    @Past
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @NotNull
    @Min(2)
    @Max(50)
    @Column(name = "max_teams", nullable = false)
    private Integer maxTeams;

    @NotNull
    @Min(0)
    @Column(name = "initial_budget", nullable = false)
    private Integer initialBudget;

    @NotNull
    @Temporal(TemporalType.TIME)
    @Column(name = "market_end_hour", nullable = false)
    private Date marketEndHour;

    @NotNull
    @Column(name = "captain_enable", nullable = false)
    private Boolean captainEnable;

    @NotNull
    @Column(name = "wildcards_enable", nullable = false)
    private Boolean wildCardsEnable;

    @Min(1)
    @Column(name = "ranking")
    private Integer ranking; 
}