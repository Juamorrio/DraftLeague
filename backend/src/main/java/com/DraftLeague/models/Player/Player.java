package com.DraftLeague.models.Player;

import com.DraftLeague.models.Team.Team;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.Player.Position;

@Getter
@Setter
@Entity
public class Player {

    @Id
    @Column(name = "id", nullable = false, unique = true)
    @NotNull
    private String id;

    @NotNull
    @Size(max = 60)
    @Column(name = "full_name", nullable = false, length = 60)
    private String fullName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 10)
    private Position position;

    @NotNull
    @Min(0)
    @Column(name = "market_value", nullable = false)
    private Integer marketValue;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @NotNull
    @Min(0)
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "avatar_url", nullable = true)
    private String avatarUrl;

    @NotNull
    @Column(name = "team_id", nullable = false)
    private Integer teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    @JsonIgnore
    private Team team;

    public String getName() {
        return this.fullName;
    }
}
