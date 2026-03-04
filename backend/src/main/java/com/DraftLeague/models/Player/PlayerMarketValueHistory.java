package com.DraftLeague.models.Player;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player_market_value_history")
public class PlayerMarketValueHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "player_id", nullable = false)
    private String playerId;

    @NotNull
    @Column(name = "gameweek", nullable = false)
    private Integer gameweek;

    @NotNull
    @Column(name = "previous_value", nullable = false)
    private Integer previousValue;

    @NotNull
    @Column(name = "new_value", nullable = false)
    private Integer newValue;

    @NotNull
    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;

    @NotNull
    @Column(name = "change_percentage", nullable = false)
    private Double changePercentage;

    @Column(name = "recorded_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recordedAt = new Date();
}
