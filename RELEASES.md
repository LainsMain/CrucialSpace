# Release Notes

## v1.0.3 (2024-11-16)

### Bug Fixes

- **Fixed Reddit preview image not saving**: Reddit images were displaying in the enrich screen but not being saved to memories. The preview image is now properly included when creating the memory.

## v1.0.2 (2024-11-16)

### New Features

- **Multi-language AI Support**: Added language preference setting allowing AI summaries, titles, todos, and reminders to be generated in the user's preferred language. Supports auto-detect or 12 specific languages (English, Spanish, French, German, Italian, Portuguese, Dutch, Polish, Russian, Japanese, Chinese, Korean).

- **Enhanced URL Handling**: URLs in the enrich screen now appear as editable bubbles above the text field, making it clear that both link and notes can be included. URLs can be edited or removed easily.

- **Copy to Clipboard**: Long-press any section in the detail screen (Summary, Note, To-Dos, Reminders, Links) to copy content to clipboard. Individual links can also be copied separately.

- **Custom Notification Icon**: Reminders now use a custom Crucial Space notification icon instead of the generic Android icon.

- **Voice + Text Combined Input**: Record voice notes and add text notes together. When recording, the send button changes to a save button. After saving, an audio badge shows the recording duration, allowing you to add text before sending.

- **Audio Indicators**: Memory cards in the feed now show a mic icon when they contain audio. Audio player is displayed above text notes in the detail view.

- **Audio Transcript Storage**: Voice recordings now store transcripts separately from text notes. Transcripts can be viewed by tapping the audio player in the detail screen.

- **Collections In-Screen Search**: The search button in Collections now opens a search field within the screen instead of navigating away. Search filters collections by name and description.

- **Collections Sorting**: Added sort options for collections: Newest first, Oldest first, Last modified, Name (A-Z), Name (Z-A).

- **Smart Collection Previews**: Collections now show intelligent preview images - single latest image for collections with less than 4 memories, or a 4-image collage for collections with 4 or more memories.

- **Reddit Image Extraction**: Improved Reddit link previews using Reddit's JSON API. Now reliably extracts images from single image posts, gallery posts, and video posts. Filters out user avatars and community icons.

- **Reddit Text Extraction**: Automatically extracts post titles and content from Reddit links and adds them to the memory note.

- **Auto-Update Checker**: Added "Check for updates" button in settings that checks GitHub releases for new versions. When an update is available, shows release notes and allows one-tap download and installation.

### Bug Fixes

- **Fixed Reddit Image Previews**: Reddit images were showing blank or displaying community icons/avatars instead of post content. Now uses Reddit's JSON API for reliable image extraction.

- **Improved URL Text Field**: URLs are now stored separately from text input, fixing issues with typing spaces when URLs are present in notes.

### Technical Changes

- Database migration to version 7 (added `audioTranscript` field)
- Enhanced logging for image extraction debugging
- Removed Shein-specific extraction (product images loaded via JavaScript only)

## v1.0.1 (2024-12-19)

### Bug Fixes

- **Fixed reminder time offset issue**: Reminders were being set 1 hour later than requested when users specified times like "7pm". This was caused by Gemini interpreting user-specified times as UTC instead of local time. The fix updates both the Android client and backend prompts to explicitly instruct Gemini to interpret user times as local time (based on the device/server timezone) and then convert them to UTC correctly.

  **Technical Details:**
  - Updated `GeminiClient.kt` to include user's local timezone offset and current local datetime in the prompt
  - Updated `backend/app/gemini.py` to include server's local timezone offset and current local datetime
  - Added explicit instruction: "When the user mentions a time (e.g., '7pm', '3:30pm'), interpret it as LOCAL TIME in the user's timezone, then convert to UTC"

  **Impact:** Reminders will now be scheduled at the correct time as specified by the user, regardless of timezone.

