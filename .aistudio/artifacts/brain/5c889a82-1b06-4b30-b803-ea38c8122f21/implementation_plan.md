# Amancipher.Ai — Advanced Cybernetic Voice & Device Control AI Assistant

Transform the application into **Amancipher.Ai**, an advanced cybernetic AI assistant for Android capable of device control via Accessibility Services and hardware APIs, multimodal Vision & screen inspection, Room Database long-term Memory Core, YouTube Data API v3 automation, WhatsApp messaging/calls with verbal/tap confirmation, and the exact sci-fi HUD & Settings aesthetic from the reference screenshots.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Confirmed choices from Phase 1:
> - **Confirmed Decision 1 (Device & Accessibility Control)**: Implement real Android hardware controls (Flashlight via `CameraManager`, Media/System Volume via `AudioManager`, Screen Brightness via window attributes/settings, App Launchers for Instagram, WhatsApp, Spotify, YouTube) alongside a real Android `AccessibilityService` (`AmancipherAccessibilityService`) capable of inspecting active window nodes, clicking elements by text, typing text, scrolling, and dispatching global media/navigation actions.
> - **Confirmed Decision 2 (Vision & Screen Inspection)**: Clicking the `VISION` pill in the bottom dock opens a dedicated **Vision & Screen Inspector Hub** supporting live camera capture, zero-permission Android Photo Picker image analysis via Gemini Vision, and real-time screen hierarchy inspection via the Accessibility Service.
> - **Confirmed Decision 3 (Branding & Persona)**: Default Assistant Name set to `Amancipher.Ai` and default User Name set to `Amancipher`, with a witty, helpful Hinglish/English persona that remembers user routines, preferences, notes, and facts in a local Room Database.

---

## 1. Overview & Core Concept

- **What It Does**: **Amancipher.Ai** is an all-in-one voice and device automation assistant. It listens and responds in witty Hinglish or English, controls device hardware (flashlight, brightness, volume), automates apps and UI actions through an Android Accessibility Service, searches and plays YouTube videos via YouTube Data API v3 or intent fallback, sends WhatsApp messages and initiates calls with an active confirmation policy, and stores long-term memories (routines, facts, preferences, notes) in a Room Database.
- **Target Audience**: Power users who want a JARVIS-style cybernetic HUD assistant (`Amancipher.Ai`) with genuine Android device control, persistent memory, and customizable AI models and voices.
- **Key Value**: Bridges conversational Gemini intelligence with deep Android system control and a faithful, high-contrast cyber-grid HUD interface.

---

## 2. User Experience & Visual Design (Matching Reference Screenshots)

### Main HUD Screen (Screenshot 4)
1. **Top Cybernetic Header Bar**:
   - **Left**: Glowing cyan hexagonal core badge with a 7-dot inner matrix icon, paired with the assistant title (`Amancipher.Ai`) and live status subtitle (`● Calm — monitoring systems`, `● Listening — capturing audio`, `● Processing — analyzing intent`, or `● Speaking — voice transmission`).
   - **Right**: Digital 24-hour clock (`20:11`) with animated 4-bar cyan signal meter and a hexagonal settings button (`╬`).
2. **Cyber-Grid Background & Radar Reactor Orb**:
   - Subtle dark teal/cyan coordinate grid (`#050B0E` base with `#0A2229` grid lines).
   - **Centerpiece Radar Orb**: Concentric segmented tick-mark rings, glowing cyan orbital nodes (4 cardinal/diagonal luminous dots on the inner track), dark obsidian inner chamber, and a glowing cyan center core with the signature 7-dot matrix pattern that pulses and rotates with voice amplitude.
3. **Framed `CONVERSATION` Panel**:
   - Header label `CONVERSATION` in tracked monospace typography.
   - Glassmorphic container with sharp cyan L-bracket corner accents.
   - Displays timestamped messages (`You · 20:10:47`, `Amancipher.Ai · 20:10:47`) with circular user/assistant avatars, Hindi/Hinglish/English script support, and interactive confirmation/action cards (WhatsApp confirm, YouTube player, Weather, Screen Inspector results).
