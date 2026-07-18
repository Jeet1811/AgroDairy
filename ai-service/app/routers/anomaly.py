from fastapi import APIRouter, Depends, HTTPException, status

from app.security import verify_internal_api_key

router = APIRouter(dependencies=[Depends(verify_internal_api_key)])


@router.post("/detect/anomaly")
def detect_anomaly():
    """
    Stubbed and unused in v1 — anomaly detection is a pure statistical
    calculation done directly in the backend (AnomalyDetectionService) to
    avoid a network hop on every production record insert. See build spec §9.2.
    """
    raise HTTPException(status_code=status.HTTP_501_NOT_IMPLEMENTED, detail="Anomaly detection runs in the backend, not here")
