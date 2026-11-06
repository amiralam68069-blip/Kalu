# NOVA - Futuristic Voice AI Assistant (JARVIS-Style)

A premium, futuristic voice AI assistant Android application built with Jetpack Compose. NOVA speaks in warm, casual Hinglish with friend-like personality, features a 3D-effect audio-reactive particle Canvas orb, and executes real Android actions including YouTube playback, WhatsApp messaging with pre-confirmation, calls, alarms, reminders, notes, and live Open-Meteo weather.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed during Phase 1:
> - **Architecture**: 100% Native Jetpack Compose app with custom glowing particle Canvas orb and native Android integration (WhatsApp intents, YouTube playback, Alarms & Notifications, Contacts, Room/DataStore persistence).
> - **Voice & Audio Pipeline**: Gemini AI backend with Speech Recognition and Android Text-to-Speech / real-time streaming audio response, supporting tap-to-talk, wake word detection, and interruptibility.
> - **Personality & Language**: Casual Hinglish (Hindi + English mix) with friendly reactions ("arre wah", "acha", "hmm", "suno"), light humor, and mood adaptation.

---

## 1. Overview & Core Concept

- **What It Does**: NOVA is an interactive personal AI companion inspired by JARVIS, designed specifically for Android. Users interact via voice (or typed text) and receive instant spoken responses in a natural, warm Hinglish tone. NOVA can automate daily tasks: playing music on YouTube, queuing WhatsApp messages to contacts with explicit voice confirmation, fetching live weather forecasts via Open-Meteo, setting alarms and reminders, managing to-do notes, and delivering personalized morning briefings.
- **Target Audience**: Users seeking an intuitive, voice-first companion with personality, local Indian language flavor (Hinglish), and rapid execution of everyday phone actions.
- **Key Value**: Combines cutting-edge conversational intelligence with practical Android device actions in a sci-fi HUD interface that feels responsive and alive.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Zero-State & Active Standby**:
   - The user opens NOVA into a full-bleed, edge-to-edge dark glassmorphic dashboard.
   - The center of the screen houses the **NOVA Core Orb**: concentric glowing rings and orbital particles pulsing rhythmically in Idle state.
   - HUD telemetry displays the current status ("STANDBY // NOVA READY"), ambient sound level, time, and quick-prompt action chips.
2. **Voice Interaction Loop**:
   - The user taps the glowing microphone or speaks "Hey Nova".
   - The Core Orb dynamically expands with luminous particle flux; a neon sound wave ripples at the base as speech is recognized in real time.
   - When speech ends, the state shifts to "Thinking" (accelerating orbital particles, subtle cyan-to-purple chromatic shifts).
   - NOVA responds in natural Hinglish with warm voice audio while synchronized text streams into the transcript card.
3. **Action Execution with Safe Confirmation**:
   - If the user says: *"Aman ko Hi message bhejo"*, NOVA looks up Aman in saved contacts, displays a confirmation card on screen, asks aloud *"Aman ko 'Hi' bhej du?"*, and upon user voice or button confirmation (*"Haan bhej do"*), launches WhatsApp with the pre-filled message.
   - If the user says: *"YouTube pe badiya sa song play karo"*, NOVA executes a search and opens the video/music player within the app or YouTube.
   - If the user asks for weather: *"Aaj mausam kaisa hai?"*, NOVA queries Open-Meteo for real-time temperature, condition, and rain probability without requiring an external API key.
4. **Command & Settings Customization**:
   - Quick-action drawer and settings sheet allow renaming the assistant (e.g., from "NOVA" to "FRIDAY" or "JARVIS"), modifying user nickname and interests, configuring Gemini API keys, managing phone contacts, and picking neon accent themes (Cyan Pulse, Ultraviolet, Crimson Neon).

### Visual Identity & Theme
- **Aesthetic Direction**: Sci-Fi Holographic Glassmorphism / Cybernetic HUD.
- **Color Palette**:
  - `Background`: Deep Obsidian Black (`#080A10`) with radial gradient backdrops (`#0F172A`).
  - `Primary Glow`: Neon Cyan (`#00F2FE` / `#4FACFE`).
  - `Secondary Accent`: Electric Violet (`#9D4EDD` / `#7928CA`).
  - `Glass Surfaces`: Dark translucent charcoal (`#141926` with 60% opacity) and 1dp neon borders (`#00F2FE` at 25% opacity).
  - `Text Hierarchy`: Crisp White (`#FFFFFF`) for titles, Ice Blue (`#D6F1FF`) for speech captions, and Muted Slate (`#7E8B9B`) for telemetry labels.
