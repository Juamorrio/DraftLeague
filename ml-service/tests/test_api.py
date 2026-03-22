"""
API-level tests for the FastAPI ML service.
Uses FastAPI TestClient (httpx) — no real DB or model needed for most tests.
"""

import os
import pytest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient


@pytest.fixture(autouse=True)
def set_env(tmp_path, monkeypatch):
    monkeypatch.setenv("MODEL_PATH", str(tmp_path / "xgboost_model.pkl"))
    monkeypatch.setenv("INTERNAL_API_KEY", "test-secret")
    monkeypatch.setenv("DB_URL", "sqlite:///:memory:")


@pytest.fixture()
def client():
    # Import app after env vars are set
    from main import app
    with TestClient(app, raise_server_exceptions=False) as c:
        yield c


def test_health_returns_ok(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "ok"
    assert "model_loaded" in data


def test_predict_without_model_returns_503(client):
    with patch("model.predictor.is_loaded", return_value=False):
        resp = client.post("/predict/player/p1", json={})
    assert resp.status_code == 503


def test_train_without_internal_key_returns_403(client):
    resp = client.post("/train", json={"gameweek": 1})
    assert resp.status_code == 403


def test_train_with_wrong_key_returns_403(client):
    resp = client.post("/train", json={"gameweek": 1}, headers={"X-Internal-Key": "wrong"})
    assert resp.status_code == 403


def test_predict_with_model_loaded(client):
    with patch("model.predictor.is_loaded", return_value=True), \
         patch("model.predictor.predict", return_value=7.5), \
         patch("model.predictor.get_feature_importance", return_value={"rating": 1.0}):
        resp = client.post("/predict/player/p42", json={
            "rating": 7.5, "minutes_played": 90, "goals": 1,
            "assists": 0, "shots_on_target": 3, "tackles": 2,
            "blocks": 0, "saves": 0, "goals_conceded": 0,
            "clean_sheet": 0, "yellow_cards": 0, "red_cards": 0,
            "position_encoded": 3, "is_home_team": 1,
            "recent_form_last3": 6.0, "opponent_strength": 0.9
        })
    assert resp.status_code == 200
    data = resp.json()
    assert data["predictedPoints"] == 7.5
    assert data["modelSource"] == "XGBOOST"
    assert data["playerId"] == "p42"


def test_feature_importance_without_model_returns_503(client):
    with patch("model.predictor.is_loaded", return_value=False):
        resp = client.get("/feature-importance")
    assert resp.status_code == 503
