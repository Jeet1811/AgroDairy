"""
Quick holdout evaluation for the demand model: trains on all but the last 14
days per product, then reports MAE/RMSE on that held-out window. Not part of
the serving path — a manual sanity check to run after generate_seed_data.py.
"""

import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error, mean_squared_error
from xgboost import XGBRegressor

import psycopg

from train_demand_model import DATABASE_URL, FEATURE_COLUMNS, add_features, build_daily_dataset


def main():
    with psycopg.connect(DATABASE_URL) as conn:
        raw = build_daily_dataset(conn)

    if raw.empty:
        print("No data available to evaluate.")
        return

    dataset = add_features(raw)
    product_ids = sorted(dataset["product_id"].astype(str).unique())
    product_id_to_code = {pid: i for i, pid in enumerate(product_ids)}
    dataset["product_id_encoded"] = dataset["product_id"].astype(str).map(product_id_to_code)
    dataset = dataset.sort_values(["product_id", "d"])

    test_mask = dataset.groupby("product_id")["d"].transform(lambda s: s >= s.max() - pd.Timedelta(days=13))
    train_df, test_df = dataset[~test_mask], dataset[test_mask]

    if train_df.empty or test_df.empty:
        print("Not enough history for a train/test split (need more than 14 days per product).")
        return

    model = XGBRegressor(n_estimators=200, max_depth=5, learning_rate=0.05, objective="reg:squarederror", random_state=42)
    model.fit(train_df[FEATURE_COLUMNS], train_df["quantity"])
    predictions = model.predict(test_df[FEATURE_COLUMNS])

    mae = mean_absolute_error(test_df["quantity"], predictions)
    rmse = np.sqrt(mean_squared_error(test_df["quantity"], predictions))
    print(f"Holdout (last 14 days per product): MAE={mae:.2f}, RMSE={rmse:.2f}, rows={len(test_df)}")


if __name__ == "__main__":
    main()
