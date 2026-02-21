from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import pandas as pd
import numpy as np
import os

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Global variables for model and encoders
model = None
label_encoders = None
feature_cols = None

def load_model():
    """
    Load the trained model and label encoders
    """
    global model, label_encoders, feature_cols

    try:
        model = joblib.load('models/random_forest.pkl')
        label_encoders = joblib.load('models/label_encoders.pkl')

        # Load feature columns order (exact order used during training)
        with open('models/feature_columns.txt', 'r') as f:
            feature_cols = [line.strip() for line in f.readlines()]

        print("[+] Model and encoders loaded successfully")
        print(f"[+] Loaded {len(feature_cols)} features in training order")
        return True
    except Exception as e:
        print(f"[!] Error loading model: {e}")
        return False

@app.route('/health', methods=['GET'])
def health_check():
    """
    Health check endpoint
    """
    return jsonify({
        'status': 'healthy',
        'model_loaded': model is not None
    })

@app.route('/train', methods=['POST'])
def train_model():
    """
    Trigger model training
    """
    try:
        import subprocess

        # Run model_trainer.py
        result = subprocess.run(['python', 'model_trainer.py'], capture_output=True, text=True)

        if result.returncode == 0:
            # Reload model after training
            load_model()

            return jsonify({
                'status': 'success',
                'message': 'Model trained successfully',
                'output': result.stdout
            })
        else:
            return jsonify({
                'status': 'error',
                'message': 'Training failed',
                'error': result.stderr
            }), 500

    except Exception as e:
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500

@app.route('/predict', methods=['POST'])
def predict_player():
    """
    Predict fantasy points for a single player
    """
    if model is None:
        return jsonify({
            'error': 'Model not loaded. Please train the model first.'
        }), 503

    try:
        data = request.json

        # Prepare features dataframe
        features_dict = {}

        # Encode categorical features with fallback for unknown labels
        if 'position' in data and 'position' in label_encoders:
            try:
                features_dict['position'] = label_encoders['position'].transform([data.get('position', 'DEF')])[0]
            except ValueError:
                # If label is unknown, use the most common class (index 0)
                print(f"[!] Unknown position '{data.get('position')}', using default")
                features_dict['position'] = 0
        if 'playerType' in data and 'playerType' in label_encoders:
            try:
                features_dict['playerType'] = label_encoders['playerType'].transform([data.get('playerType', 'DEFENDER')])[0]
            except ValueError:
                print(f"[!] Unknown playerType '{data.get('playerType')}', using default")
                features_dict['playerType'] = 0
        if 'isHomeTeam' in data and 'isHomeTeam' in label_encoders:
            try:
                features_dict['isHomeTeam'] = label_encoders['isHomeTeam'].transform([str(data.get('isHomeTeam', True))])[0]
            except ValueError:
                print(f"[!] Unknown isHomeTeam '{data.get('isHomeTeam')}', using default")
                features_dict['isHomeTeam'] = 0

        # Add numerical features
        numerical_features = [
            'round',
            # Last 3
            'avgRatingLast3', 'avgMinutesLast3', 'avgGoalsLast3', 'avgAssistsLast3',
            'avgXgLast3', 'avgXaLast3', 'avgPassAccuracyLast3', 'avgTouchesLast3', 'avgDuelsWonLast3',
            # Last 5
            'avgRatingLast5', 'avgMinutesLast5', 'avgGoalsLast5', 'avgAssistsLast5',
            'avgXgLast5', 'avgXaLast5',
            # Last 10
            'avgRatingLast10', 'avgMinutesLast10',
            # Trends
            'ratingTrend', 'minutesTrend',
            # Consistency
            'ratingStdDev', 'pointsStdDev',
            # Recent form
            'recentFormPoints',
            # Home/Away
            'avgRatingHome', 'avgRatingAway', 'homeAdvantage',
            # Season totals
            'matchesPlayed', 'seasonAvgRating', 'totalSeasonPoints'
        ]

        for feature in numerical_features:
            features_dict[feature] = data.get(feature, 0.0)

        # Create DataFrame with correct feature order
        X = pd.DataFrame([features_dict])

        print(f"[*] Features received: {sorted(X.columns.tolist())}")
        print(f"[*] Expected features: {sorted(feature_cols)}")
        print(f"[*] Missing features: {set(feature_cols) - set(X.columns)}")
        print(f"[*] Extra features: {set(X.columns) - set(feature_cols)}")

        # Ensure all required features are present
        for col in feature_cols:
            if col not in X.columns:
                print(f"[!] Adding missing feature '{col}' with default value 0")
                X[col] = 0

        # Reorder columns to match training - create new DataFrame with exact order
        X = X[feature_cols]

        print(f"[+] Final feature order: {list(X.columns)}")
        print(f"[+] Matches training order: {list(X.columns) == feature_cols}")

        # Make prediction - convert to numpy to avoid feature name checking
        prediction = model.predict(X.values)[0]

        # Calculate confidence interval (using prediction variance from trees)
        # Get individual tree predictions
        tree_predictions = np.array([tree.predict(X.values)[0] for tree in model.estimators_])
        std = np.std(tree_predictions)

        confidence_interval = [
            int(max(0, prediction - 1.96 * std)),
            int(prediction + 1.96 * std)
        ]

        # Feature importance for this prediction
        feature_importance = dict(zip(feature_cols, model.feature_importances_))
        # Sort and get top 10
        sorted_importance = dict(sorted(feature_importance.items(), key=lambda x: x[1], reverse=True)[:10])

        response = {
            'playerId': data.get('playerId'),
            'predictedPoints': round(prediction, 1),
            'confidence_interval': confidence_interval,
            'features_importance': sorted_importance
        }

        return jsonify(response)

    except Exception as e:
        return jsonify({
            'error': f'Prediction failed: {str(e)}'
        }), 500

