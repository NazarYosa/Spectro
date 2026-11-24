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
3. **Decoupled Session Lifecycle & Independent Logout**:
   - **Independent Device Lifecycles**: Logging out on Android does **NOT** log out Windows, and logging out on Windows does **NOT** log out Android. Each device manages its account session independently.
   - **Clean Windows Guest Mode Reversion**: When Spectro Windows logs out, all account data, imported playlists (`state.userPlaylists`), liked tracks (`state.likedTracks`), and session tokens **MUST BE COMPLETELY PURGED**, returning the desktop application cleanly to Guest mode.
4. **Anti-Re-Pairing Guard (Single Active Account Constraint)**:
   - Once Spectro Windows has paired an account (`is_logged_in: true`), it **CANNOT** accept incoming pairing sync packets or generate new pairing codes until the user explicitly logs out first.
   - The pairing modal and settings interface must display "Akun Sudah Terhubung" and prompt the user to log out first before attempting to pair a different device or account.

---

## 5. Platform Resource & Footprint Constraints

1. **Android Package & Storage Ceiling**:
   - **Strict Size Limit**: The Android universal APK package size and initial storage consumption **MUST NEVER EXCEED 21 MB**.
   - Per-ABI APK builds (`arm64-v8a`, `armeabi-v7a`) must remain strictly under 15 MB.
   - ProGuard/R8 code shrinking and resource optimization must be actively maintained to preserve lightweight app delivery.
2. **Windows Desktop Resource & Memory Optimization**:
   - The Windows executable size, runtime memory (RAM) footprint, and background process consumption must be aggressively minimized without degrading audio quality, removing features, or introducing bugs.
   - Idle background audio nodes, redundant listeners, and cached image buffers must be freed immediately upon navigation or session change.

---

## 6. Audio Engine & Equalizer

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

## 7. UX & Localization Standards

1. **User-Friendly, Non-Technical Language**:
   - All user-facing strings must use friendly, natural phrasing (e.g., "Account Synchronization" instead of "MQTT Cloud Broker Relay", "Display Language" instead of "i18n Locale Selector", "Audio Quality" instead of "Bitrate & Opus Profile").
2. **System Language Adaptation**:
   - Windows app automatically detects the operating system language (English / Indonesian) and allows manual overriding via Settings.
3. **Modern Borderless Window Design**:
   - Windows client runs with custom frameless chrome (`frame: false`), custom titlebar controls (Minimize, Maximize/Restore, Close), and integrated navigation arrows with full browser-style history support.

---

## 8. Unified Versioning & Release Artifact Specification

To eliminate discrepancies between release tags, installed binaries, and downloaded filenames, all platforms and build pipelines must strictly comply with the **Spectro Unified Versioning Specification**:

### 1. Single Global Version Truth (Semantic Versioning)
- Version format strictly follows Semantic Versioning: `MAJOR.MINOR.PATCH` (e.g., `1.0.0`).
- **Zero Version Drift**: Android and Windows **MUST ALWAYS SHARE THE EXACT SAME VERSION NUMBER**. It is strictly forbidden for Android to be on `v1.0.5` while Windows is on `v1.0.0` or vice versa.

### 2. Synchronized Version Inventory
Whenever bumping the version number, the following files **MUST BE UPDATED IN LOCKSTEP**:
1. **Android Application**:
   - `spectro-android/build.gradle.kts`:
     ```kotlin
     val appVersionName = "1.0.1"
     ```
     *(The `versionCode` is computed automatically: `major * 10000 + minor * 100 + patch`).*
2. **Windows Desktop Application**:
   - `spectro-windows/package.json`:
     ```json
     "version": "1.0.1"
     ```
   - `spectro-windows/dist/app.js`:
     ```javascript
     const APP_VERSION = "1.0.1";
     ```
3. **Repository Documentation & Release References**:
   - `RULES.md`: Verify active version specification.
   - `README.md`: Update latest release badge and download references.
   - `docs/BUILD_GUIDE.md`: Update build command and artifact paths.

### 3. Standardized Release Artifact Naming Convention
All compiled binaries published to GitHub Releases or distributed to end users **MUST STRICTLY ADHERE** to the following naming pattern matching the active release version:

| Platform | Target Architecture | Official Release Artifact Name | Internal Output Path |
| :--- | :--- | :--- | :--- |
| **Android** | Universal (All Devices) | `Spectro-v<version>-universal.apk` | `spectro-android/build/outputs/apk/release/` |
| **Android** | 64-bit ARM (arm64-v8a) | `Spectro-v<version>-arm64-v8a.apk` | `spectro-android/build/outputs/apk/release/` |
| **Android** | 32-bit ARM (armeabi-v7a) | `Spectro-v<version>-armeabi-v7a.apk` | `spectro-android/build/outputs/apk/release/` |
| **Windows** | 64-bit Setup Installer (NSIS) | `Spectro-v<version>-Setup.exe` / `Spectro-Setup.exe` | `spectro-windows/release-build/` |
| **Windows** | 64-bit Portable Executable | `Spectro-v<version>-Portable.exe` / `Spectro.exe` | `spectro-windows/release-build/` |

### 4. Binary Integrity & Anti-Stale Build Guard
- **Pre-Build Verification**: Before releasing or tagging a version, the build pipeline must verify that the compiled `BuildConfig.VERSION_NAME` in the APK and the `productVersion` in the Windows executable match the target release tag.
- **Obsolete Binary Purge**: Build directories (`build/outputs/apk/release/` and `release-build/`) must be cleaned prior to release assembly to prevent bundling stale binaries from prior builds.

---

## 9. Search Categories & Media Thumbnail Fallback Integrity

1. **Interactive Category Filtering**:
   - The search interface must provide YouTube Music-style category filter pills:
     - **Semua (All)**: `params: null`
     - **Lagu (Songs)**: `params: "EgWKAQIIAWoKEAkQBRAKEAMQBA=="`
     - **Video (Videos)**: `params: "EgWKAQIQAWoKEAkQChAFEAMQBA=="`
     - **Artis (Artists)**: `params: "EgWKAQIgAWoKEAkQChAFEAMQBA=="`
     - **Album (Albums)**: `params: "EgWKAQIYAWoKEAkQChAFEAMQBA=="`
     - **Playlist (Playlists)**: `params: "EgeKAQQoAEABagoQAxAEEAoQCRAF"`
   - Switching categories retains the active search query and dynamically re-queries InnerTube without reloading the view.
   - Artists are presented in a circular avatar card grid (`border-radius: 50%`), Albums and Playlists in square cover card grids, and Songs/Videos in a detailed track table.

2. **Guaranteed Multi-Tier Thumbnail Fallback**:
   - Track thumbnails must never degrade to the generic app logo (`logo.png`) when a valid YouTube `videoId` exists.
   - Image fallback hierarchy:
     1. `t.art` (Original high-resolution InnerTube thumbnail URL)
     2. `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`
     3. `https://i.ytimg.com/vi/${videoId}/mqdefault.jpg`
     4. `logo.png` (Terminal fallback only if no video ID exists)
   - All image elements must include `referrerpolicy="no-referrer"` to prevent Google CDN `403 Forbidden` errors.

---

## 10. Project Structure

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
│   ├── installer.nsh          # NSIS Custom Uninstallation Script
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
