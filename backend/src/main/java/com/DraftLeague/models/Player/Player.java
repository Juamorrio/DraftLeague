package com.DraftLeague.models.Player;

import org.checkerframework.checker.units.qual.C;

import com.DraftLeague.models.Statistics.PlayerStatistic;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Player {

    @Id
    @Column(name = "id", nullable = false, unique = true)
    @NotNull
    private Integer id;

    @NotNull
    @Size(max = 60)
    @Column(name = "full_name", nullable = false, length = 60)
    private String fullName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 3)
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

    @ManyToOne(optional = false)
    @Valid
    private PlayerStatistic playerStatistic;
}
