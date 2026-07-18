from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.routers import anomaly, forecast, health
from app.services import forecasting_service


@asynccontextmanager
async def lifespan(app: FastAPI):
    forecasting_service.load_model()
    yield


app = FastAPI(title="AgroDairy AI Service", lifespan=lifespan)

app.include_router(health.router)
app.include_router(forecast.router)
app.include_router(anomaly.router)
