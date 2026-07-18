from fastapi import APIRouter

from app.services import forecasting_service

router = APIRouter()


@router.get("/health")
def health():
    return {
        "status": "ok",
        "modelLoaded": forecasting_service.is_model_loaded(),
        "modelVersion": forecasting_service.model_version(),
    }