4. **Bottom 4-Control Dock**:
   - **`[ ⏻ ONLINE / OFFLINE ]`** pill button on the left: toggles active monitoring / hands-free wake mode.
   - **`[ 🖥 VISION ]`** pill button: launches the multimodal Camera Vision & Screen Inspection modal.
   - **`( 🎙 )` Center Glowing Cyan Mic Button**: large circular neon cyan button with outer halo glow for tap-to-talk or interrupting speech.
   - **`[ 🕒 HISTORY ]`** pill button on the right: opens conversation history, saved memories, and text command input drawer.

### Full-Screen Settings Page (Screenshots 1, 2, 3)
1. **Sticky Header**: Back button (`<`), spaced title `S e t t i n g s`, and `SAVE` action button.
2. **API Key Card**:
   - Masked `GEMINI API KEY` input field with visibility toggle (`👁`), `Paste from clipboard` action link, and `● Key Configured` status indicator.
3. **AI Model Card**:
   - Dropdown selector featuring:
     - `Gemini 3.1 Flash Live (Ultra-Low Latency - Default)`
     - `Gemini 2.5 Flash (Balanced Speed & Reasoning)`
     - `Gemini 2.5 Pro (Deep Intelligence & Coding)`
4. **Voice Selector Card**:
   - 2x4 interactive pill grid: `Puck`, `Charon`, `Kore`, `Fenrir`, `Aoede`, `Leda`, `Orus`, `Zephyr` (active voice highlighted in solid glowing cyan `#5CE1E6` with dark text, modulating TTS pitch/rate and Gemini voice config).
5. **Personality Card**:
   - Mode dropdown (`Assistant Mode`, `Witty Cybernetic`, `Tactical JARVIS`, `Casual Friend`).
   - Selectable trait chips: `Friendly`, `Helpful`, `Witty`, `Hinglish or English`.
   - `USER NAME` input field (default `Amancipher`) and `ASSISTANT NAME` input field (default `Amancipher.Ai`).
6. **Permissions Card**:
   - Live count (`6/6 granted` or current count) for Microphone, Camera, Notifications, Accessibility Service, Write Settings (Brightness), and Network.
   - Glowing cyan `ALLOW ALL` button and expandable `Manage Permissions` list with one-tap system permission/accessibility settings launchers.
7. **Memory Core Card (Room Database)**:
   - Description: *"Short-term conversation buffer + Room Database long-term knowledge (Routines, Preferences, Notes, Facts)."*
   - Red-outlined `CLEAR` button to wipe memories and cyan-outlined `VIEW MEMORIES` button to inspect/add/delete stored facts and routines.
8. **YouTube Automation Card**:
   - `Enable YouTube Automation` switch.
   - `YOUTUBE API KEY` masked input field, `Paste from clipboard`, `VALIDATE KEY` button, and live status indicator (`Status: Key is empty` / `Status: Validated`).
9. **Mobile Control Card**:
   - Header: `WHAT AMANCIPHER.AI CAN DO ON YOUR PHONE` with interactive tap-to-run command examples:
     - `"Turn on / off flashlight"`
     - `"Set brightness to 50%"`, `"Increase brightness"`
     - `"Volume up / down"`, `"Set volume to 80%"`
     - `"Open Instagram"`, `"Open WhatsApp"`, `"Open Spotify"`
     - `"Send WhatsApp to Rahul saying I will be late"`
     - `"Call Rahul on WhatsApp"`, `"Video call Mom"`
     - `"Remember my gym starts at 6 PM"`, `"Forget routine"`
   - Footer status: `● WhatsApp Confirmation Policy: Active` (*"Amancipher.Ai will never send any message without explicit verbal confirmation."*).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real Room Database for Memory Core**:
  - *Chosen Approach*: Use Room (`@Entity`, `@Dao`, `RoomDatabase`) with KSP to persist long-term user memories (`MemoryEntity` with category: `Routine`, `Preference`, `Note`, `Fact`). Inject stored memories into every Gemini prompt so Amancipher.Ai remembers routines like *"my gym starts at 6 PM"* across sessions.
