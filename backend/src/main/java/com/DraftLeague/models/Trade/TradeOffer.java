package com.DraftLeague.models.Trade;

import java.util.Date;

import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Team.Team;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class TradeOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @Valid
    private Team fromTeam;

    @NotNull
    @ManyToOne(optional = false)
    @Valid
    private Team toTeam;

    @NotNull
    @ManyToOne(optional = false)
    @Valid
    private Player player;

    @NotNull
    @ManyToOne(optional = false)
    @Valid
    private League league;

    @NotNull
    @Min(1)
    @Column(name = "offer_price", nullable = false)
    private Integer offerPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TradeOfferStatus status = TradeOfferStatus.PENDING;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;
}
