import psycopg

from app.config import DATABASE_URL


def get_connection() -> psycopg.Connection:
    """Read-only connection to the shared Postgres instance — ai-service never writes."""
    return psycopg.connect(DATABASE_URL)
