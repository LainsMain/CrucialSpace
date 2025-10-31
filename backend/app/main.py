import os
import logging
import mimetypes
from typing import List, Optional

from fastapi import FastAPI, File, Form, UploadFile, Header, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel, field_validator
from tempfile import NamedTemporaryFile

from app.gemini import analyze, embed_text
from app.stt import transcribe


logger = logging.getLogger("crucialspace")
logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))


def _get_env(name: str, default: Optional[str] = None) -> str:
    value = os.getenv(name, default)
    if value is None:
        raise RuntimeError(f"Environment variable {name} must be set")
    return value


def _get_env_int(name: str, default: int) -> int:
    try:
        return int(os.getenv(name, str(default)))
    except ValueError:
        return default


CS_SHARED_SECRET = os.getenv("CS_SHARED_SECRET", "")
MAX_UPLOAD_MB = _get_env_int("MAX_UPLOAD_MB", 25)
TMP_DIR = os.getenv("TMP_DIR", "/data/tmp")
os.makedirs(TMP_DIR, exist_ok=True)


class AiReminder(BaseModel):
    event: str
    datetime: str


class AiResult(BaseModel):
    title: str
    summary: str
    todos: List[str]
    reminders: List[AiReminder]
    urls: List[str] = []
    embedding: Optional[List[float]] = None
    transcript: Optional[str] = None
    collections: List[str] = []

    @field_validator("todos")
    @classmethod
    def _normalize_todos(cls, value: List[str]) -> List[str]:
        return [str(v).strip() for v in value if str(v).strip()]


app = FastAPI(title="Crucial Space API")


@app.get("/health")
def health() -> JSONResponse:
    return JSONResponse({"status": "ok"})


def _ensure_secret(x_cs_secret: Optional[str]) -> None:
    if not CS_SHARED_SECRET:
        # Allow empty secret only if explicitly intended; warn loudly
        logger.warning("CS_SHARED_SECRET not set; /process will accept any caller")
        return
    if x_cs_secret != CS_SHARED_SECRET:
        raise HTTPException(status_code=401, detail="unauthorized")


def _check_size(name: str, data: bytes) -> None:
    max_bytes = MAX_UPLOAD_MB * 1024 * 1024
    if len(data) > max_bytes:
        raise HTTPException(
            status_code=413,
            detail=f"{name} too large; max {MAX_UPLOAD_MB} MB",
        )


@app.post("/process", response_model=AiResult)
async def process(
    image: Optional[UploadFile] = File(None),
    note_text: Optional[str] = Form(None),
    audio: Optional[UploadFile] = File(None),
    now_utc: Optional[str] = Form(None),
    existing_collections: Optional[str] = Form(None),
    x_cs_secret: Optional[str] = Header(None),
):
    _ensure_secret(x_cs_secret)

    if image is not None and not (image.content_type or "").startswith("image/"):
        raise HTTPException(status_code=415, detail="image must be of type image/*")

    # Save image to temp file
    img_path = None
    aud_path = None
    try:
        if image is not None:
            image_bytes = await image.read()
            _check_size("image", image_bytes)
            img_suffix = mimetypes.guess_extension(image.content_type or "") or ".bin"
            with NamedTemporaryFile(delete=False, suffix=img_suffix, dir=TMP_DIR) as imgf:
                imgf.write(image_bytes)
                img_path = imgf.name

        transcript: Optional[str] = None
        if audio is not None:
            audio_bytes = await audio.read()
            _check_size("audio", audio_bytes)
            aud_suffix = mimetypes.guess_extension(audio.content_type or "") or ".wav"
            with NamedTemporaryFile(delete=False, suffix=aud_suffix, dir=TMP_DIR) as audf:
                audf.write(audio_bytes)
                aud_path = audf.name
            try:
                transcript = transcribe(aud_path)
            except Exception as exc:
                logger.exception("STT failed: %s", exc)
                transcript = None

        note = (note_text or "").strip()
        stt_text = (transcript or "").strip()
        logger.info("Invoking Gemini analyze (note_len=%d, stt_len=%d)", len(note), len(stt_text))
        result = analyze(img_path, note, stt_text, now_utc, existing_collections or "")
        # Build an embedding from the most informative text available
        try:
            joined = " ".join(
                [
                    str(result.get("title") or ""),
                    str(result.get("summary") or ""),
                    " ".join(result.get("todos") or []),
                    " ".join(result.get("urls") or []),
                    (note or ""),
                    (stt_text or ""),
                ]
            ).strip()
            emb = embed_text(joined) if joined else []
        except Exception:
            emb = []
        result["embedding"] = emb
        result["transcript"] = transcript
        return result
    finally:
        # Best-effort cleanup
        try:
            if img_path and os.path.exists(img_path):
                os.remove(img_path)
        except Exception:
            pass
        try:
            if aud_path and os.path.exists(aud_path):
                os.remove(aud_path)
        except Exception:
            pass


class EmbedRequest(BaseModel):
    text: str


@app.post("/embed")
def embed(req: EmbedRequest, x_cs_secret: Optional[str] = Header(None)) -> JSONResponse:
    _ensure_secret(x_cs_secret)
    vec = embed_text((req.text or "").strip()) if req.text else []
    return JSONResponse({"embedding": vec})