- **Typography & Motion**:
  - Clean futuristic monospace for HUD readouts and status meters (`FontFamily.Monospace`).
  - High-legibility sans-serif for transcripts and cards.
  - Smooth 60fps trigonometric particle physics on Compose `Canvas` reacting dynamically to audio levels and assistant states (`Idle`, `Listening`, `Thinking`, `Speaking`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom Jetpack Compose Canvas Orb vs Embedded WebGL WebView**:
  - *Chosen Approach*: A 100% native Compose Canvas with layered radial gradients, trigonometric rotating orbital arcs, particle swarm physics, and bezier audio waveforms.
  - *Why*: Delivers 60fps hardware-accelerated rendering with zero WebView overhead, instant startup time, native lifecycle awareness, and seamless touch responsiveness.
- **Decision 2: Safety Gate on Outbound Communication**:
  - *Chosen Approach*: Two-phase confirmation (voice prompt + visual card with "Confirm / Cancel") before triggering external communication (WhatsApp, SMS, Phone Call).
  - *Why*: Prevents accidental messages or calls triggered by background noise or misinterpreted speech.
- **Decision 3: Zero-Config Open-Meteo Weather Integration**:
  - *Chosen Approach*: Query Open-Meteo REST API using standard Retrofit/OkHttp for current location weather.
  - *Why*: Free, open-access, and requires no API keys, ensuring immediate out-of-the-box reliability.
- **Decision 4: Flexible Gemini Client with Local Fallback**:
  - *Chosen Approach*: Configurable API Key via `.env` / BuildConfig or in-app Settings dialog with built-in intent parsing for offline device operations.
  - *Why*: Allows seamless testing even before an API key is entered, while unlocking deep conversational intelligence when active.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   MainActivity UI                      │
│  ┌──────────────────┐  ┌─────────────────────────────┐ │
│  │   Top HUD Bar    │  │ Quick Action Chips (Row)    │ │
│  │ (Status/Battery) │  │ [Music][Msg][Weather][Notes]│ │
│  └──────────────────┘  └─────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │          NOVA Reactive Particle Canvas            │ │
│  │  (Rotational Arcs + Particle Swarm + Sine Waves)  │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │           Live Transcript & Action Card           │ │
│  │  (User Query, NOVA Hinglish Reply, Confirm Modal) │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │     Bottom Controls: Mic Pulse + Text Input       │ │
│  └───────────────────────────────────────────────────┘ │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    NovaViewModel                       │
│  - AssistantState (Idle, Listening, Thinking, Speaking)│
│  - TranscriptHistory & PendingAction Confirmation      │
│  - Settings & User Profile State                       │
└───────┬───────────────────┬───────────────────┬────────┘
        │                   │                   │
┌───────▼───────┐   ┌───────▼───────┐   ┌───────▼────────┐
│  GeminiClient │   │ AndroidVoice  │   │  ActionRouter  │
│  - Hinglish   │   │  - STT Engine │   │  - WhatsApp    │
│    Persona    │   │  - TTS Engine │   │  - Open-Meteo  │
│  - Tool/Intent│   │  - Waveform   │   │  - YouTube/Web │
│    Detection  │   │    Levels     │   │  - Alarms/Notes│
└───────────────┘   └───────────────┘   └────────────────┘
```

### Data Model & Persistence
- **NovaPreferences**: Assistant name, user name, preferred glow theme, wake word toggle, custom Gemini API key.
- **ContactEntity**: Name, phone number, relationship/notes for WhatsApp and call routing.
- **NoteItem**: Title, content, timestamp, and completion status.
- **AlarmReminder**: Label, time, and active status.

### Component & State Mapping
- `NovaOrbCanvas`: Accepts `AssistantState` and `audioAmplitude` (0.0 to 1.0f). Generates orbital rotation, expansion factor, particle turbulence, and glow rings.
- `VoiceManager`: Orchestrates Android `SpeechRecognizer` for voice input, decodes audio amplitude in real-time, and feeds `TextToSpeech` with customized female pitch and speech rate.
- `ActionRouter`: Dispatches verified actions to Android system intents (`Intent.ACTION_VIEW` for WhatsApp, `Intent.ACTION_DIAL`, `AlarmClock.ACTION_SET_ALARM`, and YouTube search).
- `SettingsBottomSheet`: Manages user credentials, assistant preferences, and contact records.

---

## 5. Verification Plan
- **Compilation Check**: Run `compile_applet` to ensure error-free Kotlin and Compose build.
- **UI & Layout Verification**: Verify full-bleed edge-to-edge layouts, navigation bar safe insets, and responsive card sizing.
- **Permission & Intent Handling**: Verify graceful runtime permission checks for microphone (`RECORD_AUDIO`) and notifications.
