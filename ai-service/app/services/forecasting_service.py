from datetime import date, timedelta
from typing import Dict, Optional
from uuid import UUID

import joblib
import pandas as pd

from app.config import MODEL_PATH
from app.db import get_connection
from app.schemas import DemandForecastResponse, DemandPrediction

_bundle: Optional[dict] = None

HISTORY_WINDOW_DAYS = 14


class ProductNotTrainedError(Exception):
    """Raised when a forecast is requested for a product the model has never seen."""


def load_model() -> Optional[dict]:
    global _bundle
    try:
        _bundle = joblib.load(MODEL_PATH)
    except FileNotFoundError:
        _bundle = None
    return _bundle


def is_model_loaded() -> bool:
    return _bundle is not None


def model_version() -> Optional[str]:
    return _bundle["model_version"] if _bundle else None


def _recent_daily_quantities(conn, product_id: str, days: int) -> Dict[date, float]:
    """Realized demand (orders + subscription deliveries) for the trailing window ending yesterday."""
    query = """
        SELECT d::date AS d, SUM(qty) AS qty FROM (
            SELECT (o.created_at AT TIME ZONE 'UTC')::date AS d, oi.quantity AS qty
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.product_id = %s AND o.status <> 'CANCELLED'
              AND (o.created_at AT TIME ZONE 'UTC')::date >= current_date - %s
              AND (o.created_at AT TIME ZONE 'UTC')::date < current_date
            UNION ALL
            SELECT del.delivery_date AS d, s.quantity AS qty
            FROM deliveries del
            JOIN subscriptions s ON s.id = del.subscription_id
            WHERE s.product_id = %s AND del.delivery_date >= current_date - %s
              AND del.delivery_date < current_date
        ) combined
        GROUP BY d
    """
    with conn.cursor() as cur:
        cur.execute(query, (product_id, days, product_id, days))
        rows = cur.fetchall()
    return {row[0]: float(row[1]) for row in rows}


def _active_subscriptions(conn, product_id: str):
    query = """
        SELECT frequency, weekdays, start_date, end_date, quantity
        FROM subscriptions
        WHERE product_id = %s AND status = 'ACTIVE'
    """
    with conn.cursor() as cur:
        cur.execute(query, (product_id,))
        return cur.fetchall()


def _is_eligible(frequency: str, weekdays: Optional[str], start_date: date, end_date: Optional[date], target_date: date) -> bool:
    """Mirrors DailyDeliveryGeneratorJob's eligibility rule on the Java side."""
    if target_date < start_date:
        return False
    if end_date is not None and target_date > end_date:
        return False
    if frequency == "DAILY":
        return True
    if frequency == "ALTERNATE_DAY":
        return (target_date - start_date).days % 2 == 0
    if frequency == "CUSTOM":
        if not weekdays:
            return False
        iso_day = target_date.isoweekday()  # Monday=1 .. Sunday=7
        return str(iso_day) in [d.strip() for d in weekdays.split(",")]
    return False


def _subscription_quantity_for(subscriptions, target_date: date) -> float:
    total = 0.0
    for frequency, weekdays, start_date, end_date, quantity in subscriptions:
        if _is_eligible(frequency, weekdays, start_date, end_date, target_date):
            total += float(quantity)
    return total


def predict(product_id: UUID, horizon_days: int) -> DemandForecastResponse:
    if _bundle is None:
        raise RuntimeError("Model is not loaded")

    product_key = str(product_id)
    product_code = _bundle["product_id_to_code"].get(product_key)
    if product_code is None:
        raise ProductNotTrainedError(f"No trained model data for product {product_key}")

    today = date.today()
    with get_connection() as conn:
        values = _recent_daily_quantities(conn, product_key, days=HISTORY_WINDOW_DAYS)
        subscriptions = _active_subscriptions(conn, product_key)

    feature_columns = _bundle["feature_columns"]
    predictions = []

    for step in range(1, horizon_days + 1):
        target_date = today + timedelta(days=step)

        lag_1 = values.get(target_date - timedelta(days=1), 0.0)
        lag_7 = values.get(target_date - timedelta(days=7), 0.0)
        window_7 = [values.get(target_date - timedelta(days=d), 0.0) for d in range(1, 8)]
        window_14 = [values.get(target_date - timedelta(days=d), 0.0) for d in range(1, 15)]

        row = pd.DataFrame([{
            "product_id_encoded": product_code,
            "day_of_week": target_date.weekday(),
            "month": target_date.month,
            "is_weekend": 1 if target_date.weekday() >= 5 else 0,
            "rolling_mean_7": sum(window_7) / 7,
            "rolling_mean_14": sum(window_14) / 14,
            "lag_1": lag_1,
            "lag_7": lag_7,
            "active_subscription_quantity_sum": _subscription_quantity_for(subscriptions, target_date),
        }])[feature_columns]

        predicted = max(0.0, float(_bundle["model"].predict(row)[0]))
        values[target_date] = predicted

        predictions.append(DemandPrediction(
            date=target_date,
            predictedQuantity=round(predicted, 2),
            confidenceLow=round(predicted * 0.9, 2),
            confidenceHigh=round(predicted * 1.1, 2),
        ))

    return DemandForecastResponse(
        productId=product_id,
        modelVersion=_bundle["model_version"],
        predictions=predictions,
    )
