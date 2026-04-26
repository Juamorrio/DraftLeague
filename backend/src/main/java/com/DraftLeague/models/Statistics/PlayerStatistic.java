package com.DraftLeague.models.Statistics;

import com.DraftLeague.models.Player.Player;
import com.DraftLeague.models.Match.Match;
import com.DraftLeague.models.Team.ChipType;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "player_statistic")
public class PlayerStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "player_id", nullable = false)
    private String playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    @NotNull
    @Column(name = "match_id", nullable = false)
    private Integer matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", insertable = false, updatable = false)
    private Match match;

    @NotNull
    @Column(name = "is_home_team", nullable = false)
    private Boolean isHomeTeam;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "player_type", nullable = false)
    private PlayerType playerType;

    @Column(name = "role")
    private String role;

    @Column(name = "rating")
    private Double rating;

    @NotNull
    @Min(0)
    @Column(name = "minutes_played", nullable = false)
    private Integer minutesPlayed;

    @Column(name = "is_substitute")
    private Boolean isSubstitute = false;

    @Column(name = "is_captain")
    private Boolean isCaptain = false;

    @Min(0)
    @Column(name = "shirt_number")
    private Integer shirtNumber;

    @Column(name = "total_fantasy_points")
    private Integer totalFantasyPoints = 0;

    @Embedded
    private ShootingStats shooting = new ShootingStats();

    @Embedded
    private PassingStats passing = new PassingStats();

    @Embedded
    private DribblingStats dribbling = new DribblingStats();

    @Embedded
    private DefensiveStats defensive = new DefensiveStats();

    @Embedded
    private DisciplineStats discipline = new DisciplineStats();

    @Embedded
    private GoalkeeperStats goalkeeper = new GoalkeeperStats();


    public int calculateFantasyPoints() {
        int points = 0;

        if (minutesPlayed >= 60) {
            points += 3;
        } else if (minutesPlayed >= 45) {
            points += 2;
        } else if (minutesPlayed > 0) {
            points += 1;
        }

        Integer goals = shooting.getGoals();
        if (goals != null && goals > 0) {
            switch (playerType) {
                case GOALKEEPER:
                case DEFENDER:
                    points += goals * 6;
                    break;
                case MIDFIELDER:
                    points += goals * 5;
                    break;
                case FORWARD:
                    points += goals * 4;
                    break;
            }
        }

        Integer assists = passing.getAssists();
        if (assists != null && assists > 0) {
            points += assists * 3;
        }

        Integer chancesCreated = passing.getChancesCreated();
        if ((playerType == PlayerType.MIDFIELDER || playerType == PlayerType.FORWARD)
                && chancesCreated != null && chancesCreated >= 3) {
            points += chancesCreated / 3;
        }

        if (rating != null) {
            if (rating >= 9.0) {
                points += 5;
            } else if (rating >= 8.0) {
                points += 3;
            } else if (rating >= 7.0) {
                points += 1;
            } else if (rating < 5.0) {
                points -= 1;
            }
        }

        Integer yellowCards = discipline.getYellowCards();
        Integer redCards = discipline.getRedCards();
        Integer foulsCommitted = discipline.getFoulsCommitted();

        if (yellowCards != null && yellowCards > 0) {
            points -= yellowCards;
        }
        if (redCards != null && redCards > 0) {
            points -= redCards * 3;
        }
        if (foulsCommitted != null && foulsCommitted >= 3) {
            points -= foulsCommitted / 3;
        }

        Integer tackles = defensive.getTackles();
        Integer interceptions = defensive.getInterceptions();
        Integer blocks = defensive.getBlocks();
        Integer duelsWon = defensive.getDuelsWon();

        if (playerType == PlayerType.DEFENDER || playerType == PlayerType.MIDFIELDER) {
            if (tackles != null) points += tackles / 3;
            if (interceptions != null) points += interceptions / 3;
            if (blocks != null) points += blocks / 4;
        }

        if (duelsWon != null && duelsWon >= 5) {
            points += 1;
        }

        Boolean cleanSheet = goalkeeper.getCleanSheet();
        if ((playerType == PlayerType.GOALKEEPER || playerType == PlayerType.DEFENDER)
                && minutesPlayed >= 60) {
            if (cleanSheet != null && cleanSheet) {
                points += 4;
            }
        }

        if (playerType == PlayerType.MIDFIELDER && minutesPlayed >= 60) {
            if (cleanSheet != null && cleanSheet) {
                points += 1;
            }
        }

        Integer saves = goalkeeper.getSaves();
        if (playerType == PlayerType.GOALKEEPER && saves != null && saves >= 3) {
            points += saves / 3;
        }

        if (goals != null && goals >= 3) {
            points += 5;
        }

        Integer penaltiesSaved = goalkeeper.getPenaltiesSaved();
        if (playerType == PlayerType.GOALKEEPER && penaltiesSaved != null && penaltiesSaved > 0) {
            points += penaltiesSaved * 5;
        }

        if (assists != null && assists == 2) {
            points += 3;
        }
        if (assists != null && assists >= 3) {
            points += 8;
        }

        Integer goalsConceded = goalkeeper.getGoalsConceded();
        if (playerType == PlayerType.GOALKEEPER && goalsConceded != null && goalsConceded >= 3) {
            points -= 2;
        }
        if (playerType == PlayerType.DEFENDER && goalsConceded != null && goalsConceded >= 3) {
            points -= 1;
        }

        Integer penaltyCommitted = discipline.getPenaltyCommitted();
        Integer penaltyMissed = discipline.getPenaltyMissed();
        if (penaltyCommitted != null && penaltyCommitted > 0) {
            points -= penaltyCommitted * 2;
        }
        if (penaltyMissed != null && penaltyMissed > 0) {
            points -= penaltyMissed * 2;
        }

        return Math.max(points, 0);
    }


    public int calculateFantasyPointsWithChip(String chipName) {
        if (chipName == null) return calculateFantasyPoints();
        ChipType chip = ChipType.isValid(chipName) ? ChipType.valueOf(chipName) : null;
        if (chip == null) return calculateFantasyPoints();

        int points = 0;

        if (minutesPlayed >= 60) {
            points += (chip == ChipType.GOLDEN_MINUTES) ? 5 : 3;
        } else if (minutesPlayed >= 45) {
            points += 2;
        } else if (minutesPlayed > 0) {
            points += 1;
        }

        Integer goals = shooting.getGoals();
        if (goals != null && goals > 0) {
            if (chip == ChipType.DOUBLE_GOALS) {
                switch (playerType) {
                    case GOALKEEPER: case DEFENDER: points += goals * 12; break;
                    case MIDFIELDER:                points += goals * 10; break;
                    case FORWARD:                   points += goals * 8;  break;
                }
            } else if (chip == ChipType.LETHAL_STRIKER) {
                switch (playerType) {
                    case GOALKEEPER: case DEFENDER: points += goals * 6;  break;
                    case MIDFIELDER:                points += goals * 5;  break;
                    case FORWARD:                   points += goals * 12; break;
                }
            } else {
                switch (playerType) {
                    case GOALKEEPER: case DEFENDER: points += goals * 6; break;
                    case MIDFIELDER:                points += goals * 5; break;
                    case FORWARD:                   points += goals * 4; break;
                }
            }
        }

        Integer assists = passing.getAssists();
        if (assists != null && assists > 0) {
            points += assists * (chip == ChipType.DOUBLE_ASSISTS ? 6 : 3);
        }

        Integer chancesCreated = passing.getChancesCreated();
        if ((playerType == PlayerType.MIDFIELDER || playerType == PlayerType.FORWARD)
                && chancesCreated != null && chancesCreated > 0) {
            if (chip == ChipType.CREATIVE_MIDS) {
                points += chancesCreated;
            } else if (chancesCreated >= 3) {
                points += chancesCreated / 3;
            }
        }

        if (rating != null) {
            if (rating >= 9.0)      points += 5;
            else if (rating >= 8.0) points += 3;
            else if (rating >= 7.0) points += 1;
            else if (rating < 5.0)  points -= 1;
        }

        if (chip != ChipType.NO_PENALTY) {
            Integer yellowCards = discipline.getYellowCards();
            Integer redCards = discipline.getRedCards();
            if (yellowCards != null && yellowCards > 0) points -= yellowCards;
            if (redCards != null && redCards > 0)       points -= redCards * 3;
        }

        Integer foulsCommitted = discipline.getFoulsCommitted();
        if (foulsCommitted != null && foulsCommitted >= 3) {
            points -= foulsCommitted / 3;
        }

        Integer tackles = defensive.getTackles();
        Integer interceptions = defensive.getInterceptions();
        Integer blocks = defensive.getBlocks();
        Integer duelsWon = defensive.getDuelsWon();

        if (playerType == PlayerType.DEFENDER || playerType == PlayerType.MIDFIELDER) {
            if (tackles != null)       points += tackles / 3;
            if (interceptions != null) points += interceptions / 3;
            if (blocks != null)        points += blocks / 4;
        }

        if (duelsWon != null && duelsWon >= 5) {
            points += 1;
        }

        Boolean cleanSheet = goalkeeper.getCleanSheet();
        if ((playerType == PlayerType.GOALKEEPER || playerType == PlayerType.DEFENDER)
                && minutesPlayed >= 60 && cleanSheet != null && cleanSheet) {
            points += (chip == ChipType.DEFENSIVE_WEEK) ? 8 : 4;
        }
        if (playerType == PlayerType.MIDFIELDER && minutesPlayed >= 60
                && cleanSheet != null && cleanSheet) {
            points += (chip == ChipType.DEFENSIVE_WEEK) ? 2 : 1;
        }

        Integer saves = goalkeeper.getSaves();
        if (playerType == PlayerType.GOALKEEPER && saves != null) {
            if (chip == ChipType.SUPER_SAVES) {
                if (saves >= 2) points += saves / 2;
            } else {
                if (saves >= 3) points += saves / 3;
            }
        }

        if (goals != null && goals >= 3) {
            points += 5;
        }

        Integer penaltiesSaved = goalkeeper.getPenaltiesSaved();
        if (playerType == PlayerType.GOALKEEPER && penaltiesSaved != null && penaltiesSaved > 0) {
            points += penaltiesSaved * 5;
        }

        if (assists != null) {
            if (assists == 2)      points += 3;
            else if (assists >= 3) points += 8;
        }

        Integer goalsConceded = goalkeeper.getGoalsConceded();
        if (playerType == PlayerType.GOALKEEPER && goalsConceded != null && goalsConceded >= 3) {
            points -= 2;
        }
        if (playerType == PlayerType.DEFENDER && goalsConceded != null && goalsConceded >= 3) {
            points -= 1;
        }

        Integer penaltyCommitted = discipline.getPenaltyCommitted();
        Integer penaltyMissed = discipline.getPenaltyMissed();
        if (penaltyCommitted != null && penaltyCommitted > 0) points -= penaltyCommitted * 2;
        if (penaltyMissed    != null && penaltyMissed    > 0) points -= penaltyMissed    * 2;

        return Math.max(points, 0);
    }

    public Map<String, Integer> calculateFantasyPointsBreakdown() {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        if (minutesPlayed >= 60) {
            breakdown.put("minutesPlayed", 3);
            total += 3;
        } else if (minutesPlayed >= 45) {
            breakdown.put("minutesPlayed", 2);
            total += 2;
        } else if (minutesPlayed > 0) {
            breakdown.put("minutesPlayed", 1);
            total += 1;
        }

        Integer goals = shooting.getGoals();
        if (goals != null && goals > 0) {
            int goalPoints = 0;
            switch (playerType) {
                case GOALKEEPER:
                case DEFENDER:
                    goalPoints = goals * 6;
                    break;
                case MIDFIELDER:
                    goalPoints = goals * 5;
                    break;
                case FORWARD:
                    goalPoints = goals * 4;
                    break;
            }
            if (goalPoints != 0) {
                breakdown.put("goals", goalPoints);
                total += goalPoints;
            }
        }

        Integer assists = passing.getAssists();
        if (assists != null && assists > 0) {
            int assistPoints = assists * 3;
            breakdown.put("assists", assistPoints);
            total += assistPoints;
        }

        Integer chancesCreated = passing.getChancesCreated();
        if ((playerType == PlayerType.MIDFIELDER || playerType == PlayerType.FORWARD)
                && chancesCreated != null && chancesCreated >= 3) {
            int chancesPoints = chancesCreated / 3;
            breakdown.put("chancesCreated", chancesPoints);
            total += chancesPoints;
        }

        if (rating != null) {
            int ratingPoints = 0;
            if (rating >= 9.0) {
                ratingPoints = 5;
            } else if (rating >= 8.0) {
                ratingPoints = 3;
            } else if (rating >= 7.0) {
                ratingPoints = 1;
            } else if (rating < 5.0) {
                ratingPoints = -1;
            }
            if (ratingPoints != 0) {
                breakdown.put("ratingBonus", ratingPoints);
                total += ratingPoints;
            }
        }

        Integer yellowCards = discipline.getYellowCards();
        Integer redCards = discipline.getRedCards();
        Integer foulsCommitted = discipline.getFoulsCommitted();

        if (yellowCards != null && yellowCards > 0) {
            int ycPoints = -yellowCards;
            breakdown.put("yellowCards", ycPoints);
            total += ycPoints;
        }
        if (redCards != null && redCards > 0) {
            int rcPoints = -redCards * 3;
            breakdown.put("redCards", rcPoints);
            total += rcPoints;
        }
        if (foulsCommitted != null && foulsCommitted >= 3) {
            int foulsPoints = -(foulsCommitted / 3);
            breakdown.put("foulsCommitted", foulsPoints);
            total += foulsPoints;
        }

        Integer tackles = defensive.getTackles();
        Integer interceptions = defensive.getInterceptions();
        Integer blocks = defensive.getBlocks();
        Integer duelsWon = defensive.getDuelsWon();

        if (playerType == PlayerType.DEFENDER || playerType == PlayerType.MIDFIELDER) {
            int defPoints = 0;
            if (tackles != null) defPoints += tackles / 3;
            if (interceptions != null) defPoints += interceptions / 3;
            if (blocks != null) defPoints += blocks / 4;
            if (defPoints != 0) {
                breakdown.put("defensiveActions", defPoints);
                total += defPoints;
            }
        }

        if (duelsWon != null && duelsWon >= 5) {
            breakdown.put("duelsBonus", 1);
            total += 1;
        }

        Boolean cleanSheet = goalkeeper.getCleanSheet();
        if ((playerType == PlayerType.GOALKEEPER || playerType == PlayerType.DEFENDER)
                && minutesPlayed >= 60) {
            if (cleanSheet != null && cleanSheet) {
                breakdown.put("cleanSheet", 4);
                total += 4;
            }
        }

        if (playerType == PlayerType.MIDFIELDER && minutesPlayed >= 60) {
            if (cleanSheet != null && cleanSheet) {
                breakdown.put("cleanSheetMid", 1);
                total += 1;
            }
        }

        Integer saves = goalkeeper.getSaves();
        if (playerType == PlayerType.GOALKEEPER && saves != null && saves >= 3) {
            int savesPoints = saves / 3;
            breakdown.put("saves", savesPoints);
            total += savesPoints;
        }

        if (goals != null && goals >= 3) {
            breakdown.put("hatTrick", 5);
            total += 5;
        }

        Integer penaltiesSaved = goalkeeper.getPenaltiesSaved();
        if (playerType == PlayerType.GOALKEEPER && penaltiesSaved != null && penaltiesSaved > 0) {
            int penPoints = penaltiesSaved * 5;
            breakdown.put("penaltySaved", penPoints);
            total += penPoints;
        }

        if (assists != null && assists == 2) {
            breakdown.put("doubleAssist", 3);
            total += 3;
        }
        if (assists != null && assists >= 3) {
            breakdown.put("tripleAssist", 8);
            total += 8;
        }

        Integer goalsConceded = goalkeeper.getGoalsConceded();
        if (playerType == PlayerType.GOALKEEPER && goalsConceded != null && goalsConceded >= 3) {
            breakdown.put("multipleGoalsConceded", -2);
            total -= 2;
        }
        if (playerType == PlayerType.DEFENDER && goalsConceded != null && goalsConceded >= 3) {
            breakdown.put("multipleGoalsConcededDef", -1);
            total -= 1;
        }

        Integer penaltyCommitted = discipline.getPenaltyCommitted();
        Integer penaltyMissed = discipline.getPenaltyMissed();
        if (penaltyCommitted != null && penaltyCommitted > 0) {
            int penCommittedPoints = -penaltyCommitted * 2;
            breakdown.put("penaltyCommitted", penCommittedPoints);
            total += penCommittedPoints;
        }
        if (penaltyMissed != null && penaltyMissed > 0) {
            int penMissedPoints = -penaltyMissed * 2;
            breakdown.put("penaltyMissed", penMissedPoints);
            total += penMissedPoints;
        }

        breakdown.put("total", Math.max(total, 0));
        return breakdown;
    }

    public Map<String, Integer> calculateFantasyPointsBreakdownWithChip(String chipName) {
        if (chipName == null) return calculateFantasyPointsBreakdown();
        if (!ChipType.isValid(chipName)) return calculateFantasyPointsBreakdown();
        ChipType chip = ChipType.valueOf(chipName);
        if (chip == ChipType.TRIPLE_CAP || chip == ChipType.BENCH_BOOST) {
            return calculateFantasyPointsBreakdown();
        }

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        if (minutesPlayed >= 60) {
            int pts = (chip == ChipType.GOLDEN_MINUTES) ? 5 : 3;
            breakdown.put("minutesPlayed", pts);
            total += pts;
        } else if (minutesPlayed >= 45) {
            breakdown.put("minutesPlayed", 2);
            total += 2;
        } else if (minutesPlayed > 0) {
            breakdown.put("minutesPlayed", 1);
            total += 1;
        }

        Integer goals = shooting.getGoals();
        if (goals != null && goals > 0) {
            int goalPoints;
            if (chip == ChipType.DOUBLE_GOALS) {
                switch (playerType) {
                    case GOALKEEPER: case DEFENDER: goalPoints = goals * 12; break;
                    case MIDFIELDER:                goalPoints = goals * 10; break;
                    case FORWARD:                   goalPoints = goals * 8;  break;
                    default:                        goalPoints = 0;
                }
            } else if (chip == ChipType.LETHAL_STRIKER) {
                switch (playerType) {
                    case GOALKEEPER: case DEFENDER: goalPoints = goals * 6;  break;
                    case MIDFIELDER:                goalPoints = goals * 5;  break;
                    case FORWARD:                   goalPoints = goals * 12; break;
                    default:                        goalPoints = 0;
                }
            } else {
                switch (playerType) {
                    case GOALKEEPER: case DEFENDER: goalPoints = goals * 6; break;
                    case MIDFIELDER:                goalPoints = goals * 5; break;
                    case FORWARD:                   goalPoints = goals * 4; break;
                    default:                        goalPoints = 0;
                }
            }
            if (goalPoints != 0) {
                breakdown.put("goals", goalPoints);
                total += goalPoints;
            }
        }

        Integer assists = passing.getAssists();
        if (assists != null && assists > 0) {
            int assistPoints = assists * (chip == ChipType.DOUBLE_ASSISTS ? 6 : 3);
            breakdown.put("assists", assistPoints);
            total += assistPoints;
        }

        Integer chancesCreated = passing.getChancesCreated();
        if ((playerType == PlayerType.MIDFIELDER || playerType == PlayerType.FORWARD)
                && chancesCreated != null && chancesCreated > 0) {
            int chancesPoints;
            if (chip == ChipType.CREATIVE_MIDS) {
                chancesPoints = chancesCreated;
            } else if (chancesCreated >= 3) {
                chancesPoints = chancesCreated / 3;
            } else {
                chancesPoints = 0;
            }
            if (chancesPoints != 0) {
                breakdown.put("chancesCreated", chancesPoints);
                total += chancesPoints;
            }
        }

        if (rating != null) {
            int ratingPoints = 0;
            if (rating >= 9.0)      ratingPoints = 5;
            else if (rating >= 8.0) ratingPoints = 3;
            else if (rating >= 7.0) ratingPoints = 1;
            else if (rating < 5.0)  ratingPoints = -1;
            if (ratingPoints != 0) {
                breakdown.put("ratingBonus", ratingPoints);
                total += ratingPoints;
            }
        }

        if (chip != ChipType.NO_PENALTY) {
            Integer yellowCards = discipline.getYellowCards();
            Integer redCards = discipline.getRedCards();
            if (yellowCards != null && yellowCards > 0) {
                breakdown.put("yellowCards", -yellowCards);
                total -= yellowCards;
            }
            if (redCards != null && redCards > 0) {
                breakdown.put("redCards", -redCards * 3);
                total -= redCards * 3;
            }
        }

        Integer foulsCommitted = discipline.getFoulsCommitted();
        if (foulsCommitted != null && foulsCommitted >= 3) {
            int foulsPoints = -(foulsCommitted / 3);
            breakdown.put("foulsCommitted", foulsPoints);
            total += foulsPoints;
        }

        Integer tackles = defensive.getTackles();
        Integer interceptions = defensive.getInterceptions();
        Integer blocks = defensive.getBlocks();
        Integer duelsWon = defensive.getDuelsWon();

        if (playerType == PlayerType.DEFENDER || playerType == PlayerType.MIDFIELDER) {
            int defPoints = 0;
            if (tackles != null)       defPoints += tackles / 3;
            if (interceptions != null) defPoints += interceptions / 3;
            if (blocks != null)        defPoints += blocks / 4;
            if (defPoints != 0) {
                breakdown.put("defensiveActions", defPoints);
                total += defPoints;
            }
        }

        if (duelsWon != null && duelsWon >= 5) {
            breakdown.put("duelsBonus", 1);
            total += 1;
        }

        Boolean cleanSheet = goalkeeper.getCleanSheet();
        if ((playerType == PlayerType.GOALKEEPER || playerType == PlayerType.DEFENDER)
                && minutesPlayed >= 60 && cleanSheet != null && cleanSheet) {
            int csPoints = (chip == ChipType.DEFENSIVE_WEEK) ? 8 : 4;
            breakdown.put("cleanSheet", csPoints);
            total += csPoints;
        }
        if (playerType == PlayerType.MIDFIELDER && minutesPlayed >= 60
                && cleanSheet != null && cleanSheet) {
            int csPoints = (chip == ChipType.DEFENSIVE_WEEK) ? 2 : 1;
            breakdown.put("cleanSheetMid", csPoints);
            total += csPoints;
        }

        Integer saves = goalkeeper.getSaves();
        if (playerType == PlayerType.GOALKEEPER && saves != null) {
            int savesPoints;
            if (chip == ChipType.SUPER_SAVES) {
                savesPoints = (saves >= 2) ? saves / 2 : 0;
            } else {
                savesPoints = (saves >= 3) ? saves / 3 : 0;
            }
            if (savesPoints != 0) {
                breakdown.put("saves", savesPoints);
                total += savesPoints;
            }
        }

        if (goals != null && goals >= 3) {
            breakdown.put("hatTrick", 5);
            total += 5;
        }

        Integer penaltiesSaved = goalkeeper.getPenaltiesSaved();
        if (playerType == PlayerType.GOALKEEPER && penaltiesSaved != null && penaltiesSaved > 0) {
            int penPoints = penaltiesSaved * 5;
            breakdown.put("penaltySaved", penPoints);
            total += penPoints;
        }

        if (assists != null && assists == 2) {
            breakdown.put("doubleAssist", 3);
            total += 3;
        }
        if (assists != null && assists >= 3) {
            breakdown.put("tripleAssist", 8);
            total += 8;
        }

        Integer goalsConceded = goalkeeper.getGoalsConceded();
        if (playerType == PlayerType.GOALKEEPER && goalsConceded != null && goalsConceded >= 3) {
            breakdown.put("multipleGoalsConceded", -2);
            total -= 2;
        }
        if (playerType == PlayerType.DEFENDER && goalsConceded != null && goalsConceded >= 3) {
            breakdown.put("multipleGoalsConcededDef", -1);
            total -= 1;
        }

        Integer penaltyCommitted = discipline.getPenaltyCommitted();
        Integer penaltyMissed = discipline.getPenaltyMissed();
        if (penaltyCommitted != null && penaltyCommitted > 0) {
            int penCommittedPoints = -penaltyCommitted * 2;
            breakdown.put("penaltyCommitted", penCommittedPoints);
            total += penCommittedPoints;
        }
        if (penaltyMissed != null && penaltyMissed > 0) {
            int penMissedPoints = -penaltyMissed * 2;
            breakdown.put("penaltyMissed", penMissedPoints);
            total += penMissedPoints;
        }

        breakdown.put("total", Math.max(total, 0));
        return breakdown;
    }

    public enum PlayerType {
        GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD
    }
}
