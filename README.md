<div align="center">

  <img src="assets/banner.jpg" alt="SoundWave Banner" width="220" style="border-radius: 28px; box-shadow: 0 10px 30px rgba(157, 78, 221, 0.4);" />

  # 🌊 SoundWave
  ### **Premium • 100% Offline • Android Music Player**

  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.00-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
  [![Media3 ExoPlayer](https://img.shields.io/badge/Media3%20ExoPlayer-1.4.1-FF0055?style=for-the-badge&logo=googleplay&logoColor=white)](https://developer.android.com/media/media3)
  [![Room Database](https://img.shields.io/badge/Room-2.6.1-F58220?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
  [![Offline Guaranteed](https://img.shields.io/badge/Privacy-100%25%20Offline-00C853?style=for-the-badge&logo=shield&logoColor=white)]()
  [![Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)

  <p align="center">
    <strong>SoundWave</strong> is an audiophile-grade, native offline Android music player engineered with <strong>Kotlin</strong> and <strong>Jetpack Compose</strong>. Crafted for pure, uninterrupted musical immersion without accounts, subscriptions, analytics, cloud backends, or advertisements.
  </p>

  <p align="center">
    <a href="#-download--installation">📥 <b>Download Latest APK</b></a> •
    <a href="#-key-features">✨ <b>Features</b></a> •
    <a href="#-app-screens--architecture">📱 <b>Screens & Architecture</b></a> •
    <a href="#-tech-stack">🛠️ <b>Tech Stack</b></a> •
    <a href="#-developer">👨‍💻 <b>Developer</b></a>
  </p>
</div>

---

## 📥 Download & Installation

The ready-to-install debug APK is available directly in the project root:

| Package | Version | Minimum OS | File Size | Location |
| :--- | :--- | :--- | :--- | :--- |
| **SoundWave APK** | `v1.0.0` | Android 8.0 (API 26+) | ~24.7 MB | [`SoundWave-v1.0.0.apk`](./SoundWave-v1.0.0.apk) |

### Quick Install Steps:
1. Transfer **[`SoundWave-v1.0.0.apk`](./SoundWave-v1.0.0.apk)** to your Android smartphone or tablet.
2. Tap the APK in your file manager to install (allow *"Install from unknown sources"* if prompted).
3. Grant audio permission on first launch and enjoy your local library with zero telemetry!

---

## 🌟 Why SoundWave?

Traditional streaming and modern music players have become cluttered with trackers, account gates, and intrusive subscriptions. **SoundWave** is engineered from the ground up to restore ownership of your offline media:

- 🔒 **100% Offline by Design:** The `android.permission.INTERNET` manifest permission is completely absent. Zero network sockets can be created.
- 🚫 **No Accounts or Paywalls:** No sign-up screens, no passwords, no subscriptions.
- 🛡️ **Total Privacy:** Your playlists, listening history, custom equalizer profiles, and stats stay strictly on your local device storage.
- ✈️ **Flawless Airplane Mode:** All core playback, search, ringtone cutter, and sound effects run effortlessly with zero connectivity.
- ⚡ **Lightweight & Battery Efficient:** ExoPlayer hardware decoding with partial wake locks ensures all-day playback with minimal battery drain.

---

## ✨ Key Features

```
┌────────────────────────────────────────────────────────────────────────┐
│                        🌊 SOUNDWAVE SUITE                             │
├──────────────────┬──────────────────┬──────────────────┬───────────────┤
│ 💽 NOW PLAYING   │ 🎚️ AUDIO SUITE   │ 🎨 THEME ENGINE  │ 🚗 UTILITIES  │
│ • Vinyl Rotation │ • 5-Band EQ      │ • Dynamic Palette│ • Drive Mode  │
│ • Swipe Changer  │ • Bass Boost     │ • AMOLED Black   │ • Analytics   │
│ • 0.5x–2.0x Spd  │ • 3D Virtualizer │ • Custom Wallppr │ • Ringtone Cut│
│ • Lockscrn Svc   │ • Fade-In Volume │ • Custom Accents │ • Home Widget │
└──────────────────┴──────────────────┴──────────────────┴───────────────┘
```

### 💽 1. Signature "Now Playing" Experience
- **Interactive Rotating Vinyl Disc:** Realistic vinyl record animation that rotates during playback and freezes precisely when paused.
- **Gesture-Based Swipe Track Changer:** Swipe **left** on the vinyl disc to skip to the next track; swipe **right** to return to the previous track, complete with responsive spring physics.
- **Playback Speed Controller:** Native Sonic audio processing from **`0.50x`** to **`2.00x`** with instant pitch-preserved tempo adjustment.
- **Sleep Timer:** Automatic volume ramp-down and playback pause with configurable duration (5 to 120 minutes).
- **Interactive Scrubber:** Fine-grained seek slider displaying real-time elapsed and total duration timestamps.

### 🔊 2. Background Playback & Uninterrupted Audio Service
- **AndroidX Media3 Service:** Background playback powered by `SoundWavePlaybackService` (`MediaSessionService`).
- **Screen-Off Track Transitions:** Automatic, continuous next-track playback when the screen is locked, the app is minimized, or another application is active.
- **Audio Focus & Headset Disconnect:** Automatically pauses audio when headphones unplug or Bluetooth disconnects; gracefully handles incoming calls and navigation alerts.
- **Rich Notification & Lock Screen:** Full lock-screen media controls with cover art, playback buttons, and timeline scrubbers.

### 🎚️ 3. Built-In Equalizer & Sound Effects
- **5-Band Equalizer:** Fine-tune 60 Hz, 230 Hz, 910 Hz, 3.6 kHz, and 14 kHz frequency bands with dB gain sliders.
- **Bass Boost:** Hardware-accelerated low-frequency punch.
- **3D Surround Virtualizer:** Expands spatial acoustic separation for headphones.
- **Factory Presets:** Normal, Classical, Dance, Flat, Heavy Metal, Hip Hop, Jazz, Pop, Rock, and Custom.

### 🎨 4. Cinematic Theme Engine & Wallpapers
- **Dynamic Palette:** Real-time color extraction from album art using AndroidX Palette for adaptive background glows.
- **AMOLED True-Black Theme:** Saves battery life on OLED displays with `#000000` pitch-black backgrounds.
- **Curated Color Presets:** Graphite & Burgundy (Signature), AMOLED Crimson, Midnight Blue, Forest Green, and Purple Haze.
- **Persistent Custom Wallpapers:** Select any personal image with adjustable background dimming. Copied to private storage so it **never disappears across app launches or reboots**.

### 🚗 5. Smart Tools & Utilities
- **Driving Mode:** Simplified, oversized, high-contrast playback controls for safe navigation on the road.
- **Listening Analytics:** Offline tracking of total listening minutes, play counts, most-played tracks, and top artists.
- **Lossless Ringtone Trimmer:** Visually select start and end timestamps from any track to create custom notification and ringtone audio files.
- **ID3 Metadata Editor:** Modify song titles, artists, and album tags directly from the app.
- **Home Screen Widget:** 4x1 interactive widget with cover art, track info, play/pause, and skip controls.

---

## 📱 App Screens & Navigation

SoundWave utilizes a fluid, modern bottom navigation system with a central Now Playing interaction:

| Destination | Icon | Description |
| :--- | :---: | :--- |
| **Home** | 🏠 | Personalized dashboard with greetings, library statistics, recently played tracks, recently added music, and quick favorites. |
| **Songs** | 🎵 | Comprehensive library browser divided into 4 tabs: **Tracks**, **Albums**, **Artists**, and **Folders** with instant search, sorting, and drilldown modals. |
| **Now Playing** | 💽 | Signature playback screen featuring animated vinyl disc, swipe gestures, lyrics display, speed controls, equalizer shortcuts, and queue reordering. |
| **Playlists** | 📑 | Create custom offline playlists, reorder songs, view playlist statistics, or export and manage your favorite track collections. |
| **Settings** | ⚙️ | Full customization hub for themes, wallpapers, fade-in timers, excluded folder manager, audio effect shortcuts, and Developer credits. |

---

## 🛠️ Architecture & Tech Stack

SoundWave is built strictly following modern Android development best practices and official Google architectural guidelines:

```
app/src/main/java/com/bishify/player/
├── local/                  # Room Database, DAOs, Entities, Preferences DataStore
│   ├── SoundWaveDatabase.kt
│   ├── Daos.kt
│   ├── Entities.kt
│   └── PreferencesManager.kt
├── mediastore/             # ContentResolver MediaStore discovery scanner
│   └── MediaStoreScanner.kt
├── model/                  # Immutable domain models (Song, Album, Artist, Folder)
│   └── Models.kt
├── player/                 # MediaController client & queue state manager
│   └── PlaybackManager.kt
├── service/                # AndroidX Media3 Background Service & Audio Effects
│   ├── SoundWavePlaybackService.kt
│   └── AudioEffectsManager.kt
├── ui/                     # Jetpack Compose UI
│   ├── MainActivity.kt
│   ├── components/         # Reusable composables (BottomNav, TopBar, MiniPlayer, VinylDisc)
│   ├── screens/            # Home, Songs, NowPlaying, Equalizer, DrivingMode, Settings
│   ├── theme/              # Color, Type, Theme, DynamicPalette
│   └── viewmodel/          # MusicViewModel (Coroutines, StateFlow, MVI architecture)
├── util/                   # Ringtone trimming and audio file utilities
│   └── RingtoneTrimmer.kt
└── widget/                 # AppWidgetProvider for home screen widget
    └── SoundWaveAppWidgetProvider.kt
```

### Key Libraries & Specifications:

| Component | Library / API | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin `2.0.20` | Expressive, null-safe modern language |
| **UI Framework** | Jetpack Compose (BOM `2024.10.00`) | Declarative reactive UI system |
| **Design System** | Material 3 (Material You) | Modern dark theme, glassmorphic cards, tokens |
| **Audio Engine** | AndroidX Media3 ExoPlayer `1.4.1` | Hardware-accelerated multi-format playback |
| **Background Service** | AndroidX `MediaSessionService` | Persistent notification and lock screen playback |
| **Local Database** | Room `2.6.1` with KSP | Playlists, listening history, and favorites cache |
| **Preferences** | Jetpack DataStore Preferences `1.1.1` | Settings, volume ramp, and theme persistence |
| **Image Loading** | Coil Compose `2.7.0` | Asynchronous local album art rendering |
| **Palette Extraction** | AndroidX Palette `1.0.0` | Dynamic color extraction from album artwork |
| **State Management** | StateFlow, SharedFlow, ViewModel | Reactive, lifecycle-aware architecture |

---

## 👨‍💻 Developer & Author

SoundWave was conceptualized, designed, and developed by:

<div align="center">
  <h3>👑 Aashish Babu</h3>
  <p><em>Lead Developer & Software Architect</em></p>

  <a href="https://t.me/NeoCypher666">
    <img src="https://img.shields.io/badge/Telegram-@NeoCypher666-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram" />
  </a>
  &nbsp;
  <a href="https://github.com/AashishCyberH4CKS">
    <img src="https://img.shields.io/badge/GitHub-AashishCyberH4CKS-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub" />
  </a>
</div>

---

## 🔨 Building From Source

### Prerequisites:
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17 or JDK 21 (configured in `gradle.properties`)
- Android SDK Platform `android-34` / `android-35`

### Build Instructions:

```bash
# 1. Clone the repository
git clone https://github.com/AashishCyberH4CKS/SoundWave.git
cd SoundWave

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Assemble Debug APK
./gradlew assembleDebug

# Output APK will be generated at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License & Privacy Notice

```
SoundWave Music Player is 100% offline software.
No user data, track titles, listening habits, device identifiers,
or telemetry ever leaves your smartphone.
```

Distributed under proprietary open distribution for personal and educational use.

<div align="center">
  <sub>Made with ❤️ by <b>Aashish Babu</b> for music enthusiasts worldwide.</sub>
</div>
#   S o u n d W a v e  
 