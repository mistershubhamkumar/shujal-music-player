package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val addedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

@Entity(tableName = "track_stats")
data class TrackStatsEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "Unknown",
    val playCount: Int = 0,
    val totalListeningMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbum: String? = null,
    val customGenre: String? = null
)

@Entity(tableName = "recently_deleted_playlists")
data class RecentlyDeletedPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val originalName: String,
    val trackIdsJson: String,
    val deletedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey val trackKey: String,
    val plainLyrics: String,
    val syncedLrc: String,
    val isManual: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "online_favorites")
data class OnlineFavoriteEntity(
    @PrimaryKey val trackId: String,
    val trackJson: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "listening_sessions")
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: String,
    val trackTitle: String,
    val artist: String,
    val durationListenedMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "online_play_history")
data class OnlinePlayHistoryEntity(
    @PrimaryKey val trackId: String,
    val trackJson: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_videos")
data class SavedVideoEntity(
    @PrimaryKey val videoId: String,
    val videoJson: String,
    val isOnline: Boolean = false,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_progress")
data class VideoPlaybackProgressEntity(
    @PrimaryKey val videoId: String,
    val videoJson: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

