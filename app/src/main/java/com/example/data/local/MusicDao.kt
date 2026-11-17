package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks ORDER BY sortOrder ASC, addedAt ASC")
    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(ref: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getTracksForPlaylistOnce(playlistId: Long): List<PlaylistTrackCrossRef>

    // Recently deleted playlists
    @Query("SELECT * FROM recently_deleted_playlists ORDER BY deletedAt DESC")
    fun observeRecentlyDeletedPlaylists(): Flow<List<RecentlyDeletedPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyDeletedPlaylist(item: RecentlyDeletedPlaylistEntity)

    @Query("DELETE FROM recently_deleted_playlists WHERE id = :id")
    suspend fun deleteRecentlyDeletedById(id: Long)

    // Track Stats & Favorites & Custom Metadata
    @Query("SELECT * FROM track_stats")
    fun observeAllTrackStats(): Flow<List<TrackStatsEntity>>

    @Query("SELECT * FROM track_stats WHERE trackId = :trackId LIMIT 1")
    suspend fun getTrackStatsOnce(trackId: String): TrackStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackStats(stats: TrackStatsEntity)

    @Query("SELECT * FROM track_stats WHERE isFavorite = 1 ORDER BY lastPlayedAt DESC")
    fun observeFavorites(): Flow<List<TrackStatsEntity>>

    @Query("SELECT * FROM track_stats WHERE playCount > 0 ORDER BY playCount DESC LIMIT 50")
    fun observeMostPlayed(): Flow<List<TrackStatsEntity>>

    @Query("SELECT * FROM track_stats WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 50")
    fun observeRecentlyPlayed(): Flow<List<TrackStatsEntity>>

    // Lyrics cache
    @Query("SELECT * FROM lyrics_cache WHERE trackKey = :trackKey LIMIT 1")
    suspend fun getLyricsByKey(trackKey: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLyrics(lyrics: LyricsEntity)

    // Online favorites
    @Query("SELECT * FROM online_favorites ORDER BY savedAt DESC")
    fun observeOnlineFavorites(): Flow<List<OnlineFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnlineFavorite(fav: OnlineFavoriteEntity)

    @Query("DELETE FROM online_favorites WHERE trackId = :trackId")
    suspend fun deleteOnlineFavorite(trackId: String)

    // Search history
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 15")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Listening sessions for stats
    @Query("SELECT * FROM listening_sessions ORDER BY timestamp DESC LIMIT 100")
    fun observeListeningSessions(): Flow<List<ListeningSessionEntity>>

    @Insert
    suspend fun insertListeningSession(session: ListeningSessionEntity)

    @Query("DELETE FROM listening_sessions")
    suspend fun clearListeningHistory()

    // Excluded folders
    @Query("SELECT * FROM excluded_folders ORDER BY addedAt DESC")
    fun observeExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcludedFolder(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :folderPath")
    suspend fun deleteExcludedFolder(folderPath: String)

    // Online play history for seamless continuation when local tracks finish
    @Query("SELECT * FROM online_play_history ORDER BY playedAt DESC LIMIT 50")
    fun observeOnlinePlayHistory(): Flow<List<OnlinePlayHistoryEntity>>

    @Query("SELECT * FROM online_play_history ORDER BY playedAt DESC LIMIT 50")
    suspend fun getOnlinePlayHistoryOnce(): List<OnlinePlayHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOnlinePlayHistory(entry: OnlinePlayHistoryEntity)

    // Saved videos (offline imported & online favorites)
    @Query("SELECT * FROM saved_videos ORDER BY addedAt DESC")
    fun observeSavedVideos(): Flow<List<SavedVideoEntity>>

    @Query("SELECT * FROM saved_videos WHERE videoId = :videoId LIMIT 1")
    suspend fun getSavedVideoOnce(videoId: String): SavedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedVideo(video: SavedVideoEntity)

    @Query("DELETE FROM saved_videos WHERE videoId = :videoId")
    suspend fun deleteSavedVideo(videoId: String)

    // Video resume positions & watch history
    @Query("SELECT * FROM video_progress ORDER BY lastWatchedAt DESC LIMIT 60")
    fun observeVideoProgress(): Flow<List<VideoPlaybackProgressEntity>>

    @Query("SELECT * FROM video_progress WHERE videoId = :videoId LIMIT 1")
    suspend fun getVideoProgressOnce(videoId: String): VideoPlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideoProgress(progress: VideoPlaybackProgressEntity)

    @Query("DELETE FROM video_progress")
    suspend fun clearVideoHistory()
}
