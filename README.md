# 🎤 TwinMind Voice Recorder (Android)

An Android app built using **Kotlin + Jetpack Compose + WorkManager** for recording audio in the background with automatic session management.

> 🧩 **Note:** This submission demonstrates the recording and session management functionality.  
> Transcription and summary generation are scaffolded but not fully implemented for now.

---

## 🚀 Features Implemented

✅ **Core Recording Service**
- Foreground service using `MediaRecorder` that records audio in 30-second chunks.  
- Automatically rotates files and handles interruptions (phone calls, audio focus loss, headset connect/disconnect).
- Gracefully stops recording and stores chunk metadata locally.

✅ **UI Built with Jetpack Compose**
- **Record Tab:** Start / stop audio recording with live status updates.
- **Status Display:** Shows current recording state — e.g. “Recording…”, “Paused – Phone call”, “Stopped”.

✅ **WorkManager Integration**
- Each audio chunk triggers background workers for transcription and summary (currently placeholders).
- Demonstrates proper usage of `OneTimeWorkRequest` with network constraints and exponential backoff.

✅ **Permission Handling**
- Runtime permissions for `RECORD_AUDIO`, `READ_PHONE_STATE`, and `POST_NOTIFICATIONS` (Android 13+).

---

## ⚙️ Features Pending / Not Fully Functional

⚠️ **Transcription (In Progress)**  
- The current implementation uses Android’s `SpeechRecognizer`, which only supports live mic input.  
- The next version would integrate Gemini / Whisper API for file-based transcription.

⚠️ **Summary Generation (In Progress)**  
- The app currently displays a placeholder text (“📊 Summary will appear here…”).  
- Logic for AI-generated summaries can be connected through the `SummaryWorker`.

---

## 🏗️ Tech Stack

| Component | Technology |
|------------|-------------|
| UI | Jetpack Compose, Material 3 |
| Background Tasks | WorkManager |
| Audio Recording | MediaRecorder |
| Dependency Injection | Hilt (for repository injection) |
| Language | Kotlin |
| Target SDK | 34 (Android 14) |

---

## 📱 How to Build & Run

1. **Clone the Repository**
   ```bash
   git clone https://github.com/NishantA9/voice-recorder-compose.git
   cd voice-recorder-compose
