package com.DraftLeague.models.Player;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PlayerTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "is_captain", nullable = false)
    private Boolean isCaptain;

    @NotNull
    @Column(name = "lined", nullable = false)
    private Boolean lined;

    @NotNull
    @Min(0)
    @Column(name = "sell_price", nullable = false)
    private Integer sellPrice;

}
