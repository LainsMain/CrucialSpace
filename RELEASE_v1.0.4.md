# Crucial Space v1.0.4 - Gemini-Only Mode & Auto-Updates

## 🚨 Breaking Changes

**Removed Backend Support**: The app now exclusively uses direct Gemini API integration. All backend/server functionality has been completely removed to simplify the app architecture.

- **Migration**: Existing Gemini API keys are preserved. If you were using a custom backend, you'll need to enter a Gemini API key in Settings.
- **Why**: Streamlines the app and prepares for future cloud backup feature.

## ✨ New Features

- **Automatic Update Check**: The app now checks for updates automatically when launched and shows a notification if a new version is available.
- **Simplified Settings**: Removed backend toggle and server configuration fields. Only Gemini API key and language preference remain.

## 🔧 Improvements

- Fixed Collections bottom padding to match Space screen (prevents content from being cut off by FAB)
- Fixed FAB positioning on Collections screen (plus button now hovers at bottom-right like Space screen)

## 📦 Installation

1. Download `app-release.apk` from the assets below
2. Install on your Android device (enable "Install from unknown sources" if needed)
3. Open Settings and enter your Gemini API key
4. Get your API key at: https://aistudio.google.com/apikey

## 🔒 Verification

**SHA-256 Checksum**: `01c63e06c2f3b12daaf0acdfa1e4c184c2e79acf430b4b16479d576352f1eb10`

Verify on Windows:
```powershell
CertUtil -hashfile app-release.apk SHA256
```

Verify on Linux/Mac:
```bash
shasum -a 256 app-release.apk
```

**Min SDK**: 26 (Android 8.0)  
**Target SDK**: 34 (Android 14)

