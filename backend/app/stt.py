import os
import logging
import mimetypes
from typing import Optional


logger = logging.getLogger("crucialspace.stt")


def _google_speech_available() -> bool:
    try:
        from google.cloud import speech  # noqa: F401
        return True
    except Exception:
        return False


def _openai_available() -> bool:
    try:
        import requests  # noqa: F401
        return bool(os.getenv("OPENAI_API_KEY"))
    except Exception:
        return False


def _transcribe_openai(audio_path: str) -> Optional[str]:
    import requests

    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        return None
    model = os.getenv("WHISPER_MODEL", "whisper-1")
    with open(audio_path, "rb") as f:
        files = {"file": (os.path.basename(audio_path), f, "application/octet-stream")}
        data = {"model": model}
        headers = {"Authorization": f"Bearer {api_key}"}
        resp = requests.post("https://api.openai.com/v1/audio/transcriptions", headers=headers, data=data, files=files, timeout=60)
    if resp.status_code >= 400:
        logger.error("OpenAI STT failed: %s %s", resp.status_code, resp.text)
        return None
    try:
        js = resp.json()
        text = (js.get("text") or "").strip()
        return text or None
    except Exception:
        return None


def _transcribe_google(audio_path: str) -> Optional[str]:
    from google.cloud import speech

    client = speech.SpeechClient()
    with open(audio_path, "rb") as f:
        content = f.read()

    audio = speech.RecognitionAudio(content=content)
    config = speech.RecognitionConfig(
        encoding=speech.RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED,
        language_code=os.getenv("STT_LANGUAGE_CODE", "en-US"),
        enable_automatic_punctuation=True,
        model=os.getenv("STT_MODEL", "default"),
    )
    resp = client.recognize(config=config, audio=audio)
    texts = []
    for result in resp.results:
        if result.alternatives:
            texts.append(result.alternatives[0].transcript)
    joined = " ".join(texts).strip()
    return joined or None


def _gemini_available() -> bool:
    return bool(os.getenv("GOOGLE_API_KEY"))


def _transcribe_gemini(audio_path: str) -> Optional[str]:
    import google.generativeai as genai

    api_key = os.getenv("GOOGLE_API_KEY")
    if not api_key:
        return None
    genai.configure(api_key=api_key)
    model_name = os.getenv("GEMINI_STT_MODEL", os.getenv("GEMINI_MODEL", "gemini-1.5-pro"))
    model = genai.GenerativeModel(model_name)

    mime, _ = mimetypes.guess_type(audio_path)
    if not mime:
        mime = "application/octet-stream"
    with open(audio_path, "rb") as f:
        data = f.read()

    lang = os.getenv("STT_LANGUAGE_CODE", "en-US")
    prompt = (
        "Transcribe the audio to plain text. "
        f"Language hint: {lang}. "
        "Return only the transcription text without timestamps or extra formatting."
    )

    parts = [
        {"text": prompt},
        {"mime_type": mime, "data": data},
    ]
    try:
        resp = model.generate_content(parts)
        text = (getattr(resp, "text", None) or "").strip()
        return text or None
    except Exception as exc:
        logger.exception("Gemini STT failed: %s", exc)
        return None


def transcribe(audio_path: str) -> Optional[str]:
    """Transcribe audio file to text.

    Uses Google Cloud Speech-to-Text if available (requires credentials),
    otherwise tries Gemini API (uses GOOGLE_API_KEY), then OpenAI Whisper, otherwise returns None.
    """
    if _google_speech_available() and os.getenv("GOOGLE_APPLICATION_CREDENTIALS"):
        try:
            return _transcribe_google(audio_path)
        except Exception as exc:
            logger.exception("Google STT failed: %s", exc)
    if _gemini_available():
        try:
            return _transcribe_gemini(audio_path)
        except Exception as exc:
            logger.exception("Gemini STT failed: %s", exc)
    if _openai_available():
        try:
            return _transcribe_openai(audio_path)
        except Exception as exc:
            logger.exception("OpenAI STT failed: %s", exc)
    logger.info("STT is disabled or not configured; skipping transcription")
    return None


