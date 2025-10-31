import os
import json
import logging
import mimetypes
import datetime
from typing import Dict, Any, List, Optional

import google.generativeai as genai


logger = logging.getLogger("crucialspace.gemini")


def _configure_client() -> genai.GenerativeModel:
    api_key = os.getenv("GOOGLE_API_KEY")
    if not api_key:
        raise RuntimeError("GOOGLE_API_KEY is not set")
    genai.configure(api_key=api_key)
    model_name = os.getenv("GEMINI_MODEL", "gemini-1.5-pro")
    return genai.GenerativeModel(model_name)


def _load_image_part(image_path: str) -> Dict[str, Any]:
    mime_type, _ = mimetypes.guess_type(image_path)
    if not mime_type:
        mime_type = "application/octet-stream"
    with open(image_path, "rb") as f:
        data = f.read()
    return {"mime_type": mime_type, "data": data}


def _build_prompt(note: str, stt_text: str, now_utc: Optional[str], existing_collections: str = "") -> str:
    user_note = note.strip()
    transcript = stt_text.strip()
    parts: List[str] = []
    if user_note:
        parts.append(f"User note: {user_note}")
    if transcript:
        parts.append(f"Transcribed voice note: {transcript}")
    if existing_collections.strip():
        parts.append("Existing collections (use if relevant; create new only if needed):\n" + existing_collections.strip())
    context = "\n".join(parts) if parts else "(no additional text context provided)"
    now_val = now_utc or datetime.datetime.now(datetime.timezone.utc).isoformat()
    return (
        "You are a Memory Assistant inside a personal knowledge app. Return ONLY valid JSON.\n"
        "Your goal is to turn the inputs (image + note + transcript) into a concise memory summary, not a photo caption.\n"
        "Produce a JSON object with this exact schema: \n"
        '{"title": string, "summary": string, "todos": string[], "reminders": [{"event": string, "datetime": string}], "urls": string[], "collections": string[]}.\n'
        "Summary style: 2–5 short sentences focused on the memory's key information.\n"
        "- Speak as a memory manager, not as a photographer.\n"
        "- NEVER say phrases like 'the image shows', 'the photo shows', 'in the picture'.\n"
        "- Begin with the subject directly (e.g., 'Burger sauce recipe: ...'), not 'The image shows a burger ...'.\n"
        "- Highlight entities (people/orgs/products), numbers (prices/qty), decisions/actions, deadlines, and must-know facts.\n"
        "- Prefer factual, bullet-like sentences separated by periods; no first-person voice.\n"
        "- You MUST format 'summary' in Markdown: use headings (#/##/###), bullet lists (- or 1.), and **bold** where useful.\n"
        "- Include blank lines between headings and paragraphs for readability. Do NOT use code fences.\n"
        "Title: a very short, scannable headline (≤ 7 words), noun-led (e.g., 'Burger Sauce Recipe').\n"
        "Extract concrete, actionable todos; write them as imperative phrases.\n"
        "If any date/time is implied, set reminders with ISO 8601 UTC in 'datetime'. If not, leave empty.\n"
        "If the context contains any URLs, include them in 'urls' as absolute URLs.\n"
        "Also propose up to 3 concise 'collections' (topics like 'Fashion', 'Minecraft', 'Textile').\n"
        "Collections policy: Prefer selecting 1–3 from the provided Existing collections when they fit well.\n"
        "Only propose a new collection if none of the existing collections are suitable, and propose at most ONE new collection.\n"
        "New collection naming: Title Case, 1–3 words, specific (avoid generic terms), no emojis/punctuation, no duplicates.\n"
        "Do not return more than 3 total collections, and no more than 1 new; return [] if nothing fits.\n"
        f"Current datetime (UTC): {now_val}. Use this as the 'now' reference when interpreting phrases like 'tomorrow'.\n"
        "If nothing is found for a field, return an empty list or an empty string accordingly.\n\n"
        f"Context:\n{context}\n\n"
        "Respond with JSON only — no code fences, no extra text."
    )


def _coerce_result(payload: Dict[str, Any]) -> Dict[str, Any]:
    title = str(payload.get("title", "")).strip()
    summary = str(payload.get("summary", "")).strip()
    todos_raw = payload.get("todos") or []
    if not isinstance(todos_raw, list):
        todos_raw = [str(todos_raw)]
    todos = [str(x).strip() for x in todos_raw if str(x).strip()]
    reminders_raw = payload.get("reminders") or []
    if not isinstance(reminders_raw, list):
        reminders_raw = []
    reminders: List[Dict[str, str]] = []
    for r in reminders_raw:
        if not isinstance(r, dict):
            continue
        event = str(r.get("event", "")).strip()
        dt = str(r.get("datetime", "")).strip()
        if event:
            reminders.append({"event": event, "datetime": dt})
    urls_raw = payload.get("urls") or []
    if not isinstance(urls_raw, list):
        urls_raw = [str(urls_raw)]
    urls = [str(u).strip() for u in urls_raw if str(u).strip()]
    col_raw = payload.get("collections") or []
    if not isinstance(col_raw, list):
        col_raw = [str(col_raw)]
    collections = [str(c).strip() for c in col_raw if str(c).strip()]
    return {"title": title, "summary": summary, "todos": todos, "reminders": reminders, "urls": urls, "collections": collections}


def _extract_json(text: str) -> Dict[str, Any]:
    # Try to find JSON if wrapped in code fences
    s = text.strip()
    if s.startswith("```"):
        # remove first fence line
        s = "\n".join(s.splitlines()[1:])
        # strip trailing fence if present
        if s.strip().endswith("```"):
            s = "\n".join(s.splitlines()[:-1])
    try:
        return json.loads(s)
    except Exception:
        # last resort: find first '{' and last '}'
        try:
            start = s.index("{")
            end = s.rindex("}")
            return json.loads(s[start : end + 1])
        except Exception as exc:
            logger.error("Failed to parse JSON from Gemini response: %s", exc)
            raise


def analyze(image_path: Optional[str], note: str, stt_text: str, now_utc: Optional[str], existing_collections: str = "") -> Dict[str, Any]:
    model = _configure_client()
    prompt = _build_prompt(note, stt_text, now_utc, existing_collections)
    parts: List[Dict[str, Any]] = [{"text": prompt}]
    if image_path:
        parts.append(_load_image_part(image_path))

    response = model.generate_content(parts)

    if not getattr(response, "text", None):
        raise RuntimeError("Empty response from Gemini")

    raw = response.text
    payload = _extract_json(raw)
    return _coerce_result(payload)


def embed_text(text: str) -> List[float]:
    """Return embedding vector using the configured embeddings model.

    Falls back to empty list on failure.
    """
    try:
        api_key = os.getenv("GOOGLE_API_KEY")
        if not api_key:
            raise RuntimeError("GOOGLE_API_KEY is not set")
        genai.configure(api_key=api_key)
        model_name = os.getenv("EMBEDDING_MODEL", "text-embedding-004")
        result = genai.embed_content(model=model_name, content=text)
        vec = result.get("embedding") or result.get("vector") or result
        # google.generativeai returns { 'embedding': { 'values': [...] } } for some SDK versions
        if isinstance(vec, dict) and "values" in vec:
            return [float(x) for x in vec["values"]]
        if isinstance(vec, list):
            return [float(x) for x in vec]
        return []
    except Exception as exc:
        logger.exception("embed_text failed: %s", exc)
        return []


