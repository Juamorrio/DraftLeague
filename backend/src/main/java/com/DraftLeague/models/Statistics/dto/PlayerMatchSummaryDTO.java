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
    private Double rating;
    private Integer fantasyPoints;

    // Offensive stats
    private Integer goals;
    private Integer assists;
    private Integer totalShots;
    private Integer shotsOnTarget;
    private Integer chancesCreated;

    // Passing stats
    private Integer totalPasses;
    private Integer accuratePasses;
    private Double passAccuracy;

    // Defensive stats
    private Integer tackles;
    private Integer interceptions;
    private Integer blocks;

    // Duels
    private Integer duelsWon;
    private Integer duelsLost;

    // Cards
    private Integer yellowCards;
    private Integer redCards;

    // Goalkeeper specific
    private Integer saves;
    private Integer goalsAllowed;

    // Captain and jersey info
    private Boolean isCaptain;
    private Integer shirtNumber;

    // Penalty discipline
    private Integer penaltyCommitted;

    // Points breakdown
    private Map<String, Integer> pointsBreakdown;
}
