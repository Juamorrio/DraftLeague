package com.DraftLeague.models.Team;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "football_club")
public class FootballClub {

    @Id
    private Integer id;

    @NotNull
    private String name;

    private String shortName;
    private String tla;
    private String crest;
}
