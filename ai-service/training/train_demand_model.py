"""
Trains the demand-forecasting model: a single XGBoost regressor covering all
products, with product_id as a categorical feature (simpler to maintain than
one model per product, per the build spec).

Target: daily quantity sold per product, combining realized orders and
subscription-driven deliveries. Reads DATABASE_URL from the environment.
Run generate_seed_data.py first if there isn't enough real order history yet.
"""

import os

import joblib
import pandas as pd
import psycopg
from xgboost import XGBRegressor

DATABASE_URL = os.environ["DATABASE_URL"]
MODEL_VERSION = "demand-xgb-v1"
MODEL_OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "models", "demand_model.joblib")

FEATURE_COLUMNS = [
    "product_id_encoded",
    "day_of_week",
    "month",
    "is_weekend",
    "rolling_mean_7",
    "rolling_mean_14",
    "lag_1",
    "lag_7",
    "active_subscription_quantity_sum",
]


def load_order_quantities(conn) -> pd.DataFrame:
    query = """
        SELECT oi.product_id AS product_id, (o.created_at AT TIME ZONE 'UTC')::date AS d, SUM(oi.quantity) AS qty
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        WHERE o.status <> 'CANCELLED'
        GROUP BY oi.product_id, (o.created_at AT TIME ZONE 'UTC')::date
    """
    return pd.read_sql(query, conn)


def load_subscription_quantities(conn) -> pd.DataFrame:
    query = """
        SELECT s.product_id AS product_id, d.delivery_date AS d, SUM(s.quantity) AS qty
        FROM deliveries d
        JOIN subscriptions s ON s.id = d.subscription_id
        GROUP BY s.product_id, d.delivery_date
    """
    return pd.read_sql(query, conn)


def build_daily_dataset(conn) -> pd.DataFrame:
    """One row per (product, calendar day) with realized quantity and the
    subscription-driven share of it, zero-filled across each product's full
    observed date range so rolling/lag features are well defined."""
    orders_df = load_order_quantities(conn)
    subs_df = load_subscription_quantities(conn)

    combined = pd.concat([orders_df, subs_df], ignore_index=True)
    if combined.empty:
        return pd.DataFrame(columns=["product_id", "d", "quantity", "subscription_quantity"])

    daily_totals = combined.groupby(["product_id", "d"], as_index=False)["qty"].sum().rename(columns={"qty": "quantity"})
    subs_only = subs_df.groupby(["product_id", "d"], as_index=False)["qty"].sum().rename(columns={"qty": "subscription_quantity"})

    frames = []
    for product_id, group in daily_totals.groupby("product_id"):
        full_range = pd.date_range(group["d"].min(), group["d"].max(), freq="D")
        product_frame = pd.DataFrame({"d": full_range.date})
        product_frame["product_id"] = product_id
        product_frame = product_frame.merge(group[["d", "quantity"]], on="d", how="left")
        product_frame["quantity"] = product_frame["quantity"].fillna(0)
        product_subs = subs_only[subs_only["product_id"] == product_id][["d", "subscription_quantity"]]
        product_frame = product_frame.merge(product_subs, on="d", how="left")
        product_frame["subscription_quantity"] = product_frame["subscription_quantity"].fillna(0)
        frames.append(product_frame)

    return pd.concat(frames, ignore_index=True)


def add_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.sort_values(["product_id", "d"]).reset_index(drop=True)
    df["d"] = pd.to_datetime(df["d"])
    df["day_of_week"] = df["d"].dt.dayofweek
    df["month"] = df["d"].dt.month
    df["is_weekend"] = df["day_of_week"].isin([5, 6]).astype(int)

    grouped = df.groupby("product_id")["quantity"]
    df["rolling_mean_7"] = grouped.transform(lambda s: s.shift(1).rolling(window=7, min_periods=1).mean())
    df["rolling_mean_14"] = grouped.transform(lambda s: s.shift(1).rolling(window=14, min_periods=1).mean())
    df["lag_1"] = grouped.shift(1)
    df["lag_7"] = grouped.shift(7)
    df["active_subscription_quantity_sum"] = df["subscription_quantity"]

    fill_columns = ["rolling_mean_7", "rolling_mean_14", "lag_1", "lag_7"]
    df[fill_columns] = df[fill_columns].fillna(0)
    return df


def train(df: pd.DataFrame):
    product_ids = sorted(df["product_id"].astype(str).unique())
    product_id_to_code = {pid: i for i, pid in enumerate(product_ids)}
    df["product_id_encoded"] = df["product_id"].astype(str).map(product_id_to_code)

    model = XGBRegressor(
        n_estimators=200,
        max_depth=5,
        learning_rate=0.05,
        objective="reg:squarederror",
        random_state=42,
    )
    model.fit(df[FEATURE_COLUMNS], df["quantity"])
    return model, product_id_to_code


def main():
    with psycopg.connect(DATABASE_URL) as conn:
        raw = build_daily_dataset(conn)

    if raw.empty or len(raw) < 30:
        print("Not enough order/delivery history to train a model. Run generate_seed_data.py first.")
        return

    dataset = add_features(raw)
    model, product_id_to_code = train(dataset)

    bundle = {
        "model": model,
        "model_version": MODEL_VERSION,
        "feature_columns": FEATURE_COLUMNS,
        "product_id_to_code": product_id_to_code,
    }
    os.makedirs(os.path.dirname(MODEL_OUTPUT_PATH), exist_ok=True)
    joblib.dump(bundle, MODEL_OUTPUT_PATH)
    print(f"Trained on {len(dataset)} rows across {len(product_id_to_code)} product(s).")
    print(f"Model saved to {MODEL_OUTPUT_PATH}")


if __name__ == "__main__":
    main()
