"""
Unit tests for the predictor module.
"""

import os
import pytest
from unittest.mock import MagicMock, patch
import numpy as np


def test_predict_returns_float_in_range(tmp_path, monkeypatch):
    monkeypatch.setenv("MODEL_PATH", str(tmp_path / "model.pkl"))

    mock_model = MagicMock()
    mock_model.predict.return_value = np.array([8.3])
    mock_model.get_booster.return_value.get_score.return_value = {"rating": 100.0, "goals": 80.0}

    import model.predictor as pred
    pred._model = mock_model

    result = pred.predict({"rating": 7.5, "minutes_played": 90})
    assert 0.0 <= result <= 50.0
    assert isinstance(result, float)

    # cleanup
    pred._model = None


def test_predict_clamps_negative_to_zero(tmp_path, monkeypatch):
    monkeypatch.setenv("MODEL_PATH", str(tmp_path / "model.pkl"))

    mock_model = MagicMock()
    mock_model.predict.return_value = np.array([-5.0])

    import model.predictor as pred
    pred._model = mock_model

    result = pred.predict({})
    assert result == 0.0
    pred._model = None


def test_predict_clamps_above_50(tmp_path, monkeypatch):
    monkeypatch.setenv("MODEL_PATH", str(tmp_path / "model.pkl"))

    mock_model = MagicMock()
    mock_model.predict.return_value = np.array([99.0])

    import model.predictor as pred
    pred._model = mock_model

    result = pred.predict({})
    assert result == 50.0
    pred._model = None


def test_feature_importance_normalized_max_is_one(tmp_path, monkeypatch):
    monkeypatch.setenv("MODEL_PATH", str(tmp_path / "model.pkl"))

    mock_model = MagicMock()
    mock_model.predict.return_value = np.array([5.0])
    mock_model.get_booster.return_value.get_score.return_value = {
        "rating": 200.0, "goals": 100.0, "assists": 50.0
    }

    import model.predictor as pred
    pred._model = mock_model

    importance = pred.get_feature_importance()
    assert importance["rating"] == 1.0
    assert importance["goals"] == 0.5
    assert all(0.0 <= v <= 1.0 for v in importance.values())
    pred._model = None


def test_model_not_loaded_raises_runtime_error(tmp_path, monkeypatch):
    monkeypatch.setenv("MODEL_PATH", str(tmp_path / "nonexistent.pkl"))

    import model.predictor as pred
    pred._model = None

    with pytest.raises(RuntimeError, match="Model file not found"):
        pred.ensure_loaded()
