from fastapi import FastAPI

from app.routers import health

app = FastAPI(title="AgroDairy AI Service")

app.include_router(health.router)
