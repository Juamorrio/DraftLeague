package com.DraftLeague.models.League;
import com.DraftLeague.models.Notification.NotificationLeague;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.DraftLeague.models.Market.MarketPlayer;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.user.User;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Market.MarketPlayer;

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
    @Column(name = "market_end_hour", nullable = false)
    private String marketEndHour;

    @NotNull
    @Column(name = "captain_enable", nullable = false)
    private Boolean captainEnable;


    @ManyToOne(optional = true)
    @Valid
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams = new ArrayList<>();

    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MarketPlayer> marketPlayers = new ArrayList<>();

    //@ManyToOne(optional = true)
    //@Valid
    //private Chat chat;

    //@ManyToOne(optional = true)
    //@Valid
    //private NotificationLeague notificationLeague;
}
