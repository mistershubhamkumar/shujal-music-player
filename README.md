# Shujal Music Player

A modern, minimalist Android Music Player built with **Kotlin**, **Jetpack Compose (Material 3)**, **Media3 ExoPlayer**, **Room Database**, and **DataStore**.

## Highlighted Features

- **Classic "Play Music" Home Player**: Clean soft-sculpted player home screen with spinning circular album art, lyrics toggle, tactile seekbar with timestamps, Share & Favourite controls, Repeat/Shuffle, Queue sheet, and Sleep Timer dialog.
- **Dynamic Island Floating Music Control**: System-wide floating Dynamic Island overlay (`WindowManager` + Compose) that appears at the top of the screen during background playback with persistent **Previous / Play-Pause / Next** controls, live audio bars, and expandable capsule.
- **Sequential First-Launch Setup**: Automatically walks through required permissions one by one on first install/open without cluttering Settings.
- **Long-Press Multi-Select**: Long press any song in your library to select multiple songs with checkboxes and share or delete them directly.
- **Smart Auto Playlists & Offline Library**: Automatic MediaStore audio scanner (MP3, AAC, FLAC, WAV, OGG, M4A, OPUS) across internal storage & SD card, browseable by Songs, Albums, Artists, Folders, Genres, Playlists, Smart Mixes, Favourites, and Online & Radio (loaded on demand with zero preloaded demo songs).
- **Audio Configuration (All Inside Settings)**: Crossfade smooth transitions (0s–12s), Sleep Timer (Off / 10m / 15m / 30m / 45m / 60m / End of Track), Gapless playback, Volume Normalization, Fade In/Out on Play & Pause, Stereo Balance, and Playback Speed & Pitch.
- **Synchronized Lyrics**: Automatic `.lrc` & plain lyrics lookup via LRCLIB, manual offline lyrics editor, and Room offline cache.
- **55+ Themes & Dark/Light Modes**: Classic Mist (matching sculpted light & dark themes), 54+ curated themes, custom accent swatches, and Dark / Light / System appearance modes.

## Building & Installing

1. Build via Gradle CLI:
   ```bash
   gradle assembleDebug
   ```
2. Optional: Configure a Jamendo Client ID inside **Settings -> Online API Configuration** for Creative Commons full-track streaming.
3. Install the generated APK from `APK/Shujal_Music_Player.apk` on Android 7.0+ (API 24+).
