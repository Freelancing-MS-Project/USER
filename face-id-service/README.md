# Face ID Service

Python 3.11 FastAPI microservice for Face ID verification against the reference image stored in the USER microservice.

## Features

- `POST /verify` accepts `multipart/form-data`
- Fetches the reference image from the USER service
- Detects and encodes the first face in each image
- Compares both faces with configurable tolerance
- Returns JSON with `match` and `confidence`
- Exposes Swagger UI at `http://localhost:5000/docs`

## Project Structure

```text
face-id-service/
├── app.py
├── requirements.txt
├── README.md
└── Dockerfile
```

## Prerequisites

- Python 3.11
- A working C/C++ build toolchain may be required for `face_recognition` / `dlib`
- USER microservice running and reachable at:

```text
http://localhost:8085/ProjetMicroUseryahya
```

## Installation

```bash
cd face-id-service
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

On Windows PowerShell:

```powershell
cd face-id-service
py -3.11 -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## Run Locally

```bash
uvicorn app:app --host 0.0.0.0 --port 5000 --reload
```

Service URL:

```text
http://localhost:5000
```

Swagger documentation:

```text
http://localhost:5000/docs
```

## Configuration

Environment variables:

- `USER_SERVICE_BASE_URL`
  Default: `http://localhost:8085/ProjetMicroUseryahya`
- `FACE_ID_TOLERANCE`
  Default: `0.6`
- `USER_SERVICE_TIMEOUT_SECONDS`
  Default: `10`
- `LOG_LEVEL`
  Default: `INFO`

Example:

```bash
export USER_SERVICE_BASE_URL=http://localhost:8085/ProjetMicroUseryahya
export FACE_ID_TOLERANCE=0.6
uvicorn app:app --host 0.0.0.0 --port 5000 --reload
```

## API

### Health Check

```http
GET /health
```

Response:

```json
{
  "status": "ok"
}
```

### Verify Face

```http
POST /verify
Content-Type: multipart/form-data
```

Form fields:

- `userId` -> Text
- `file` -> File
- `tolerance` -> Text, optional, default `0.6`

Success response:

```json
{
  "match": true,
  "confidence": 0.87
}
```

Error response:

```json
{
  "error": "No face detected in uploaded image"
}
```

## Postman Test

Request:

```text
POST http://localhost:5000/verify
```

Body -> `form-data`

- `userId` -> Text
- `file` -> File
- `tolerance` -> Text (optional)

### Expected Success Response

Same person:

```json
{
  "match": true,
  "confidence": 0.87
}
```

Different person:

```json
{
  "match": false,
  "confidence": 0.42
}
```

### Expected Error Responses

No face detected:

```json
{
  "error": "No face detected in uploaded image"
}
```

USER service unreachable:

```json
{
  "error": "USER service not reachable"
}
```

Reference image missing:

```json
{
  "error": "Reference image not found"
}
```

Invalid image:

```json
{
  "error": "Invalid image format: uploaded image"
}
```

Tolerance error:

```json
{
  "error": "tolerance must be between 0 and 1"
}
```

## Internal Flow

1. Validate `userId`, `file`, and `tolerance`
2. Download the reference image from:

```text
GET /api/users/{userId}/image
```

3. Load both images into memory
4. Detect the first face in each image
5. Compute facial encodings
6. Compare with:
   - `face_recognition.compare_faces`
   - `face_recognition.face_distance`
7. Return:
   - `match`
   - `confidence`

## Docker

Build:

```bash
cd face-id-service
docker build -t face-id-service .
```

Run:

```bash
docker run --rm -p 5000:5000 \
  -e USER_SERVICE_BASE_URL=http://host.docker.internal:8080/ProjetMicroUseryahya \
  face-id-service
```

## Notes

- If multiple faces are detected, the service uses the first one.
- Confidence is derived from `1 - face_distance`, clamped between `0` and `1`.
- Lower tolerance is stricter. Default `0.6` is a common baseline.
- The endpoint is `async`, while blocking HTTP and face-encoding work is pushed to worker threads.
