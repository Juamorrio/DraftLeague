package com.DraftLeague.models.Statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMatchSummaryDTO {
    // Match info
    private Integer matchId;
    private Integer round;
    private String opponent;
    private String homeTeam;
    private String awayTeam;
    private Boolean isHomeTeam;

    // Match result
    private Integer goalsScored;
    private Integer goalsConceded;

    // Player performance
    private Integer minutesPlayed;
    private Double fotmobRating;
    private Integer fantasyPoints;

    // Offensive stats
    private Integer goals;
    private Integer assists;
    private Integer totalShots;
    private Integer chancesCreated;
    private Double expectedGoals;
    private Double expectedAssists;

    // Passing stats
    private Integer totalPasses;
    private Integer accuratePasses;
    private Integer totalCrosses;
    private Integer accurateCrosses;
    private Double passAccuracy;

    // Physical stats
    private Integer touches;
    private Integer defensiveActions;

    // Defensive stats
    private Integer tackles;
    private Integer interceptions;
    private Integer clearances;
    private Integer blocks;
    private Integer recoveries;

    // Duels
    private Integer duelsWon;
    private Integer duelsLost;

    // Cards
    private Integer yellowCards;
    private Integer redCards;

    // Goalkeeper specific
    private Integer saves;
    private Integer goalkeeperSaves;
    private Integer goalsAllowed;

    // Points breakdown
    private Map<String, Integer> pointsBreakdown;
}