@app.route('/predict/team', methods=['POST'])
def predict_team():
    """
    Predict fantasy points for an entire team
    """
    if model is None:
        return jsonify({
            'error': 'Model not loaded. Please train the model first.'
        }), 503

    try:
        data = request.json
        team_id = data.get('teamId')
        players = data.get('players', [])

        if not players:
            return jsonify({
                'error': 'No players provided'
            }), 400

        # Predict for each player
        player_predictions = []
        total_predicted = 0

        for player_data in players:
            # Make individual prediction (reuse predict logic)
            features_dict = {}

            # Encode categorical
            if 'position' in player_data and 'position' in label_encoders:
                features_dict['position'] = label_encoders['position'].transform([player_data.get('position', 'DEF')])[0]
            if 'playerType' in player_data and 'playerType' in label_encoders:
                features_dict['playerType'] = label_encoders['playerType'].transform([player_data.get('playerType', 'DEFENDER')])[0]
            if 'isHomeTeam' in player_data and 'isHomeTeam' in label_encoders:
                features_dict['isHomeTeam'] = label_encoders['isHomeTeam'].transform([str(player_data.get('isHomeTeam', True))])[0]

            # Add numerical features
            numerical_features = [
                'round', 'avgRatingLast3', 'avgMinutesLast3', 'avgGoalsLast3', 'avgAssistsLast3',
                'avgXgLast3', 'avgXaLast3', 'avgPassAccuracyLast3', 'avgTouchesLast3', 'avgDuelsWonLast3',
                'avgRatingLast5', 'avgMinutesLast5', 'avgGoalsLast5', 'avgAssistsLast5',
                'avgXgLast5', 'avgXaLast5', 'avgRatingLast10', 'avgMinutesLast10',
                'ratingTrend', 'minutesTrend', 'ratingStdDev', 'pointsStdDev',
                'recentFormPoints', 'avgRatingHome', 'avgRatingAway', 'homeAdvantage',
                'matchesPlayed', 'seasonAvgRating', 'totalSeasonPoints'
            ]

            for feature in numerical_features:
                features_dict[feature] = player_data.get(feature, 0.0)

            X = pd.DataFrame([features_dict])

            # Ensure all features present
            for col in feature_cols:
                if col not in X.columns:
                    X[col] = 0

            X = X[feature_cols]

            # Predict
            prediction = model.predict(X)[0]
            total_predicted += prediction

            player_predictions.append({
                'playerId': player_data.get('playerId'),
                'name': player_data.get('playerName'),
                'position': player_data.get('position'),
                'predicted': round(prediction, 1)
            })

        # Calculate team confidence interval from individual tree predictions
        all_team_tree_preds = np.zeros(len(model.estimators_))
        for player_data_ci in players:
            features_dict_ci = {}
            if 'position' in player_data_ci and 'position' in label_encoders:
                try:
                    features_dict_ci['position'] = label_encoders['position'].transform([player_data_ci.get('position', 'DEF')])[0]
                except ValueError:
                    features_dict_ci['position'] = 0
            if 'playerType' in player_data_ci and 'playerType' in label_encoders:
                try:
                    features_dict_ci['playerType'] = label_encoders['playerType'].transform([player_data_ci.get('playerType', 'DEFENDER')])[0]
                except ValueError:
                    features_dict_ci['playerType'] = 0
            if 'isHomeTeam' in player_data_ci and 'isHomeTeam' in label_encoders:
                try:
                    features_dict_ci['isHomeTeam'] = label_encoders['isHomeTeam'].transform([str(player_data_ci.get('isHomeTeam', True))])[0]
                except ValueError:
                    features_dict_ci['isHomeTeam'] = 0
            for feat in numerical_features:
                features_dict_ci[feat] = player_data_ci.get(feat, 0.0)
            X_ci = pd.DataFrame([features_dict_ci])
            for col in feature_cols:
                if col not in X_ci.columns:
                    X_ci[col] = 0
            X_ci = X_ci[feature_cols]
            tree_preds = np.array([tree.predict(X_ci.values)[0] for tree in model.estimators_])
            all_team_tree_preds += tree_preds

        team_std = np.std(all_team_tree_preds)
        confidence_interval = [
            int(max(0, total_predicted - 1.96 * team_std)),
            int(total_predicted + 1.96 * team_std)
        ]

        response = {
            'teamId': team_id,
            'totalPredictedPoints': round(total_predicted, 1),
            'players': player_predictions,
            'confidence_interval': confidence_interval
        }

        return jsonify(response)

    except Exception as e:
        return jsonify({
            'error': f'Team prediction failed: {str(e)}'
        }), 500

if __name__ == '__main__':
    print("[*] Starting DraftLeague ML Service...")

    # Load model on startup
    if os.path.exists('models/random_forest.pkl'):
        load_model()
    else:
        print("[!] No trained model found. Please train the model first using model_trainer.py")

    app.run(host='0.0.0.0', port=5000, debug=True)
