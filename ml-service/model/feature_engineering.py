"""
Feature engineering for the XGBoost fantasy points prediction model.
Converts raw player_statistic rows into the 16-feature vector used for training and inference.
"""

import pandas as pd
import numpy as np

# Ordered list of the 16 features — order must match between training and inference
FEATURE_NAMES = [
    "rating",
    "minutes_played",
    "goals",
    "assists",
    "shots_on_target",
    "tackles",
    "blocks",
    "saves",
    "goals_conceded",
    "clean_sheet",
    "yellow_cards",
    "red_cards",
    "position_encoded",
    "is_home_team",
    "recent_form_last3",
    "opponent_strength",
]

# Encoding for player_type enum values stored in the DB
POSITION_ENCODING = {
    "GOALKEEPER": 0,
    "DEFENDER": 1,
    "MIDFIELDER": 2,
    "FORWARD": 3,
}


def encode_position(player_type: str) -> int:
    return POSITION_ENCODING.get(str(player_type).upper(), 2)


def compute_recent_form(df: pd.DataFrame) -> pd.Series:
    """
    Compute per-player rolling average of total_fantasy_points over the last 3 matches
    (excluding the current row to avoid data leakage).
    Assumes df is sorted by player_id and match date/id ascending.
    """
    return (
        df.groupby("player_id")["total_fantasy_points"]
        .transform(lambda x: x.shift(1).rolling(3, min_periods=1).mean())
        .fillna(0.0)
    )


def compute_opponent_strength(stats_df: pd.DataFrame, matches_df: pd.DataFrame) -> pd.Series:
    """
    Opponent strength = opponent's average goals conceded per match in their last 6 matches,
    normalized by the league-wide average goals per match.

    stats_df must have columns: match_id, player_id, is_home_team
    matches_df must have columns: id, home_team_id, away_team_id, home_goals, away_goals
    """
    # Goals conceded by each team in each match
    home_conceded = matches_df[["id", "away_team_id", "home_goals"]].rename(
        columns={"id": "match_id", "away_team_id": "team_id", "home_goals": "goals_conceded_by_team"}
    )
    away_conceded = matches_df[["id", "home_team_id", "away_goals"]].rename(
        columns={"id": "match_id", "home_team_id": "team_id", "away_goals": "goals_conceded_by_team"}
    )
    all_conceded = pd.concat([home_conceded, away_conceded], ignore_index=True)

    # Rolling average conceded per team (last 6 matches)
    all_conceded = all_conceded.sort_values("match_id")
    all_conceded["rolling_conceded"] = (
        all_conceded.groupby("team_id")["goals_conceded_by_team"]
        .transform(lambda x: x.shift(1).rolling(6, min_periods=1).mean())
        .fillna(1.0)
    )
    team_conceded_map = all_conceded.set_index(["match_id", "team_id"])["rolling_conceded"].to_dict()

    league_avg = matches_df["home_goals"].mean() + matches_df["away_goals"].mean()
    league_avg = max(league_avg / 2, 0.5)

    # For each stat row, find the opponent team id
    merged = stats_df[["match_id", "is_home_team"]].copy()
    merged = merged.merge(
        matches_df[["id", "home_team_id", "away_team_id"]].rename(columns={"id": "match_id"}),
        on="match_id",
        how="left",
    )
    merged["opponent_team_id"] = merged.apply(
        lambda r: r["away_team_id"] if r["is_home_team"] else r["home_team_id"], axis=1
    )

    def lookup_strength(row):
        key = (row["match_id"], row["opponent_team_id"])
        raw = team_conceded_map.get(key, league_avg)
        return raw / league_avg

    return merged.apply(lookup_strength, axis=1).fillna(1.0)


def _to_int(val) -> int:
    """Convert MariaDB BIT/BOOLEAN bytes (b'\\x01') or plain booleans to int."""
    if isinstance(val, (bytes, bytearray)):
        return int.from_bytes(val, "big")
    return int(val) if val is not None else 0


def build_feature_matrix(stats_df: pd.DataFrame, matches_df: pd.DataFrame) -> pd.DataFrame:
    """
    Build the full 16-feature DataFrame from raw DB tables.
    Used during training.
    """
    df = stats_df.copy()
    df = df.sort_values(["player_id", "match_id"]).reset_index(drop=True)

    df["position_encoded"] = df["player_type"].apply(encode_position)
    df["is_home_team"] = df["is_home_team"].apply(_to_int)
    df["clean_sheet"] = df["clean_sheet"].fillna(0).apply(_to_int)
    df["rating"] = df["rating"].fillna(6.0)
    df["recent_form_last3"] = compute_recent_form(df)
    df["opponent_strength"] = compute_opponent_strength(df, matches_df).values

    # Fill remaining nulls with 0
    for col in FEATURE_NAMES:
        if col not in df.columns:
            df[col] = 0
        df[col] = df[col].fillna(0)

    return df[FEATURE_NAMES]


def build_inference_features(raw_features: dict) -> pd.DataFrame:
    """
    Build a single-row DataFrame for inference from a dict of raw feature values.
    The dict keys should match FEATURE_NAMES (or their raw equivalents).
    """
    row = {}
    for feat in FEATURE_NAMES:
        row[feat] = float(raw_features.get(feat, 0.0))
    return pd.DataFrame([row], columns=FEATURE_NAMES)
