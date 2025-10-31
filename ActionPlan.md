# Crucial Space: Action Plan

### 1. 🎯 Core Concept & Vision
**Crucial Space** is an AI-powered "second memory" for Android. It's not an app you live in, but a utility you send things to. It uses the Android Share Sheet to capture any piece of content (screenshots, URLs, text), lets you instantly add context with a voice or text note, and then uses the Gemini API to make that content transcribed, summarized, and actionable.

---

### 2. 🌊 The Core User Flow (Share-to-Save)
This is the most critical part of the app.

1.  **User Action:** The user is in another app (e.g., Gallery, Chrome). They tap the **Android Share icon**.
2.  **Share Sheet:** The user selects **"Add to Crucial Space"** from the list.
3.  **"Enrich" Screen:** Your app *doesn't* open to its main page. Instead, it launches a small, fast-loading `Activity` or `BottomSheet`. This screen contains:
    * A **thumbnail** of the shared content (e.g., the screenshot).
    * A **text field** for a quick note.
    * A **"Record" button** (microphone icon).
4.  **User Enrichment:**
    * **Scenario A (Text):** The user types, "Need to follow up on this with the design team."
    * **Scenario B (Voice):** The user taps Record, says, "This is the bug I was talking about, remind me to create a ticket for it tomorrow morning," and taps Stop.
5.  **Save:** The user taps **"Save"**.
6.  **Background Process:** The app saves the (image + text note) or (image + audio file) to its local database and dismisses the share screen. The user is immediately back in their original app.
7.  **AI Processing:** A background service in your app wakes up, sees the new, unprocessed item, and sends it to your backend to be processed by the Gemini API.

---

### 3. 🏛️ App Architecture & Tech Stack
To make this work securely and efficiently, we can't just put the Gemini API key in the app.

* **Frontend (Android App):**
    * **Language:** Kotlin (using Jetpack Compose for a modern UI).
    * **Database:** **Room Database** (for storing all "memories" locally for offline access).
    * **Networking:** **Retrofit** (to talk to your backend).
    * **Key Component:** An `Activity` registered in the `AndroidManifest.xml` to handle the `ACTION_SEND` intent. This is what makes your app appear in the Share Sheet.

* **Backend (Your Secure Server):**
    * **Why?** This is **critical for security**. You *never* embed your Gemini API key directly in a mobile app. This backend will be a simple proxy.
    * **Technology:** **Python (FastAPI)** or **Node.js (Express)**.
    * **Function:**
        1.  The Android app uploads the content (e.g., image + audio file) to this server.
        2.  The server *securely* calls the Gemini API with your key.
        3.  The server gets the JSON response from Gemini and sends it back to the app.

* **AI & APIs (The "Brain"):**
    * **Google Gemini API:** This is for the main analysis. We'll use a **multimodal model** (like Gemini Pro Vision) so we can send the **image AND the text** (or transcription) *together* for the best context.
    * **Google Speech-to-Text API:** While Gemini can handle audio, using the dedicated Speech-to-Text API is often more robust for pure transcription. Your backend would first transcribe the audio, then send that *text* plus the *image* to Gemini.

---

### 4. 📲 Key App Screens & UI
The app itself is simple and has two main parts.

#### Screen 1: The "Enrich" Sheet (The Share Target)
This is the screen that pops up when you share to the app.
* **Purpose:** Capture context *fast*.
* **UI:** Minimalist. Just the content thumbnail, a text box, a record button, and a "Save" button.

#### Screen 2: The Main "Space" (The Feed)
This is what the user sees when they open the Crucial Space app icon.
* **Purpose:** Review and search saved memories.
* **UI:** A chronological, scrollable list of all saved items (like a `RecyclerView` or `LazyColumn`). Each item is a "card."

#### Screen 3: The "Memory Detail" Screen
This is what you see when you tap on a card from the main feed.
* **Top:** The original content (e.g., the full screenshot).
* **Middle:** The user's note.
    * If text: Just the text.
    * If audio: An audio player with the original voice note.
* **Bottom (The AI Zone):** This is where the Gemini results go.
    * **Transcription:** (If audio) "You said: *'This is the bug I was talking about...'*"
    * **Summary:** "AI Summary: *This item is a bug report for the design team...'*"
    * **Action Items:** "Suggested To-Dos:"
        * `[ ]` Create a ticket for the bug.
    * **Reminders:** "Suggested Reminders:"
        * `[ ]` Follow up tomorrow morning. (With a button to "Add to Calendar").

---

### 5. 🤖 Gemini API Integration Plan
This is the core of the "magic." When your backend sends the content to Gemini, it will use a specific prompt.

**Example Backend Process:**

1.  App sends `image.jpg` and `note.wav` to your server.
2.  Server sends `note.wav` to **Speech-to-Text API** -> gets back a transcription: `"This is the bug..."`
3.  Server sends a multimodal request to the **Gemini API**:
    * The `image.jpg` file.
    * A text prompt:
        > "You are an AI assistant. A user saved this image with the following transcribed voice note: 'This is the bug I was talking about, remind me to create a ticket for it tomorrow morning.'
        >
        > Analyze the image and the note. Return a JSON object with this exact structure:
        >
        > ```json
        > {
        >   "summary": "A brief summary of what this memory is about.",
        >   "todos": ["A list of any actionable to-do items.", "Extract all of them."],
        >   "reminders": [
        >     { "event": "The event to be reminded of", "datetime": "The extracted ISO 8601 timestamp" }
        >   ]
        > }
        > ```"
4.  Gemini API returns the JSON.
5.  Your server sends this JSON back to the Android app, which then parses it and saves it to the local Room database to display on the "Memory Detail" screen.