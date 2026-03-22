"""
Loads the trained XGBoost model and exposes predict() and get_feature_importance().
Thread-safe singleton pattern — model is loaded once on first use.
"""

import os
import logging
import threading
from typing import Optional

import joblib
import numpy as np
import pandas as pd

from model.feature_engineering import FEATURE_NAMES, build_inference_features

logger = logging.getLogger(__name__)

MODEL_PATH = os.getenv("MODEL_PATH", "/app/model_storage/xgboost_model.pkl")

_model = None
_feature_names: list[str] = FEATURE_NAMES
_lock = threading.Lock()


def _load_model():
    global _model, _feature_names
    if not os.path.exists(MODEL_PATH):
        raise RuntimeError(f"Model file not found at {MODEL_PATH}. Run /train first.")
    payload = joblib.load(MODEL_PATH)
    _model = payload["model"]
    _feature_names = payload.get("features", FEATURE_NAMES)
    logger.info("XGBoost model loaded from %s", MODEL_PATH)


def ensure_loaded():
    global _model
    if _model is None:
        with _lock:
            if _model is None:
                _load_model()


def reload():
    """Force reload of the model from disk (called after retraining)."""
    global _model
    with _lock:
        _model = None
        _load_model()


def is_loaded() -> bool:
    return _model is not None


def predict(features: dict) -> float:
    """
    Predict total fantasy points for a single player-match feature vector.
    Returns a float clamped to [0, 50].
    """
    ensure_loaded()
    X = build_inference_features(features)
    # Reorder columns to match training feature order
    X = X[_feature_names]
    raw = float(_model.predict(X)[0])
    return float(np.clip(raw, 0.0, 50.0))


def get_feature_importance() -> dict[str, float]:
    """
    Returns gain-based feature importance normalized to [0, 1].
    """
    ensure_loaded()
    scores = _model.get_booster().get_score(importance_type="gain")
    if not scores:
        return {f: 0.0 for f in _feature_names}
    max_score = max(scores.values())
    if max_score == 0:
        return {f: 0.0 for f in _feature_names}
    normalized = {k: round(v / max_score, 4) for k, v in scores.items()}
    # Ensure all features are present (some may have 0 importance)
    return {f: normalized.get(f, 0.0) for f in _feature_names}
