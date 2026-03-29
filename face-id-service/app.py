import asyncio
import io
import logging
import os
from typing import Tuple

import face_recognition
import requests
from fastapi import FastAPI, File, Form, HTTPException, UploadFile, status
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from requests import Response


USER_SERVICE_BASE_URL = os.getenv(
    "USER_SERVICE_BASE_URL",
    "http://localhost:8085/ProjetMicroUseryahya",
).rstrip("/")
DEFAULT_TOLERANCE = float(os.getenv("FACE_ID_TOLERANCE", "0.6"))
REQUEST_TIMEOUT_SECONDS = float(os.getenv("USER_SERVICE_TIMEOUT_SECONDS", "10"))

logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
)
logger = logging.getLogger("face-id-service")


class VerifyResponse(BaseModel):
    match: bool = Field(..., examples=[True])
    confidence: float = Field(..., examples=[0.87])


class ErrorResponse(BaseModel):
    error: str = Field(..., examples=["No face detected"])


app = FastAPI(
    title="Face ID Service",
    version="1.0.0",
    description=(
        "Receives a user ID and an uploaded image, fetches the reference image "
        "from the USER microservice, and verifies whether both faces match."
    ),
)


def fetch_reference_image(user_id: int) -> bytes:
    url = f"{USER_SERVICE_BASE_URL}/api/users/{user_id}/image"
    logger.info("Fetching reference image for userId=%s from %s", user_id, url)

    try:
        response: Response = requests.get(url, timeout=REQUEST_TIMEOUT_SECONDS)
    except requests.RequestException as exc:
        logger.exception("USER service is unreachable")
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="USER service not reachable",
        ) from exc

    if response.status_code == 404:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Reference image not found",
        )

    if response.status_code != 200:
        logger.error(
            "USER service returned unexpected status=%s body=%s",
            response.status_code,
            response.text,
        )
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Failed to fetch reference image",
        )

    content_type = response.headers.get("content-type", "")
    if content_type and not content_type.startswith("image/"):
        logger.error("USER service returned non-image content-type=%s", content_type)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Reference image is not a valid image",
        )

    return response.content


def load_image_bytes(image_bytes: bytes, source: str):
    try:
        return face_recognition.load_image_file(io.BytesIO(image_bytes))
    except Exception as exc:  # pragma: no cover - external library error handling
        logger.exception("Invalid image format for %s", source)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid image format: {source}",
        ) from exc


def extract_first_face_encoding(image_bytes: bytes, source: str):
    image = load_image_bytes(image_bytes, source)
    face_locations = face_recognition.face_locations(image)

    if not face_locations:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"No face detected in {source}",
        )

    if len(face_locations) > 1:
        logger.warning("Multiple faces detected in %s, using the first one", source)

    encodings = face_recognition.face_encodings(image, known_face_locations=face_locations)
    if not encodings:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unable to encode face in {source}",
        )

    return encodings[0]


def compare_faces(reference_bytes: bytes, candidate_bytes: bytes, tolerance: float) -> Tuple[bool, float]:
    reference_encoding = extract_first_face_encoding(reference_bytes, "reference image")
    candidate_encoding = extract_first_face_encoding(candidate_bytes, "uploaded image")

    distance = face_recognition.face_distance([reference_encoding], candidate_encoding)[0]
    match = bool(face_recognition.compare_faces([reference_encoding], candidate_encoding, tolerance=tolerance)[0])

    confidence = max(0.0, min(1.0, 1.0 - float(distance)))
    logger.info(
        "Face comparison completed match=%s distance=%.4f confidence=%.4f tolerance=%.4f",
        match,
        float(distance),
        confidence,
        tolerance,
    )
    return match, confidence


@app.get("/health", tags=["Health"])
async def health_check():
    return {"status": "ok"}


@app.post(
    "/verify",
    tags=["Verification"],
    summary="Verify an uploaded face against the USER service reference image",
    response_model=VerifyResponse,
    responses={
        200: {"description": "Face comparison completed successfully"},
        400: {"model": ErrorResponse, "description": "Invalid request or image"},
        404: {"model": ErrorResponse, "description": "Reference image not found"},
        502: {"model": ErrorResponse, "description": "USER service error"},
        500: {"model": ErrorResponse, "description": "Internal server error"},
    },
)
async def verify_face(
    userId: int = Form(..., description="User identifier from the USER microservice"),
    file: UploadFile = File(..., description="Image file to verify"),
    tolerance: float = Form(
        DEFAULT_TOLERANCE,
        description="Optional face matching tolerance between 0 and 1",
    ),
):
    if file is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No image uploaded",
        )

    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Uploaded file must be an image",
        )

    if tolerance <= 0 or tolerance > 1:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="tolerance must be between 0 and 1",
        )

    uploaded_bytes = await file.read()
    if not uploaded_bytes:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Uploaded image is empty",
        )

    reference_bytes = await asyncio.to_thread(fetch_reference_image, userId)
    match, confidence = await asyncio.to_thread(
        compare_faces,
        reference_bytes,
        uploaded_bytes,
        tolerance,
    )

    return JSONResponse(
        status_code=status.HTTP_200_OK,
        content={
            "match": match,
            "confidence": round(confidence, 4),
        },
    )


@app.exception_handler(HTTPException)
async def http_exception_handler(_, exc: HTTPException):
    detail = exc.detail if isinstance(exc.detail, str) else "Request failed"
    return JSONResponse(status_code=exc.status_code, content={"error": detail})


@app.exception_handler(Exception)
async def generic_exception_handler(_, exc: Exception):
    logger.exception("Unexpected error during face verification")
    return JSONResponse(status_code=500, content={"error": "Internal server error"})
