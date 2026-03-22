"""
Trains an XGBoost regression model to predict total_fantasy_points from player statistics.
Reads data directly from MariaDB, engineers features, trains the model, and saves it as a .pkl file.
"""

import os
import json
import logging
from datetime import datetime

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import mean_squared_error, mean_absolute_error
from sklearn.model_selection import train_test_split
from sqlalchemy import create_engine, text
from xgboost import XGBRegressor

from model.feature_engineering import build_feature_matrix, FEATURE_NAMES

logger = logging.getLogger(__name__)

MODEL_PATH = os.getenv("MODEL_PATH", "/app/model_storage/xgboost_model.pkl")
DB_URL = os.getenv("DB_URL", "mysql+pymysql://root:root@localhost:3306/draftleague")


def _load_data(engine) -> tuple[pd.DataFrame, pd.DataFrame]:
    stats_query = text("""
        SELECT
            ps.id,
            ps.player_id,
            ps.match_id,
            ps.is_home_team,
            ps.player_type,
            ps.rating,
            ps.minutes_played,
            ps.goals,
            ps.assists,
            ps.shots_on_target,
            ps.tackles,
            ps.blocks,
            ps.saves,
            ps.goals_conceded,
            ps.clean_sheet,
            ps.yellow_cards,
            ps.red_cards,
            ps.total_fantasy_points
        FROM player_statistic ps
        WHERE ps.total_fantasy_points IS NOT NULL
          AND ps.minutes_played >= 1
    """)
    matches_query = text("""
        SELECT id, home_team_id, away_team_id, home_goals, away_goals
        FROM matches
        WHERE home_goals IS NOT NULL AND away_goals IS NOT NULL
    """)
    with engine.connect() as conn:
        stats_df = pd.read_sql(stats_query, conn)
        matches_df = pd.read_sql(matches_query, conn)
    return stats_df, matches_df


def train(gameweek: int = 0) -> dict:
    """
    Full training pipeline. Returns a dict with training metrics.
    """
    logger.info("Starting XGBoost training (gameweek=%d)...", gameweek)

    engine = create_engine(DB_URL, pool_pre_ping=True)
    stats_df, matches_df = _load_data(engine)
    engine.dispose()

    n_rows = len(stats_df)
    logger.info("Loaded %d player-statistic rows", n_rows)

    if n_rows < 50:
        raise ValueError(f"Insufficient data for training: only {n_rows} rows found. Need at least 50.")

    X = build_feature_matrix(stats_df, matches_df)
    y = stats_df["total_fantasy_points"].values.astype(float)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    model = XGBRegressor(
        n_estimators=300,
        max_depth=6,
        learning_rate=0.05,
        subsample=0.8,
        colsample_bytree=0.8,
        random_state=42,
        eval_metric="rmse",
        verbosity=0,
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    rmse = float(np.sqrt(mean_squared_error(y_test, y_pred)))
    mae = float(mean_absolute_error(y_test, y_pred))
    logger.info("Training complete — RMSE=%.3f  MAE=%.3f  rows=%d", rmse, mae, n_rows)

    # Save model + metadata atomically
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    tmp_path = MODEL_PATH + ".tmp"
    joblib.dump({"model": model, "features": FEATURE_NAMES}, tmp_path)
    os.replace(tmp_path, MODEL_PATH)

    metadata = {
        "trained_at": datetime.utcnow().isoformat() + "Z",
        "gameweek": gameweek,
        "rows": n_rows,
        "rmse": rmse,
        "mae": mae,
    }
    meta_path = MODEL_PATH.replace(".pkl", "_metadata.json")
    with open(meta_path, "w") as f:
        json.dump(metadata, f, indent=2)

    logger.info("Model saved to %s", MODEL_PATH)
    return metadata
