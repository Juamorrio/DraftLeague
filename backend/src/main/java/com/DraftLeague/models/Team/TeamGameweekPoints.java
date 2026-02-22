package com.DraftLeague.models.Team;
import com.DraftLeague.models.Team.TeamGameweekPoints;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import com.DraftLeague.models.Team.Team;

@Getter
@Setter
@Entity
@Table(name = "team_gameweek_points")
public class TeamGameweekPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "gameweek", nullable = false)
    private Integer gameweek;

    @Column(name = "points")
    private Integer points = 0;

    @Column(name = "goalkeeper_points")
    private Integer goalkeeperPoints = 0;

    @Column(name = "defender_points")
    private Integer defenderPoints = 0;

    @Column(name = "midfielder_points")
    private Integer midfielderPoints = 0;

    @Column(name = "forward_points")
    private Integer forwardPoints = 0;

    @Column(name = "captain_id")
    private String captainId;

    @Column(name = "captain_bonus")
    private Integer captainBonus = 0;

    @Column(name = "bench_points")
    private Integer benchPoints = 0;

    @Column(name = "top_scorer_id")
    private String topScorerId;

    @Column(name = "top_scorer_points")
    private Integer topScorerPoints = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "calculated_at")
    private Date calculatedAt;
}
