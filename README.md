# Crucial Space

**Crucial Space** is an AI-powered "second memory" for Android. Inspired by Essential Space from Nothing Phone, but designed to work on all Android devices.

## 📱 What is Crucial Space?

Crucial Space is a utility app that transforms how you capture and remember information. Instead of switching between apps, you simply **share** content to Crucial Space from anywhere on your phone. The app uses AI to automatically transcribe, summarize, and extract actionable insights from your content.

### Key Features

- **Share-to-Save**: Share screenshots, URLs, text, or images from any app via Android's Share Sheet
- **Voice & Text Notes**: Quickly add context with voice recordings or text notes
- **AI-Powered Analysis**: Automatically transcribes audio, summarizes content, extracts action items, and identifies reminders
- **Smart Collections**: Automatically organizes memories into relevant collections
- **Offline-First**: All memories stored locally using Room Database for fast, offline access
- **Privacy-Focused**: Your data stays on your device; AI processing happens on your own backend

## 🏗️ Architecture

### Android App
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room Database (local storage)
- **Networking**: Retrofit + OkHttp
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

### Backend API
- **Framework**: FastAPI (Python)
- **AI Services**: 
  - Google Gemini API (multimodal analysis)
  - Google Speech-to-Text API or OpenAI Whisper (transcription)
- **Features**:
  - Image + text analysis
  - Audio transcription
  - Text embeddings for semantic search
  - Collection suggestions

## 🚀 Quick Start

### Prerequisites

- **Android**: Android Studio with Android SDK
- **Backend**: Python 3.12+, Docker (optional)
- **API Keys**: 
  - Google API Key (for Gemini)
  - Optional: Google Cloud Speech-to-Text credentials or OpenAI API key

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd CrucialSpace
   ```

2. **Configure environment variables**
   ```bash
   cd backend
   cp env.example .env
   ```
   
   Edit `.env` with your configuration:
   ```env
   GOOGLE_API_KEY=your_api_key_here
   CS_SHARED_SECRET=your_shared_secret_here
   GEMINI_MODEL=gemini-1.5-pro
   MAX_UPLOAD_MB=25
   ```

3. **Run with Docker (Recommended)**
   ```bash
   docker build -t crucialspace-api ./backend
   docker run -p 8000:8000 --env-file ./backend/.env crucialspace-api
   ```

4. **Or run locally**
   ```bash
   cd backend
   pip install -r requirements.txt
   uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```

The API will be available at `http://localhost:8000`

**API Endpoints**:
- `GET /health` - Health check
- `POST /process` - Process image + note/audio (requires `X-CS-Secret` header)
- `POST /embed` - Generate text embeddings (requires `X-CS-Secret` header)

### Android App Setup

1. **Open in Android Studio**
   ```bash
   cd android
   ```
   Open the `android` folder in Android Studio.

2. **Configure API endpoint**
   
   You'll need to configure the backend URL in the app. This is typically done in:
   - Build configuration files
   - Or runtime configuration

3. **Configure shared secret**
   
   Set the same `CS_SHARED_SECRET` value that you used in the backend to authenticate API requests.

4. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio's build and run functionality.

## 🔧 Configuration

### Backend Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `GOOGLE_API_KEY` | Google API key for Gemini | Yes | - |
| `CS_SHARED_SECRET` | Shared secret for API authentication | Recommended | - |
| `GEMINI_MODEL` | Gemini model to use | No | `gemini-1.5-pro` |
| `MAX_UPLOAD_MB` | Maximum upload size in MB | No | `25` |
| `TMP_DIR` | Temporary directory for uploads | No | `/data/tmp` |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to GCP credentials for Speech-to-Text | No | - |
| `OPENAI_API_KEY` | OpenAI API key (alternative STT) | No | - |
| `STT_LANGUAGE_CODE` | Language code for transcription | No | `en-US` |

### Speech-to-Text Options

The backend supports multiple STT providers (in order of preference):

1. **Google Cloud Speech-to-Text** (requires `GOOGLE_APPLICATION_CREDENTIALS`)
2. **Gemini API** (uses `GOOGLE_API_KEY`)
3. **OpenAI Whisper** (requires `OPENAI_API_KEY`)

## 🐳 Deployment

### Docker Compose

A complete deployment setup is available in the `deploy/` directory:

1. **Configure environment**
   ```bash
   cd deploy
   cp stack.env.example stack.env
   # Edit stack.env with your configuration
   ```

2. **Deploy**
   ```bash
   docker-compose up -d
   ```

The stack includes:
- **API service**: FastAPI backend
- **Caddy**: Optional reverse proxy with automatic HTTPS (configure `Caddyfile`)

### Production Considerations

- Set a strong `CS_SHARED_SECRET` for API authentication
- Use environment variables or secrets management
- Configure Caddy with your domain for HTTPS
- Set up proper firewall rules
- Consider rate limiting for the API
- Monitor API usage and costs for Gemini/STT services

## 📖 How It Works

### User Flow

1. **Share Content**: User shares a screenshot, URL, or text from any app
2. **Add Context**: Quick enrichment screen appears with options to:
   - Add a text note
   - Record a voice note
3. **Save**: Content is saved locally and API processing begins
4. **AI Processing**: Backend processes the content:
   - Transcribes audio (if provided)
   - Analyzes image + text using Gemini
   - Extracts: summary, todos, reminders, URLs, collections
   - Generates embeddings for semantic search
5. **Display**: Memory appears in the main feed with AI-generated insights

### Backend Processing

The `/process` endpoint:
- Receives image (optional), text note (optional), and/or audio (optional)
- Transcribes audio if provided
- Sends multimodal request to Gemini API with image + text context
- Parses structured JSON response with:
  - Title (short headline)
  - Summary (formatted markdown)
  - Todos (actionable items)
  - Reminders (with datetime)
  - URLs (extracted links)
  - Collections (topic tags)
- Generates text embedding for semantic search
- Returns structured data to the app

## 🔒 Security

- **API Authentication**: Uses shared secret header (`X-CS-Secret`)
- **Local Storage**: All memories stored locally on device
- **No API Keys in App**: All API keys remain on backend server
- **Optional HTTPS**: Configure Caddy for encrypted connections

## 📝 License

[Add your license here]

## 🤝 Contributing

[Add contribution guidelines if applicable]

## 📧 Support

[Add support/contact information]

