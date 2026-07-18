import os

from dotenv import load_dotenv

# In Docker, env vars come from docker-compose.yml directly, and no .env file exists —
# load_dotenv() is then a harmless no-op. Running natively (no Docker), this picks up
# ai-service/.env so DATABASE_URL/INTERNAL_API_KEY don't need to be exported by hand.
load_dotenv()

DATABASE_URL = os.environ["DATABASE_URL"]
INTERNAL_API_KEY = os.environ["INTERNAL_API_KEY"]
MODEL_PATH = os.environ.get("MODEL_PATH", os.path.join(os.path.dirname(__file__), "models", "demand_model.joblib"))
