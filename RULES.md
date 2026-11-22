# SPECTRO DEVELOPMENT RULES & ARCHITECTURE SPECIFICATION

This document serves as the ground-truth technical specification for all development on **Spectro** (Android and Windows). Every tested and proven feature **MUST BE PRESERVED** to prevent functional regression.

---

## 1. Anti-Regression & System Stability

1. **Feature Invariance**:
   - Proven core features (6-digit MQTT Device Sync, Single-Genre Radio Queue, High-Contrast Clean White Lyrics, Native NSIS Installer, Dynamic 7-Source Lyrics Waterfall) must never be altered or degraded without explicit architectural approval.
2. **Background Process Isolation (Zero Crash Guarantee)**:
   - All background network operations (MQTT Spectro Connect, WebSocket sync, polling, metadata retrieval) **MUST** be isolated within strict error boundaries:
     - **Android**: Wrapped in `try-catch` blocks and executed using dedicated `CoroutineExceptionHandler` instances.
     - **Windows**: Handled with `try-catch` blocks and `Promise.catch()` handlers.
   - Network interruptions or broker unavailability **MUST NEVER** cause application crashes, ANRs, audio stutters, or playback interruptions.
3. **Graceful Degradation**:
   - If an external provider (such as an individual lyrics source or audio stream) fails or returns HTTP 404/401, the system must transparently fall back to subsequent providers without user-visible errors.

---

## 2. Dynamic Radio Queue Algorithm

Every playback session across Android and Windows must adhere strictly to the following queue pipeline:

1. **Single-Genre & DNA Anchor**:
   - The queue is seeded strictly from the primary genre and mood characteristics of the seed song via YouTube Music Radio endpoints (`RDAMVM<id>`).
   - **Strict Negative Keyword Filtering**: The queue must aggressively prune non-official, derivative, or low-fidelity uploads:
     - `cover`, `karaoke`, `instrumental`, `slowed`, `reverb`, `sped up`, `speed up`, `tribute`, `parody`, `remake`, `guitar cover`, `piano cover`, `drum cover`, `vocal cover`, `ai cover`, `fancam`, `reaction`, `live at`, `live from`, `live in`, `unreleased`, `snippet`, `teaser`, `full album`, `loop`, `bass boosted`, `nightcore`, `8d audio`, `tiktok version`, `live concert`, `live performance`, `mashup`, `aesthetic`, `edit`, `tutorial`.
   - **Duplicate Suppression**: Strips out alternative versions, duplicate uploads, or songs with identical normalized titles.
2. **Tiered Popularity Dynamic Shuffling**:
   - To avoid stale or static queues:
     - The top pool of popular, chart-topping genre tracks (top 30 recommendations) and extended continuation tracks are fetched.
     - **Dynamic Shuffling**: The pool is dynamically randomized before assigning tracks into Tier 1 (first 14 tracks) and Tier 2 (next 14 tracks), with remaining pool tracks forming Tier 3.
     - **Fresh on Every Play Guarantee**: When navigating between tracks (e.g., Song A -> Song B -> returning to Song A), the queue generated for Song A **MUST ALWAYS BE UNIQUE** in track selection and sequence.
   - **Artist Interleaving**: Prevents clustering by ensuring no artist appears more than twice consecutively, maintaining artist diversity across the genre.
   - **Queue Structure**:
     - **Index 0**: Active seed song selected by the user.
     - **Index 1..N**: Dynamically shuffled, popularity-tiered, artist-interleaved genre recommendations.
3. **Click Transition Guard**:
   - On Windows/Desktop, manual track changes within the queue must employ a transition timestamp guard (`lastTrackLoadTime`) to prevent spurious YouTube player `ENDED` events from skipping the newly loaded track.
4. **Infinite & Self-Extending Playback**:
   - Clicking any track in the queue initiates playback immediately, preserves prior playback history, and dynamically repopulates upcoming tracks with 50 fresh, randomized genre-matched suggestions rooted in the selected song.
   - When the remaining queue shrinks to 5 or fewer tracks, the player automatically requests radio continuation chunks, guaranteeing an endless music stream that **NEVER loops back to the start**.

---

## 3. Lyrics Engine & Provider Hierarchy

1. **Visual Clarity & Typography**:
   - Active lyrics text is displayed in pure, high-contrast white (`#FFFFFF`) without distracting text-shadows or color tinting.
2. **Smooth GPU Transitions**:
   - Transitions between active lines use GPU-accelerated CSS `opacity` and `transform` (`0.4s ease`) paired with `scroll-behavior: smooth`.
3. **No Horizontal Scroll**:
   - The lyrics view is strictly locked with `overflow-x: hidden`.