- **Decision 2: Real Android Hardware & Accessibility Service Control**:
  - *Chosen Approach*: Implement `DeviceController` for hardware actions (`CameraManager.setTorchMode` for flashlight, `AudioManager` for volume/media playback dispatch, window brightness + system settings intent for brightness, package/URI intents for Instagram/Spotify/WhatsApp) and `AmancipherAccessibilityService` for screen node inspection, element clicking, text typing, and scrolling.
- **Decision 3: Multimodal Vision & Screen Context**:
  - *Chosen Approach*: Provide both camera/photo visual analysis via Gemini multimodal API and live Accessibility screen node reading so the user can ask *"What is on my screen?"* or *"Analyze this photo"*.

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────────────┐
│                         MainActivity (Compose)                        │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────┐ │
│  │        HUD Main Screen          │  │    Full-Screen Settings     │ │
│  │ - Top Hex Bar & Digital Clock   │  │ - Gemini & YouTube API Keys │ │
│  │ - Radar Reactor Orb Canvas      │  │ - Model & 8-Voice Selector  │ │
│  │ - Framed CONVERSATION Box       │  │ - Personality & User Name   │ │
│  │ - [ONLINE][VISION][MIC][HISTORY]│  │ - Permissions & Memory Core │ │
│  └────────────────┬────────────────┘  └──────────────┬──────────────┘ │
└───────────────────┼──────────────────────────────────┼────────────────┘
                    │                                  │
┌───────────────────▼──────────────────────────────────▼────────────────┐
│                         NovaViewModel (MVVM)                          │
│  - UI State (Online/Offline, AssistantState, Active Screen/Modals)    │
│  - Conversation Buffer & Pending Confirmation State                   │
└─────┬─────────────────┬──────────────────┬──────────────────┬─────────┘
      │                 │                  │                  │
┌─────▼──────┐   ┌──────▼───────┐   ┌──────▼───────┐   ┌──────▼─────────┐
│AmancipherAI│   │ MemoryCoreDB │   │DeviceControl │   │Vision & Speech │
│  Engine    │   │ (Room + DAO) │   │ & A11yService│   │   Managers     │
│ - Gemini   │   │ - Routines   │   │ - Flashlight │   │ - STT / TTS    │
│   Vision & │   │ - Preferences│   │ - Volume/Brt │   │   (8 Voices)   │
│   Chat     │   │ - Notes/Facts│   │ - Screen Node│   │ - Camera/Photo │
│ - YouTube  │   │              │   │   Click/Type │   │   Analyzer     │
│   API v3   │   │              │   │ - App Launch │   │                │
└────────────┘   └──────────────┘   └──────────────┘   └────────────────┘
```

### Interactive Component & State Mapping
- **Top Hex Settings Button (`╬`)**: Switches smoothly between the HUD Main Screen and the Full-Screen Settings view (with `BackHandler` support).
- **`ONLINE` Button**: Toggles active listening / hands-free wake word monitoring (`Calm — monitoring systems` vs `Offline — standby`).
- **`VISION` Button**: Opens the Vision & Screen Inspection sheet where users can capture a camera photo, pick an image, or inspect current screen elements via `AmancipherAccessibilityService`, and ask Amancipher.Ai about it.
- **`HISTORY` Button**: Opens a sheet with full conversation history, a quick text-command input bar, and stored long-term Memory Core items.
- **Voice & Verbal Confirmation**: When a WhatsApp message or call command is issued, Amancipher.Ai asks for confirmation; saying *"Haan"*, *"Yes"*, *"Send it"*, or *"Bhej do"* (or tapping the confirmation button) immediately executes the action.
