import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import cross_val_score, train_test_split
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.preprocessing import LabelEncoder
import joblib
import os

def load_and_preprocess_data(csv_path='data/training_data.csv'):
    """
    Load and preprocess training data from CSV
    """
    print(f"[*] Loading training data from {csv_path}")
    df = pd.read_csv(csv_path)

    print(f"[+] Loaded {len(df)} training samples")

    # Convert targetFantasyPoints to numeric, replacing errors with NaN
    df['targetFantasyPoints'] = pd.to_numeric(df['targetFantasyPoints'], errors='coerce')

    # Remove rows with invalid target values (NaN or null)
    df = df[df['targetFantasyPoints'].notna()]

    print(f"[+] After cleaning: {len(df)} valid training samples")

    # Handle missing values in other columns
    df = df.fillna(0)

    # Encode categorical features
    label_encoders = {}
    categorical_cols = ['position', 'playerType', 'isHomeTeam']

    for col in categorical_cols:
        if col in df.columns:
            le = LabelEncoder()
            df[col] = le.fit_transform(df[col].astype(str))
            label_encoders[col] = le

    # Save label encoders for later use
    joblib.dump(label_encoders, 'models/label_encoders.pkl')

    return df, label_encoders

def prepare_features_and_target(df):
    """
    Separate features (X) and target (y) from dataframe
    """
    # Features to use (exclude identifiers and target)
    feature_cols = [
        'position', 'playerType', 'round', 'isHomeTeam',
        # Last 3
        'avgRatingLast3', 'avgMinutesLast3', 'avgGoalsLast3', 'avgAssistsLast3',
        'avgShotsOnTargetLast3', 'avgKeyPassesLast3', 'avgPassAccuracyLast3', 'avgDuelsWonLast3',
        # Last 5
        'avgRatingLast5', 'avgMinutesLast5', 'avgGoalsLast5', 'avgAssistsLast5',
        'avgShotsOnTargetLast5', 'avgKeyPassesLast5',
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

    # Filter to only existing columns
    feature_cols = [col for col in feature_cols if col in df.columns]

    X = df[feature_cols]
    y = df['targetFantasyPoints']

    print(f"[*] Using {len(feature_cols)} features")
    print(f"    Features: {', '.join(feature_cols[:5])}...")

    return X, y, feature_cols

def train_model(X, y):
    """
    Train Random Forest Regressor model
    """
    print("\n[*] Training Random Forest model...")

    # Split data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    # Initialize model
    model = RandomForestRegressor(
        n_estimators=100,
        max_depth=15,
        min_samples_split=10,
        min_samples_leaf=4,
        random_state=42,
        n_jobs=-1
    )

    # Train model
    model.fit(X_train, y_train)

    # Evaluate on test set
    y_pred = model.predict(X_test)
    mae = mean_absolute_error(y_test, y_pred)
    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
    r2 = r2_score(y_test, y_pred)

    print(f"\n[+] Model Performance on Test Set:")
    print(f"    MAE (Mean Absolute Error):  {mae:.2f} points")
    print(f"    RMSE (Root Mean Squared):    {rmse:.2f} points")
    print(f"    R2 Score:                    {r2:.3f}")

    # Cross-validation
    print("\n[*] Performing 5-fold cross-validation...")
    cv_scores = cross_val_score(model, X, y, cv=5, scoring='neg_mean_absolute_error', n_jobs=-1)
    cv_mae = -cv_scores.mean()
    cv_std = cv_scores.std()

    print(f"    Cross-validation MAE: {cv_mae:.2f} +/- {cv_std:.2f} points")

    # Feature importance
    feature_importance = pd.DataFrame({
        'feature': X.columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)

    print(f"\n[*] Top 10 Most Important Features:")
    for idx, row in feature_importance.head(10).iterrows():
        print(f"    {row['feature']:25s} {row['importance']:.4f}")

    return model, feature_importance

def save_model(model, feature_importance, feature_cols):
    """
    Save trained model, feature importance, and feature column order
    """
    os.makedirs('models', exist_ok=True)

    # Save model
    joblib.dump(model, 'models/random_forest.pkl')
    print(f"\n[+] Model saved to models/random_forest.pkl")

    # Save feature importance (sorted by importance for human readability)
    feature_importance.to_csv('models/feature_importance.csv', index=False)
    print(f"[+] Feature importance saved to models/feature_importance.csv")

    # Save feature columns order (exact order used during training)
    with open('models/feature_columns.txt', 'w') as f:
        for col in feature_cols:
            f.write(f"{col}\n")
    print(f"[+] Feature column order saved to models/feature_columns.txt")

def main():
    """
    Main training pipeline
    """
    print("=" * 60)
    print("[*] DraftLeague ML Model Trainer")
    print("=" * 60)

    # Load data
    df, label_encoders = load_and_preprocess_data()

    # Prepare features and target
    X, y, feature_cols = prepare_features_and_target(df)

    # Train model
    model, feature_importance = train_model(X, y)

    # Save model
    save_model(model, feature_importance, feature_cols)

    print("\n" + "=" * 60)
    print("[+] Training completed successfully!")
    print("=" * 60)

if __name__ == '__main__':
    main()
