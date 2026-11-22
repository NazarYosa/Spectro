# Multi-Source Synced Lyrics Engine

Spectro incorporates a lyrics engine capable of synchronized word-by-word karaoke highlighting, timed line-by-line scrolling, and multi-source fallbacks.

---

## 1. Supported Providers

| Provider | Sync Precision | Description |
| :--- | :--- | :--- |
| **BetterLyrics** | Word-by-Word / Line | TTML lyrics synced to individual words. |
| **Paxsenix** | Word-by-Word / Line | High-fidelity Apple Music synchronized catalog. |
| **LyricsPlus** | Line-by-Line | Musixmatch synchronized lyrics mirror. |
| **LrcLib** | Line-by-Line & Static | Open-source crowd-sourced synchronized LRC database. |
| **KuGou** | Line-by-Line | Comprehensive Asian and international synchronized catalog. |
| **YouTube Music** | Static Plaintext | Official track lyrics provided by YouTube Music. |
| **YouTube Captions**| Line-by-Line | Automatic speech-to-text video captions as a final fallback. |

---

## 2. Parallel Probing & Dynamic Dropdown

To guarantee speed without locking the UI during waterfall searches:

1. **Parallel Availability Probe**:
   When a track starts playing, Spectro queries enabled providers in parallel using `Promise.allSettled()` (Windows) and concurrent coroutines (Android).
2. **Dynamic Dropdown Filter**:
   The player UI displays a sleek lyrics source selector (`.lyrics-source-pill`). **Only providers that returned valid lyrics for the current song appear in the dropdown.**
   - If a source returns HTTP 404, 401, or an empty result, it is omitted from the dropdown for that track.
3. **Per-Song Manual Override**:
   Users can tap the dropdown on the player screen to switch to any other available provider for that specific song.
   - When the track ends and the next song plays, the source automatically resets back to the user's default Settings priority order.

---

## 3. Visual & Typography Specifications

- **Active Line Color**: `#FFFFFF` (Pure high-contrast white).
- **Inactive Line Color**: `rgba(255, 255, 255, 0.45)`.
- **Transitions**: Smooth GPU-accelerated CSS `transform` and `opacity` transitions (`0.4s ease`).
- **Karaoke Highlighting**: When word-level timing is available, words light up progressively as they are sung.
