"""
Generates synthetic historical order data so the demand-forecasting model has
something to learn from when real order history is too thin (which it will be,
initially). Safe to re-run: it deletes its own previously-generated synthetic
orders (tagged by a dedicated bot user) before inserting fresh ones.

Requires at least one active product to already exist (create one via the
backend API first). Reads DATABASE_URL from the environment, same as the app.
"""

import os
import random
import uuid
from datetime import date, datetime, time, timedelta, timezone

import psycopg

DATABASE_URL = os.environ["DATABASE_URL"]
SYNTHETIC_USER_EMAIL = "training-data-bot@agrodairy.internal"
HISTORY_DAYS = 270  # ~9 months

WEEKDAY_MULTIPLIER = {
    0: 1.1,  # Monday
    1: 1.0,  # Tuesday
    2: 1.0,  # Wednesday
    3: 1.0,  # Thursday
    4: 1.3,  # Friday
    5: 0.8,  # Saturday
    6: 0.6,  # Sunday
}


def get_or_create_synthetic_user(conn) -> str:
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM users WHERE email = %s", (SYNTHETIC_USER_EMAIL,))
        row = cur.fetchone()
        if row:
            return str(row[0])
        user_id = str(uuid.uuid4())
        cur.execute(
            """
            INSERT INTO users (id, email, password_hash, full_name, role, is_active)
            VALUES (%s, %s, %s, %s, 'CUSTOMER', true)
            """,
            (user_id, SYNTHETIC_USER_EMAIL, "$2a$10$disabled.account.no.login.possible.here", "Training Data Bot"),
        )
        return user_id


def fetch_active_products(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT id, price FROM products WHERE active = true")
        return cur.fetchall()


def clear_previous_synthetic_orders(conn, user_id: str) -> None:
    with conn.cursor() as cur:
        cur.execute(
            "DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE user_id = %s)",
            (user_id,),
        )
        cur.execute("DELETE FROM orders WHERE user_id = %s", (user_id,))


def daily_quantity(base_demand: float, day_index: int, day_of_week: int) -> int:
    trend = 1.0 + (day_index * 0.0006)  # mild upward drift across the window
    noise = random.gauss(1.0, 0.12)
    quantity = base_demand * WEEKDAY_MULTIPLIER[day_of_week] * trend * noise
    return max(0, round(quantity))


def generate(conn) -> None:
    user_id = get_or_create_synthetic_user(conn)
    clear_previous_synthetic_orders(conn, user_id)

    products = fetch_active_products(conn)
    if not products:
        print("No active products found. Create at least one product via the backend API first.")
        return

    start_date = date.today() - timedelta(days=HISTORY_DAYS)
    total_orders = 0
    with conn.cursor() as cur:
        for product_id, price in products:
            base_demand = random.uniform(15, 45)
            for day_index in range(HISTORY_DAYS):
                order_date = start_date + timedelta(days=day_index)
                quantity = daily_quantity(base_demand, day_index, order_date.weekday())
                if quantity <= 0:
                    continue

                order_id = str(uuid.uuid4())
                total_amount = float(price) * quantity
                created_at = datetime.combine(order_date, time(12, 0), tzinfo=timezone.utc)
                cur.execute(
                    """
                    INSERT INTO orders
                        (id, user_id, status, total_amount, payment_status, delivery_address, created_at, updated_at)
                    VALUES (%s, %s, 'DELIVERED', %s, 'PAID', 'Synthetic training data', %s, %s)
                    """,
                    (order_id, user_id, total_amount, created_at, created_at),
                )
                cur.execute(
                    """
                    INSERT INTO order_items (id, order_id, product_id, quantity, unit_price)
                    VALUES (%s, %s, %s, %s, %s)
                    """,
                    (str(uuid.uuid4()), order_id, product_id, quantity, price),
                )
                total_orders += 1

    conn.commit()
    print(f"Generated {total_orders} synthetic orders across {len(products)} product(s) over {HISTORY_DAYS} days.")


if __name__ == "__main__":
    with psycopg.connect(DATABASE_URL) as connection:
        generate(connection)
