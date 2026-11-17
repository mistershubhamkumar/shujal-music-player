package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.ListeningSessionEntity
import com.example.data.local.LyricsEntity
import com.example.data.local.MusicDao
import com.example.data.local.OnlineFavoriteEntity
import com.example.data.local.OnlinePlayHistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.RecentlyDeletedPlaylistEntity
import com.example.data.local.SavedVideoEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.TrackStatsEntity
import com.example.data.local.VideoPlaybackProgressEntity
import com.example.data.network.OnlineMusicRepository
import com.example.data.scanner.LocalMusicScanner
import com.example.data.scanner.LocalVideoScanner
import com.example.model.LyricsData
import com.example.model.SmartPlaylist
import com.example.model.SmartPlaylistKind
import com.example.model.Track
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

@Serializable
data class PlaylistBackupDto(
    val name: String,
    val description: String = "",
    val trackIds: List<String> = emptyList()
)

@Serializable
data class LibraryBackupPayload(
    val exportedAt: Long = System.currentTimeMillis(),
    val playlists: List<PlaylistBackupDto> = emptyList(),
    val favoriteTrackIds: List<String> = emptyList(),
    val excludedFolders: List<String> = emptyList()
)

class MusicRepository(
    private val context: Context,
    private val dao: MusicDao,
    private val scanner: LocalMusicScanner,
    private val onlineRepo: OnlineMusicRepository,
    private val videoScanner: LocalVideoScanner = LocalVideoScanner(context)
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    val playlists: Flow<List<PlaylistEntity>> = dao.observePlaylists()
    val playlistTrackRefs: Flow<List<PlaylistTrackCrossRef>> = dao.observeAllPlaylistTracks()
    val recentlyDeletedPlaylists: Flow<List<RecentlyDeletedPlaylistEntity>> =
        dao.observeRecentlyDeletedPlaylists()
    val allTrackStats: Flow<List<TrackStatsEntity>> = dao.observeAllTrackStats()
    val onlineFavorites: Flow<List<Track>> = dao.observeOnlineFavorites().map { items ->
        items.mapNotNull { entity ->
            runCatching { json.decodeFromString<Track>(entity.trackJson) }.getOrNull()
        }
    }
    val searchHistory: Flow<List<SearchHistoryEntity>> = dao.observeSearchHistory()
    val listeningSessions: Flow<List<ListeningSessionEntity>> = dao.observeListeningSessions()
    val excludedFolders: Flow<List<ExcludedFolderEntity>> = dao.observeExcludedFolders()
    val onlinePlayHistory: Flow<List<Track>> = dao.observeOnlinePlayHistory().map { items ->
        items.mapNotNull { entity ->
            runCatching { json.decodeFromString<Track>(entity.trackJson) }.getOrNull()
        }
    }

    val savedVideos: Flow<List<SavedVideoEntity>> = dao.observeSavedVideos()
    val videoProgressList: Flow<List<VideoPlaybackProgressEntity>> = dao.observeVideoProgress()

    fun decodeVideo(videoJson: String): VideoItem? =
        runCatching { json.decodeFromString<VideoItem>(videoJson) }.getOrNull()

    suspend fun scanMediaStoreVideos(): List<VideoItem> = videoScanner.scanMediaStoreVideos()

    suspend fun importVideoUris(uris: List<Uri>): List<VideoItem> {
        val imported = mutableListOf<VideoItem>()
        for (uri in uris) {
            val video = videoScanner.extractVideoFromUri(uri, "Picked Videos")
            val existing = dao.getSavedVideoOnce(video.id)
            dao.upsertSavedVideo(
                SavedVideoEntity(
                    videoId = video.id,
                    videoJson = json.encodeToString(video),
                    isOnline = false,
                    isFavorite = existing?.isFavorite ?: false,
                    addedAt = System.currentTimeMillis()
                )
            )
            imported += video
        }
        return imported
    }

    suspend fun importVideoFolderTree(treeUri: Uri): List<VideoItem> {
        val found = videoScanner.scanFolderTreeUri(treeUri)
        found.forEach { video ->
            val existing = dao.getSavedVideoOnce(video.id)
            dao.upsertSavedVideo(
                SavedVideoEntity(
                    videoId = video.id,
                    videoJson = json.encodeToString(video),
                    isOnline = false,
                    isFavorite = existing?.isFavorite ?: false,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
        return found
    }

    suspend fun removeSavedVideo(videoId: String) {
        dao.deleteSavedVideo(videoId)
    }

    suspend fun toggleVideoFavorite(video: VideoItem): Boolean {
        val existing = dao.getSavedVideoOnce(video.id)
        val nextFav = !(existing?.isFavorite ?: false)
        if (existing == null && video.isOnline && !nextFav) {
            return false
        }
        if (video.isOnline && !nextFav) {
            dao.deleteSavedVideo(video.id)
        } else {
            dao.upsertSavedVideo(
                SavedVideoEntity(
                    videoId = video.id,
                    videoJson = json.encodeToString(video),
                    isOnline = video.isOnline,
                    isFavorite = nextFav,
                    addedAt = existing?.addedAt ?: System.currentTimeMillis()
                )
            )
        }
        return nextFav
    }

    suspend fun saveVideoProgress(video: VideoItem, positionMs: Long, durationMs: Long) {
        dao.upsertVideoProgress(
            VideoPlaybackProgressEntity(
                videoId = video.id,
                videoJson = json.encodeToString(video),
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(video.durationMs),
                lastWatchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearVideoHistory() {
        dao.clearVideoHistory()
    }

    fun buildVideoShareIntent(video: VideoItem): android.content.Intent =
        videoScanner.buildShareIntent(video)

    fun createDeleteVideoRequestIntent(video: VideoItem): android.app.PendingIntent? =
        videoScanner.createDeleteRequestIntent(video)

    suspend fun deleteVideoLegacy(video: VideoItem): Boolean =
        videoScanner.deleteVideoLegacy(video)

    suspend fun getOnlinePlayHistoryOnce(): List<Track> = dao.getOnlinePlayHistoryOnce().mapNotNull { entity ->
        runCatching { json.decodeFromString<Track>(entity.trackJson) }.getOrNull()
    }

    suspend fun scanLocalTracks(excluded: Set<String>): List<Track> =
        scanner.scanDeviceAudio(excluded)

    suspend fun extractExternalTrack(uri: Uri): Track =
        scanner.extractTrackFromUri(uri)

    fun detectDuplicates(tracks: List<Track>): List<List<Track>> =
        scanner.detectDuplicates(tracks)

    fun canWriteSystemSettings(): Boolean = scanner.canWriteSystemSettings()

    fun setAsRingtone(track: Track): Result<String> = scanner.setAsRingtone(track)

    suspend fun recordTrackPlayed(track: Track) {
        val existing = dao.getTrackStatsOnce(track.id)
        val now = System.currentTimeMillis()
        val updated = existing?.copy(
            playCount = existing.playCount + 1,
            lastPlayedAt = now,
            totalListeningMs = existing.totalListeningMs + track.durationMs.coerceAtLeast(15_000L)
        ) ?: TrackStatsEntity(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            genre = track.genre,
            playCount = 1,
            totalListeningMs = track.durationMs.coerceAtLeast(15_000L),
            lastPlayedAt = now
        )
        dao.upsertTrackStats(updated)
        dao.insertListeningSession(
            ListeningSessionEntity(
                trackId = track.id,
                trackTitle = track.title,
                artist = track.artist,
                durationListenedMs = track.durationMs.coerceAtLeast(15_000L),
                timestamp = now
            )
        )
        if (!track.isLocal && !track.isRadio) {
            dao.upsertOnlinePlayHistory(
                OnlinePlayHistoryEntity(
                    trackId = track.id,
                    trackJson = json.encodeToString(track),
                    playedAt = now
                )
            )
        }
    }

    suspend fun toggleFavorite(track: Track): Boolean {
        val existing = dao.getTrackStatsOnce(track.id)
        val newFav = !(existing?.isFavorite ?: false)
        val updated = existing?.copy(isFavorite = newFav) ?: TrackStatsEntity(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            genre = track.genre,
            isFavorite = newFav
        )
        dao.upsertTrackStats(updated)

        if (!track.isLocal) {
            if (newFav) {
                dao.insertOnlineFavorite(
                    OnlineFavoriteEntity(
                        trackId = track.id,
                        trackJson = json.encodeToString(track)
                    )
                )
            } else {
                dao.deleteOnlineFavorite(track.id)
            }
        }
        return newFav
    }

    suspend fun updateTrackTags(
        track: Track,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newGenre: String
    ) {
        val existing = dao.getTrackStatsOnce(track.id)
        val updated = (existing ?: TrackStatsEntity(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            genre = track.genre
        )).copy(
            customTitle = newTitle.trim().takeIf { it.isNotBlank() },
            customArtist = newArtist.trim().takeIf { it.isNotBlank() },
            customAlbum = newAlbum.trim().takeIf { it.isNotBlank() },
            customGenre = newGenre.trim().takeIf { it.isNotBlank() }
        )
        dao.upsertTrackStats(updated)
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return dao.insertPlaylist(
            PlaylistEntity(name = name.trim().ifBlank { "New Playlist" }, description = description)
        )
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        dao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun deletePlaylistSoft(playlist: PlaylistEntity) {
        val refs = dao.getTracksForPlaylistOnce(playlist.id)
        val idsJson = json.encodeToString(refs.map { it.trackId })
        dao.insertRecentlyDeletedPlaylist(
            RecentlyDeletedPlaylistEntity(
                originalName = playlist.name,
                trackIdsJson = idsJson
            )
        )
        dao.clearPlaylistTracks(playlist.id)
        dao.deletePlaylistById(playlist.id)
    }

    suspend fun restorePlaylist(deleted: RecentlyDeletedPlaylistEntity) {
        val newId = dao.insertPlaylist(PlaylistEntity(name = deleted.originalName))
        val trackIds = runCatching {
            json.decodeFromString<List<String>>(deleted.trackIdsJson)
        }.getOrDefault(emptyList())
        trackIds.forEachIndexed { idx, trackId ->
            dao.addTrackToPlaylist(
                PlaylistTrackCrossRef(playlistId = newId, trackId = trackId, sortOrder = idx)
            )
        }
        dao.deleteRecentlyDeletedById(deleted.id)
    }

    suspend fun permanentlyDeleteArchivedPlaylist(id: Long) {
        dao.deleteRecentlyDeletedById(id)
    }

    suspend fun excludeFolder(path: String) {
        dao.insertExcludedFolder(ExcludedFolderEntity(folderPath = path))
    }

    suspend fun includeFolder(path: String) {
        dao.deleteExcludedFolder(path)
    }

    suspend fun saveSearchQuery(query: String) {
        if (query.isNotBlank()) {
            dao.upsertSearchHistory(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun clearSearchHistory() = dao.clearSearchHistory()

    suspend fun clearListeningHistory() = dao.clearListeningHistory()

    suspend fun getOrFetchLyrics(track: Track): Result<LyricsData> {
        val cached = dao.getLyricsByKey(track.id)
        if (cached != null) {
            return Result.success(
                LyricsData(
                    trackKey = track.id,
                    plainLyrics = cached.plainLyrics,
                    syncedLines = OnlineMusicRepository.parseLrcLines(cached.syncedLrc),
                    isManual = cached.isManual
                )
            )
        }
        val fetched = onlineRepo.fetchSyncedLyrics(
            trackKey = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album
        )
        fetched.onSuccess { data ->
            val rawLrc = if (data.syncedLines.isNotEmpty()) {
                data.syncedLines.joinToString("\n") { line ->
                    val m = (line.timeMs / 60000).toString().padStart(2, '0')
                    val s = ((line.timeMs % 60000) / 1000).toString().padStart(2, '0')
                    val ms = ((line.timeMs % 1000) / 10).toString().padStart(2, '0')
                    "[$m:$s.$ms]${line.text}"
                }
            } else ""
            dao.upsertLyrics(
                LyricsEntity(
                    trackKey = track.id,
                    plainLyrics = data.plainLyrics,
                    syncedLrc = rawLrc,
                    isManual = false
                )
            )
        }
        return fetched
    }

    suspend fun saveManualLyrics(track: Track, text: String) {
        val synced = OnlineMusicRepository.parseLrcLines(text)
        val plain = text.replace(Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?]"), "").trim()
        dao.upsertLyrics(
            LyricsEntity(
                trackKey = track.id,
                plainLyrics = plain.ifBlank { text },
                syncedLrc = if (synced.isNotEmpty()) text else "",
                isManual = true
            )
        )
    }

    suspend fun importLrcFromUri(track: Track, uri: Uri): Result<LyricsData> = withContext(Dispatchers.IO) {
        runCatching {
            val lrcText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Could not read LRC file")
            val synced = OnlineMusicRepository.parseLrcLines(lrcText)
            val plain = lrcText.replace(Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?]"), "").trim()
            dao.upsertLyrics(
                LyricsEntity(
                    trackKey = track.id,
                    plainLyrics = plain.ifBlank { lrcText },
                    syncedLrc = if (synced.isNotEmpty()) lrcText else "",
                    isManual = true
                )
            )
            LyricsData(
                trackKey = track.id,
                plainLyrics = plain.ifBlank { lrcText },
                syncedLines = synced,
                isManual = true
            )
        }
    }

    fun buildSmartPlaylists(
        allTracks: List<Track>,
        statsMap: Map<String, TrackStatsEntity>
    ): List<SmartPlaylist> {
        val trackById = allTracks.associateBy { it.id }

        val recentlyPlayed = statsMap.values
            .filter { it.lastPlayedAt > 0 }
            .sortedByDescending { it.lastPlayedAt }
            .mapNotNull { trackById[it.trackId] }

        val mostPlayed = statsMap.values
            .filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .mapNotNull { trackById[it.trackId] }

        val recentlyAdded = allTracks
            .filter { it.isLocal }
            .sortedByDescending { it.dateAddedSec }
            .take(35)

        val favourites = statsMap.values
            .filter { it.isFavorite }
            .mapNotNull { trackById[it.trackId] }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val chill = allTracks.filter { track ->
            val g = track.genre.lowercase()
            val t = track.title.lowercase()
            g.contains("chill") || g.contains("ambient") || g.contains("acoustic") ||
                g.contains("jazz") || g.contains("lofi") || t.contains("night") ||
                track.durationMs in 180_000L..320_000L
        }.ifEmpty { allTracks.take(15) }.take(25)

        val energy = allTracks.filter { track ->
            val g = track.genre.lowercase()
            val plays = statsMap[track.id]?.playCount ?: 0
            g.contains("pop") || g.contains("rock") || g.contains("dance") ||
                g.contains("hip") || g.contains("edm") || plays >= 2
        }.ifEmpty { allTracks.shuffled().take(15) }.take(25)

        val focus = allTracks.filter { track ->
            val g = track.genre.lowercase()
            g.contains("instrumental") || g.contains("classical") || g.contains("soundtrack") ||
                track.durationMs >= 210_000L || (hour in 9..17 && track.isLocal)
        }.ifEmpty { allTracks.takeLast(15) }.take(25)

        return listOf(
            SmartPlaylist(SmartPlaylistKind.RECENTLY_PLAYED, recentlyPlayed),
            SmartPlaylist(SmartPlaylistKind.MOST_PLAYED, mostPlayed),
            SmartPlaylist(SmartPlaylistKind.RECENTLY_ADDED, recentlyAdded),
            SmartPlaylist(SmartPlaylistKind.FAVOURITES, favourites),
            SmartPlaylist(SmartPlaylistKind.LATE_NIGHT_CHILL, chill),
            SmartPlaylist(SmartPlaylistKind.ENERGY_BOOST, energy),
            SmartPlaylist(SmartPlaylistKind.DEEP_FOCUS, focus)
        )
    }

    suspend fun exportBackupToUri(
        uri: Uri,
        playlists: List<PlaylistEntity>,
        refs: List<PlaylistTrackCrossRef>,
        stats: List<TrackStatsEntity>,
        excluded: List<ExcludedFolderEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = LibraryBackupPayload(
                playlists = playlists.map { pl ->
                    PlaylistBackupDto(
                        name = pl.name,
                        description = pl.description,
                        trackIds = refs.filter { it.playlistId == pl.id }.map { it.trackId }
                    )
                },
                favoriteTrackIds = stats.filter { it.isFavorite }.map { it.trackId },
                excludedFolders = excluded.map { it.folderPath }
            )
            val serialized = json.encodeToString(payload)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(serialized.toByteArray())
            } ?: error("Could not open output file.")
            "Backup exported (${payload.playlists.size} playlists, ${payload.favoriteTrackIds.size} favourites)"
        }
    }

    suspend fun importBackupFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Could not open backup file.")

            if (text.trimStart().startsWith("#EXTM3U")) {
                val lines = text.lines().filter { it.isNotBlank() && !it.startsWith("#") }
                val plId = dao.insertPlaylist(PlaylistEntity(name = "Imported M3U Playlist"))
                lines.forEachIndexed { index, pathOrUri ->
                    dao.addTrackToPlaylist(
                        PlaylistTrackCrossRef(playlistId = plId, trackId = pathOrUri.trim(), sortOrder = index)
                    )
                }
                return@runCatching "Imported M3U playlist with ${lines.size} entries"
            }

            val payload = json.decodeFromString<LibraryBackupPayload>(text)
            payload.playlists.forEach { dto ->
                val newId = dao.insertPlaylist(PlaylistEntity(name = dto.name, description = dto.description))
                dto.trackIds.forEachIndexed { index, tid ->
                    dao.addTrackToPlaylist(
                        PlaylistTrackCrossRef(playlistId = newId, trackId = tid, sortOrder = index)
                    )
                }
            }
            payload.excludedFolders.forEach { folder ->
                dao.insertExcludedFolder(ExcludedFolderEntity(folderPath = folder))
            }
            "Restored ${payload.playlists.size} playlists and ${payload.excludedFolders.size} excluded folders"
        }
    }

    suspend fun exportPlaylistM3u(uri: Uri, playlistName: String, tracks: List<Track>): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sb = StringBuilder("#EXTM3U\n#PLAYLIST:$playlistName\n")
                tracks.forEach { t ->
                    sb.append("#EXTINF:${t.durationMs / 1000},${t.artist} - ${t.title}\n")
                    sb.append("${t.uri}\n")
                }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(sb.toString().toByteArray())
                } ?: error("Failed to write M3U file.")
                "Exported $playlistName (${tracks.size} tracks)"
            }
        }
}
