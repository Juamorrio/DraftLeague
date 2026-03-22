"""
DraftLeague ML Service — FastAPI application.
Exposes XGBoost prediction and training endpoints called from the Spring Boot backend.
"""

import json
import logging
import os
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, Header, HTTPException, Body
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from model import predictor, trainer
from model.feature_engineering import FEATURE_NAMES

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s — %(message)s")
logger = logging.getLogger(__name__)

INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY", "")
MODEL_PATH = os.getenv("MODEL_PATH", "/app/model_storage/xgboost_model.pkl")
META_PATH = MODEL_PATH.replace(".pkl", "_metadata.json")


# ── Startup ──────────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    """On startup: auto-train if model file doesn't exist yet."""
    if not os.path.exists(MODEL_PATH):
        logger.info("No model found at %s — running initial training...", MODEL_PATH)
        try:
            trainer.train(gameweek=0)
            predictor.reload()
        except Exception as e:
            logger.warning("Initial training failed (no data yet?): %s", e)
    else:
        try:
            predictor.ensure_loaded()
        except Exception as e:
            logger.warning("Could not load model on startup: %s", e)
    yield


app = FastAPI(title="DraftLeague ML Service", lifespan=lifespan)


# ── Schemas ──────────────────────────────────────────────────────────────────

class PredictRequest(BaseModel):
    rating: float = 6.0
    minutes_played: float = 90.0
    goals: float = 0.0
    assists: float = 0.0
    shots_on_target: float = 0.0
    tackles: float = 0.0
    blocks: float = 0.0
    saves: float = 0.0
    goals_conceded: float = 0.0
    clean_sheet: float = 0.0
    yellow_cards: float = 0.0
    red_cards: float = 0.0
    position_encoded: float = 2.0
    is_home_team: float = 0.0
    recent_form_last3: float = 0.0
    opponent_strength: float = 1.0


class TrainRequest(BaseModel):
    gameweek: int = 0


# ── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    metadata = {}
    if os.path.exists(META_PATH):
        try:
            with open(META_PATH) as f:
                metadata = json.load(f)
        except Exception:
            pass
    return {
        "status": "ok",
        "model_loaded": predictor.is_loaded(),
        "model_path": MODEL_PATH,
        "metadata": metadata,
    }


@app.post("/predict/player/{player_id}")
def predict_player(player_id: str, request: PredictRequest):
    """
    Predict fantasy points for a player given their feature vector.
    Returns predicted points and normalized feature importance.
    """
    if not predictor.is_loaded():
        raise HTTPException(status_code=503, detail="Model not loaded. Call /train first.")

    features = request.model_dump()
    try:
        predicted = predictor.predict(features)
        importance = predictor.get_feature_importance()
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))

    return {
        "playerId": player_id,
        "predictedPoints": round(predicted, 2),
        "featuresImportance": importance,
        "modelSource": "XGBOOST",
    }


@app.get("/feature-importance")
def feature_importance():
    """Returns the normalized gain-based feature importance of the loaded model."""
    if not predictor.is_loaded():
        raise HTTPException(status_code=503, detail="Model not loaded.")
    return predictor.get_feature_importance()


@app.post("/train")
def train_model(
    request: TrainRequest = Body(default=TrainRequest()),
    x_internal_key: Optional[str] = Header(default=None, alias="X-Internal-Key"),
):
    """
    Triggers model retraining. Protected by X-Internal-Key header.
    """
    if INTERNAL_API_KEY and x_internal_key != INTERNAL_API_KEY:
        raise HTTPException(status_code=403, detail="Invalid or missing X-Internal-Key header.")

    try:
        metadata = trainer.train(gameweek=request.gameweek)
        predictor.reload()
        return {"status": "trained", **metadata}
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    except Exception as e:
        logger.exception("Training failed")
        raise HTTPException(status_code=500, detail=f"Training error: {e}")
