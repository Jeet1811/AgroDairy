from typing import Optional

from fastapi import Header, HTTPException, status

from app.config import INTERNAL_API_KEY


async def verify_internal_api_key(x_internal_api_key: Optional[str] = Header(default=None)) -> None:
    if x_internal_api_key is None or x_internal_api_key != INTERNAL_API_KEY:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid internal API key")
