# Spectro Build & Packaging Guide

This guide details how to set up the development environment, compile binaries, and produce release artifacts for both Android and Windows.

---

## 1. System Requirements

- **Git**
- **Android**:
  - JDK 25 (Temurin recommended)
  - Android SDK (API 35, Build Tools 35.0.0+)
- **Windows**:
  - Node.js (v20 LTS or v22 LTS)
  - npm (v10+)
  - Windows 10 / 11 64-bit

---

## 2. Building Spectro for Android

### Release Build
```bash
cd spectro-android
./gradlew assembleRelease
```

Generated APKs will be output to:
`spectro-android/build/outputs/apk/release/`
- `spectro-universal-release.apk`
- `spectro-arm64-v8a-release.apk`
- `spectro-armeabi-v7a-release.apk`

---

## 3. Building Spectro for Windows

### Development Mode
```bash
cd spectro-windows
npm install
npm start
```

### Packaging Installers
To create the native NSIS installer (`Spectro-Setup.exe`) and portable binary (`Spectro.exe`):
```bash
cd spectro-windows
npm install
npm run dist
```

Outputs will be placed in `spectro-windows/release-build/`:
- `Spectro-Setup.exe`: Native Windows NSIS setup wizard with desktop & start menu shortcut support.
- `Spectro.exe`: Standalone portable executable.

---

## 4. Maintenance & Cache Cleanup

A utility script `clean_spectro.cmd` is located in `spectro-windows/clean_spectro.cmd`. Running this script:
1. Terminates any running `Spectro.exe` instances.
2. Purges legacy binary caches in `%LOCALAPPDATA%\Spectro`.
3. Clears temporary Electron and WebView2 caches in `%APPDATA%\Spectro`.