4. **Custom 7-Source Waterfall Hierarchy**:
   - Users have full control in Settings to reorder priorities and toggle providers individually:
     1. `BetterLyrics (Word-by-word synced)`
     2. `Paxsenix / Apple Music (Line & word synced)`
     3. `LyricsPlus / Musixmatch (Synchronized)`
     4. `LrcLib (Community synchronized & plain)`
     5. `KuGou (Comprehensive Asian & global catalog)`
     6. `YouTube Music (Official static lyrics)`
     7. `YouTube Captions (Auto-generated video captions)`
   - The engine strictly honors user-defined order in `spectro_lyrics_settings`.
5. **Parallel Availability Probing & Dynamic Dropdown**:
   - For every active song, lyrics sources are probed in parallel.
   - The player displays a streamlined dropdown displaying **only sources that actually returned lyrics** for the current song.
   - Selecting a source from the dropdown temporarily overrides lyrics for that song only, resetting automatically when the track changes.

---

## 4. Cross-Device Account Sync (Spectro Connect)

1. **Instant 6-Digit PIN Pairing**:
   - Secure, frictionless handshake via an MQTT broker relay (`broker.emqx.io`) on topic `spectro/sync/pair/<6_DIGIT_PIN>`.
   - Eliminates WebView Google login blocks and complex OAuth Client ID configurations.
2. **Bi-Directional Connected Device Status**:
   - Android shows: `Connected with: Spectro Windows (Desktop PC)`.
   - Windows shows: `Connected with: Android (<Device Model>)`.
   - Both clients exchange two-way ACK payloads upon pairing.

---

## 5. Audio Engine & Equalizer

1. **Real-Time Web Audio API Pipeline (Windows)**:
   - Uses an active `AudioContext` connected directly to the audio element via `createMediaElementSource`.
   - Implements a 5-band parametric equalizer:
     - 60 Hz (Lowshelf)
     - 230 Hz (Peaking)
     - 910 Hz (Peaking)
     - 3.6 kHz (Peaking)
     - 14 kHz (Highshelf)
   - Dedicated bass-boost filter (100 Hz Lowshelf).
   - Dynamic parameter updates are smoothly interpolated with `setTargetAtTime`.
2. **Native Audio Effects (Android)**:
   - Integrates with Android `Equalizer` and `BassBoost` audio session effects with smooth presets and custom frequency adjustments.

---

## 6. UX & Localization Standards

1. **User-Friendly, Non-Technical Language**:
   - All user-facing strings must use friendly, natural phrasing (e.g., "Account Synchronization" instead of "MQTT Cloud Broker Relay", "Display Language" instead of "i18n Locale Selector", "Audio Quality" instead of "Bitrate & Opus Profile").
2. **System Language Adaptation**:
   - Windows app automatically detects the operating system language (English / Indonesian) and allows manual overriding via Settings.
3. **Modern Borderless Window Design**:
   - Windows client runs with custom frameless chrome (`frame: false`), custom titlebar controls (Minimize, Maximize/Restore, Close), and integrated navigation arrows with full browser-style history support.

---

## 7. Project Structure

```
Spectro/
├── spectro-android/           # Android Native Application
│   ├── src/main/java/...      # Kotlin, Jetpack Compose, ExoPlayer
│   ├── src/main/res/          # Layouts, Drawables, Multi-Language strings.xml
│   └── build.gradle.kts       # Android build config (Version 1.0.0)
├── spectro-windows/           # Windows Desktop Application (Electron)
│   ├── main.js                # Main Process, IPC Handlers, Window Management
│   ├── preload.js             # Secure Context Bridge
│   ├── package.json           # Electron & Builder Config (Version 1.0.0)
│   ├── clean_spectro.cmd      # System & Cache Maintenance Script
│   └── dist/                  # UI Application (index.html, app.js, style.css)
├── docs/                      # Comprehensive Technical Documentation
│   ├── ARCHITECTURE.md        # System Architecture & Design
│   ├── QUEUE_ALGORITHM.md     # Queue Pipeline & Recommendation Logic
│   ├── LYRICS_ENGINE.md       # Lyrics Waterfall & Provider Specs
│   ├── CROSS_DEVICE_SYNC.md   # MQTT Protocol & Handshake Workflow
│   └── BUILD_GUIDE.md         # Building & Packaging Instructions
├── .github/workflows/         # CI/CD Workflows
│   └── release.yml            # Multi-Platform GitHub Release Workflow
├── README.md                  # Project Overview & Quickstart (English)
├── RULES.md                   # Engineering Standards & Architecture Rules (English)
└── PRIVACY.md                 # Privacy Policy (English)
```
