package com.DraftLeague.models.Team;

/**
 * Enum representing all available chip types in the fantasy league.
 * Each chip can be used once per season. Only one chip can be active per gameweek.
 */
public enum ChipType {

    /** Captain scores 3× points instead of 2× */
    TRIPLE_CAP,

    /** All goals worth double points (GK/DEF 12pt, MID 10pt, FWD 8pt) */
    DOUBLE_GOALS,

    /** GK saves: 1pt every 2 saves instead of every 3 */
    SUPER_SAVES,

    /** Yellow and red cards do not subtract points */
    NO_PENALTY,

    /** Assists worth 6pts instead of 3pts */
    DOUBLE_ASSISTS,

    /** Clean sheets worth double (GK/DEF 8pt, MID 2pt) */
    DEFENSIVE_WEEK,

    /** FWD goals worth 12pt instead of 4pt */
    LETHAL_STRIKER,

    /** chancesCreated gives 1pt per chance (no ÷3 threshold), for MID/FWD */
    CREATIVE_MIDS,

    /** Playing ≥60 minutes gives +5pts instead of +3pts */
    GOLDEN_MINUTES,

    /** Bench players also contribute their points to the team total */
    BENCH_BOOST;

    /**
     * Returns true if the given string matches a defined chip type.
     */
    public static boolean isValid(String chip) {
        if (chip == null) return false;
        for (ChipType ct : values()) {
            if (ct.name().equals(chip)) return true;
        }
        return false;
    }

    /**
     * Returns true if this chip modifies stat-level calculation
     * (handled in PlayerStatistic), as opposed to service-level logic.
     */
    public boolean isStatLevel() {
        return this != TRIPLE_CAP && this != BENCH_BOOST;
    }
}
