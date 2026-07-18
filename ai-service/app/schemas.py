from datetime import date
from typing import List
from uuid import UUID

from pydantic import BaseModel, Field


class DemandForecastRequest(BaseModel):
    productId: UUID
    horizonDays: int = Field(default=7, ge=1, le=30)


class DemandPrediction(BaseModel):
    date: date
    predictedQuantity: float
    confidenceLow: float
    confidenceHigh: float


class DemandForecastResponse(BaseModel):
    productId: UUID
    modelVersion: str
    predictions: List[DemandPrediction]
