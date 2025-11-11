package com.DraftLeague.models.Team;

import java.util.Date;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.PlayerTeam;
import com.DraftLeague.models.user.User;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Team {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;


    @NotNull
    @Min(0)
    @Column(name = "budget", nullable = false)
    private Integer budget;

    @NotNull
    @Past
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Min(0)
    @Column(name = "gameweek_points")
    private Integer gameweekPoints;   

    @Min(0)
    @Column(name = "total_points")
    private Integer totalPoints;

    @NotNull
    @Column(name = "wildcard_used", nullable = false)
    //boolean??
    private Boolean wildcardUsed;

    @Min(0)
    @Column(name = "captain_id")
    private Integer captainId;        


    @ManyToOne(optional = false)
    @NotNull
    @Valid
    private User user;

    @ManyToOne(optional = false)
    @NotNull
    @Valid
    private League league;

    @ManyToOne(optional = false)
    @NotNull
    @Valid
    private PlayerTeam playerTeam;
}