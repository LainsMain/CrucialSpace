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
- **Privacy-Focused**: Your data stays on your device; AI runs directly via your Gemini API key

## 🏗️ Architecture

### Android App
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room Database (local storage)
- **AI Integration**: Direct Gemini API calls (gemini-2.5-flash)
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## 🚀 Quick Start

### Prerequisites

- **Android Device** running Android 8.0 or higher
- **Google Gemini API Key** - Free at [https://aistudio.google.com/apikey](https://aistudio.google.com/apikey)

### Installation

1. **Download the latest APK** from [GitHub Releases](https://github.com/LainsMain/CrucialSpace/releases)
2. **Install on your Android device** (enable "Install from unknown sources" if needed)
3. **Open the app** → Settings
4. **Enter your Gemini API key**
5. **Select your preferred language** (or leave on Auto-detect)
6. **Grant permissions** when prompted:
   - Notifications (for reminders)
   - Battery optimization (for background processing)
   - Camera (optional, for capturing images)
   - Microphone (optional, for voice notes)
7. **Start using!** Share content from any app to Crucial Space

### For Developers

1. **Clone the repository**
   ```bash
   git clone https://github.com/LainsMain/CrucialSpace.git
   cd CrucialSpace/android
   ```

2. **Open in Android Studio**
   - Open the `android` folder
   - Sync Gradle dependencies
   - Run on emulator or device

3. **Build debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Build release APK**
   ```bash
   ./gradlew assembleRelease
   ```
   Output: `android/app/build/outputs/apk/release/app-release.apk`

5. **Generate SHA-256 checksum** (for releases)
   ```powershell
   # Windows
   CertUtil -hashfile android\app\build\outputs\apk\release\app-release.apk SHA256
   
   # Linux/Mac
   shasum -a 256 android/app/build/outputs/apk/release/app-release.apk
   ```

## 📖 How It Works

### User Flow

1. **Share Content**: Share screenshots, URLs, text, or images from any app via Android's Share Sheet
2. **Enrich**: Quick enrichment screen appears where you can:
   - Add text notes
   - Record voice notes
   - See URL preview images (for Reddit, etc.)
3. **Save**: Content is saved locally to device
4. **AI Processing** (runs in background):
   - Transcribes audio using Gemini (if voice note provided)
   - Analyzes image + text using Gemini multimodal API
   - Extracts: summary, title, todos, reminders, URLs, collections
   - Generates embeddings for semantic search
5. **Display**: Memory appears in the main feed with AI-generated insights

### AI Models Used

- **Generation**: `gemini-2.5-flash` - Fast multimodal analysis
- **Embeddings**: `text-embedding-004` - Semantic search
- **Audio Transcription**: Gemini multimodal (audio input)

## 🔒 Security & Privacy

- **Local-First**: All memories stored locally on device using Room Database
- **Secure Storage**: Gemini API key stored in Android EncryptedSharedPreferences
- **Direct API Calls**: App communicates directly with Google's Gemini API (no intermediary servers)
- **Your Data, Your Control**: Delete the app = delete all data

## 📝 License

[MIT license]
