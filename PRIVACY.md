# Spectro Privacy Policy

Spectro is a free, modern, and open-source music streaming client available on Android and Windows. This document details how Spectro respects and protects your privacy.

**Summary:** Spectro has no proprietary user account database, serves no advertisements, and contains zero tracking, analytics, or telemetry. All user data—including playback history, favorites, saved playlists, equalizer profiles, and application settings—resides exclusively on your local device.

---

## 1. Local Device Data Storage

The following data is stored locally on your device and is never transmitted to any private server:

- Listening history and recently played tracks.
- Liked songs and customized user playlists.
- Parametric equalizer settings, presets, and audio preferences.
- Search queries and UI layout preferences.
- Session credentials (if you sign in with YouTube Music, cookies remain strictly inside the local secure storage).

---

## 2. Zero Telemetry & Analytics

- **No Third-Party Analytics**: Spectro does not integrate Google Analytics, Firebase Analytics, Mixpanel, or any user telemetry SDKs.
- **No Crash Diagnostics**: No crash logs or system metrics are automatically collected or uploaded to third-party services (such as Sentry or Crashlytics).
- **No Periodic Telemetry Pings**: The application never pings background developer servers.

---

## 3. External API Communication

To stream music and fetch rich metadata on demand, Spectro communicates directly from your device to official and community open-source APIs:

- **YouTube Music**: Streams official audio tracks, albums, artist details, and playlists directly from YouTube CDN endpoints.
- **Synchronized Lyrics Providers**: Queries timed, word-by-word, and line-by-line lyrics directly from open services (*BetterLyrics, Paxsenix, LyricsPlus, LrcLib, KuGou*).
- **MusicBrainz**: Fetches open-source artist metadata, release tags, and genre taxonomy.
- **Spectro Connect Broker (MQTT)**: Facilitates instant 6-digit PIN device pairing via a public MQTT relay (`broker.emqx.io`). The pairing topic is temporary and transmits only public handshake tokens without personally identifiable information.
- **GitHub**: Checks the official repository (https://github.com/NazarYosa/Spectro) for application updates.

---

## 4. Children's Privacy

Spectro is not directed at children under the age of 13 and does not collect or track personal information from any user.

---

## 5. Open Source & Inquiries

Because Spectro is open source, its entire networking and data handling logic is completely transparent and open for inspection.

For inquiries, feature suggestions, or issue reporting, please visit:
https://github.com/NazarYosa/Spectro/issues
