package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class TrackSourceType {
    LOCAL,
    ONLINE_FULL,
    ONLINE_PREVIEW,
    RADIO_STREAM,
    EXTERNAL_URI
}

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = 0L,
    val genre: String = "Unknown",
    val durationMs: Long = 0L,
    val uri: String,
    val artworkUri: String? = null,
    val folderPath: String = "",
    val sizeBytes: Long = 0L,
    val dateAddedSec: Long = 0L,
    val mimeType: String = "audio/mpeg",
    val sourceType: TrackSourceType = TrackSourceType.LOCAL
) {
    val isLocal: Boolean get() = sourceType == TrackSourceType.LOCAL || sourceType == TrackSourceType.EXTERNAL_URI
    val isRadio: Boolean get() = sourceType == TrackSourceType.RADIO_STREAM
    val isOnlineFull: Boolean get() = sourceType == TrackSourceType.ONLINE_FULL
    val isPreviewOnly: Boolean get() = sourceType == TrackSourceType.ONLINE_PREVIEW
    val folderName: String
        get() = folderPath.trimEnd('/').substringAfterLast('/', "Device")
}

data class AlbumGroup(
    val id: Long,
    val name: String,
    val artist: String,
    val artworkUri: String?,
    val tracks: List<Track>
)

data class ArtistGroup(
    val name: String,
    val artworkUri: String?,
    val albumCount: Int,
    val tracks: List<Track>
)

data class FolderGroup(
    val path: String,
    val name: String,
    val tracks: List<Track>
)

data class GenreGroup(
    val name: String,
    val tracks: List<Track>
)

@Serializable
data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val country: String,
    val tags: String,
    val favicon: String?,
    val bitrate: Int = 128
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class LyricsData(
    val trackKey: String,
    val plainLyrics: String,
    val syncedLines: List<LyricLine>,
    val isManual: Boolean = false
)

enum class SmartPlaylistKind(val title: String, val subtitle: String) {
    RECENTLY_PLAYED("Recently Played", "Your latest listening timeline"),
    MOST_PLAYED("Most Played", "Heavy rotation tracks you replay"),
    RECENTLY_ADDED("Recently Added", "Fresh arrivals on your device"),
    FAVOURITES("Favourites", "Tracks you loved"),
    LATE_NIGHT_CHILL("Late Night Chill", "Ambient & laid-back listening mix"),
    ENERGY_BOOST("Energy Mix", "High-tempo picks from your habit"),
    DEEP_FOCUS("Deep Focus", "Long-form & instrumental flow")
}

data class SmartPlaylist(
    val kind: SmartPlaylistKind,
    val tracks: List<Track>
)

enum class RepeatModeState {
    OFF, ALL, ONE
}

enum class ThemeAppearanceMode(val label: String) {
    DARK("Obsidian Dark"),
    LIGHT("Crystal Light"),
    SYSTEM("Follow System")
}

enum class DynamicIslandStyle(val label: String, val subtitle: String) {
    CLASSIC_PILL("Obsidian Pill", "Sleek iOS black capsule"),
    WIDE_CAPSULE("Long Capsule", "Always-wide extended bar"),
    LIQUID_GLASS("Frost Glass", "Translucent crystal rim"),
    NEON_PULSE("Neon Aura", "Cyber spectrum glow"),
    VINYL_DISC("Retro Vinyl", "Spinning turntable disc"),
    AURORA_WAVE("Aurora Silk", "Vibrant gradient shimmer"),
    MINIMAL_NOTCH("Minimal Notch", "Ultra-compact clean matte")
}

enum class RingLightTheme(val label: String) {
    RAINBOW_FLOW("Rainbow Halo"),
    NEON_CYAN("Cyber Cyan"),
    SOLAR_GOLD("Golden Glow"),
    CRIMSON_PULSE("Crimson Pulse"),
    EMERALD_BEAM("Emerald Beam"),
    ARCTIC_WHITE("Pure White"),
    VIOLET_NEBULA("Violet Aura"),
    MUSIC_SYNC("Music Reactive")
}

enum class RingLightAction(val label: String) {
    SONG_POPUP("Song Control Popup"),
    PLAY_PAUSE("Play / Pause Song"),
    NEXT_SONG("Next Song"),
    PREVIOUS_SONG("Previous Song"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    MUTE_TOGGLE("Mute / Unmute"),
    FLASHLIGHT_TORCH("Torch On / Off"),
    OPEN_PLAYER("Open Music Player"),
    NONE("No Action")
}
