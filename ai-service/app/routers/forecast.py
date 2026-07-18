from fastapi import APIRouter, Depends, HTTPException, status

from app.schemas import DemandForecastRequest, DemandForecastResponse
from app.security import verify_internal_api_key
from app.services import forecasting_service

router = APIRouter(dependencies=[Depends(verify_internal_api_key)])


@router.post("/predict/demand", response_model=DemandForecastResponse)
def predict_demand(request: DemandForecastRequest) -> DemandForecastResponse:
    if not forecasting_service.is_model_loaded():
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Forecast model is not loaded")
    try:
        return forecasting_service.predict(request.productId, request.horizonDays)
    except forecasting_service.ProductNotTrainedError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
