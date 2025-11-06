# Release Notes

## v1.0.1 (2024-12-19)

### Bug Fixes

- **Fixed reminder time offset issue**: Reminders were being set 1 hour later than requested when users specified times like "7pm". This was caused by Gemini interpreting user-specified times as UTC instead of local time. The fix updates both the Android client and backend prompts to explicitly instruct Gemini to interpret user times as local time (based on the device/server timezone) and then convert them to UTC correctly.

  **Technical Details:**
  - Updated `GeminiClient.kt` to include user's local timezone offset and current local datetime in the prompt
  - Updated `backend/app/gemini.py` to include server's local timezone offset and current local datetime
  - Added explicit instruction: "When the user mentions a time (e.g., '7pm', '3:30pm'), interpret it as LOCAL TIME in the user's timezone, then convert to UTC"

  **Impact:** Reminders will now be scheduled at the correct time as specified by the user, regardless of timezone.

