package com.example.ui

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.ListeningSessionEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.PreferencesRepository
import com.example.data.local.RecentlyDeletedPlaylistEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.TrackStatsEntity
import com.example.data.local.UserPreferences
import com.example.data.local.VideoPlaybackProgressEntity
import com.example.data.network.OnlineMusicRepository
import com.example.data.network.OnlineVideoRepository
import com.example.data.repository.MusicRepository
import com.example.data.scanner.LocalMusicScanner
import com.example.model.AlbumGroup
import com.example.model.ArtistGroup
import com.example.model.DynamicIslandStyle
import com.example.model.FolderGroup
import com.example.model.GenreGroup
import com.example.model.LyricsData
import com.example.model.RepeatModeState
import com.example.model.RingLightAction
import com.example.model.RingLightTheme
import com.example.model.SmartPlaylist
import com.example.model.ThemeAppearanceMode
import com.example.model.Track
import com.example.model.TrackSourceType
import com.example.model.VideoItem
import com.example.player.MusicPlayerController
import com.example.player.PlaybackSnapshot
import com.example.player.PlaybackStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AsyncLoadState<out T> {
    data object Idle : AsyncLoadState<Nothing>
    data object Loading : AsyncLoadState<Nothing>
    data class Success<T>(val data: T) : AsyncLoadState<T>
    data class Error(val message: String) : AsyncLoadState<Nothing>
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).musicDao()
    private val scanner = LocalMusicScanner(application)
    private val onlineRepo = OnlineMusicRepository()
    private val onlineVideoRepo = OnlineVideoRepository()
    private val musicRepo = MusicRepository(application, dao, scanner, onlineRepo)
    private val prefsRepo = PreferencesRepository(application)
    private val playerController = MusicPlayerController(application)

    val playbackState: StateFlow<PlaybackSnapshot> = PlaybackStateHolder.state

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _rawLocalTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val trackStats: StateFlow<List<TrackStatsEntity>> = musicRepo.allTrackStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = musicRepo.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistTrackRefs: StateFlow<List<PlaylistTrackCrossRef>> = musicRepo.playlistTrackRefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyDeletedPlaylists: StateFlow<List<RecentlyDeletedPlaylistEntity>> =
        musicRepo.recentlyDeletedPlaylists
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val onlineFavorites: StateFlow<List<Track>> = musicRepo.onlineFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = musicRepo.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listeningSessions: StateFlow<List<ListeningSessionEntity>> = musicRepo.listeningSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val excludedFolders: StateFlow<List<ExcludedFolderEntity>> = musicRepo.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val onlinePlayHistory: StateFlow<List<Track>> = musicRepo.onlinePlayHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localTracks: StateFlow<List<Track>> = combine(_rawLocalTracks, trackStats) { raw, stats ->
        val statMap = stats.associateBy { it.trackId }
        raw.map { t ->
            val st = statMap[t.id]
            if (st == null) t
            else t.copy(
                title = st.customTitle ?: t.title,
                artist = st.customArtist ?: t.artist,
                album = st.customAlbum ?: t.album,
                genre = st.customGenre ?: t.genre
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumGroup>> = combine(localTracks) { (tracks) ->
        tracks.groupBy { it.albumId to it.album }.map { (key, list) ->
            AlbumGroup(
                id = key.first,
                name = key.second,
                artist = list.first().artist,
                artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                tracks = list
            )
        }.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<ArtistGroup>> = combine(localTracks) { (tracks) ->
        tracks.groupBy { it.artist }.map { (artist, list) ->
            ArtistGroup(
                name = artist,
                artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                albumCount = list.map { it.album }.distinct().size,
                tracks = list
            )
        }.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderGroup>> = combine(localTracks) { (tracks) ->
        tracks.groupBy { it.folderPath }.map { (path, list) ->
            FolderGroup(
                path = path,
                name = path.trimEnd('/').substringAfterLast('/', "Device Storage"),
                tracks = list
            )
        }.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<GenreGroup>> = combine(localTracks) { (tracks) ->
        tracks.groupBy { it.genre }.map { (genre, list) ->
            GenreGroup(name = genre, tracks = list)
        }.sortedByDescending { it.tracks.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smartPlaylists: StateFlow<List<SmartPlaylist>> = combine(
        localTracks,
        onlineFavorites,
        trackStats
    ) { local, onlineFavs, stats ->
        val allTracks = (local + onlineFavs).distinctBy { it.id }
        val statsMap = stats.associateBy { it.trackId }
        musicRepo.buildSmartPlaylists(allTracks, statsMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _onlineSearchState = MutableStateFlow<AsyncLoadState<List<Track>>>(AsyncLoadState.Idle)
    val onlineSearchState: StateFlow<AsyncLoadState<List<Track>>> = _onlineSearchState.asStateFlow()

    private val _trendingState = MutableStateFlow<AsyncLoadState<List<Track>>>(AsyncLoadState.Idle)
    val trendingState: StateFlow<AsyncLoadState<List<Track>>> = _trendingState.asStateFlow()

    private val _lyricsState = MutableStateFlow<AsyncLoadState<LyricsData>>(AsyncLoadState.Idle)
    val lyricsState: StateFlow<AsyncLoadState<LyricsData>> = _lyricsState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Video Player & Library State
    private val _mediaStoreVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _isScanningVideos = MutableStateFlow(false)
    val isScanningVideos: StateFlow<Boolean> = _isScanningVideos.asStateFlow()

    private val savedVideoEntities = musicRepo.savedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineVideos: StateFlow<List<VideoItem>> = combine(savedVideoEntities, _mediaStoreVideos) { saved, mediaStore ->
        val importedOffline = saved.filter { !it.isOnline }.mapNotNull { musicRepo.decodeVideo(it.videoJson) }
        (importedOffline + mediaStore).distinctBy { it.uri }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteVideoIds: StateFlow<Set<String>> = combine(savedVideoEntities) { (saved) ->
        saved.filter { it.isFavorite }.map { it.videoId }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteVideos: StateFlow<List<VideoItem>> = combine(savedVideoEntities) { (saved) ->
        saved.filter { it.isFavorite }.mapNotNull { musicRepo.decodeVideo(it.videoJson) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoProgressList: StateFlow<List<VideoPlaybackProgressEntity>> = musicRepo.videoProgressList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoWatchHistory: StateFlow<List<Pair<VideoItem, VideoPlaybackProgressEntity>>> = combine(videoProgressList) { (entries) ->
        entries.mapNotNull { entry ->
            musicRepo.decodeVideo(entry.videoJson)?.let { it to entry }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _onlineVideoSearchState = MutableStateFlow<AsyncLoadState<List<VideoItem>>>(AsyncLoadState.Idle)
    val onlineVideoSearchState: StateFlow<AsyncLoadState<List<VideoItem>>> = _onlineVideoSearchState.asStateFlow()

    private val _onlineVideoTrendingState = MutableStateFlow<AsyncLoadState<List<VideoItem>>>(AsyncLoadState.Idle)
    val onlineVideoTrendingState: StateFlow<AsyncLoadState<List<VideoItem>>> = _onlineVideoTrendingState.asStateFlow()

    private val _activeVideoQueue = MutableStateFlow<List<VideoItem>>(emptyList())
    val activeVideoQueue: StateFlow<List<VideoItem>> = _activeVideoQueue.asStateFlow()

    private val _activeVideoIndex = MutableStateFlow(-1)
    val activeVideoIndex: StateFlow<Int> = _activeVideoIndex.asStateFlow()

    val onlinePlaylists: List<Pair<String, String>> = listOf(
        "Hindi Romantic Hits" to "hindi romantic arijit",
        "Bollywood Lofi & Chill" to "bollywood lofi chill",
        "Punjabi & Desi Party" to "punjabi hindi remix",
        "90s Evergreen Hindi" to "90s hindi retro songs",
        "Sufi & Unplugged Soul" to "hindi sufi acoustic unplugged"
    )

    private var scanJob: Job? = null
    private var mediaObserverJob: Job? = null
    private var videoObserverJob: Job? = null

    private val audioMediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (!hasAudioPermission()) return
            mediaObserverJob?.cancel()
            mediaObserverJob = viewModelScope.launch {
                delay(450L)
                scanDeviceLibrary()
            }
        }
    }

    private val videoMediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (!hasVideoPermission()) return
            videoObserverJob?.cancel()
            videoObserverJob = viewModelScope.launch {
                delay(450L)
                scanDeviceVideos()
            }
        }
    }

    init {
        playerController.connect()
        runCatching {
            application.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                audioMediaObserver
            )
            application.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                videoMediaObserver
            )
        }
        if (hasAudioPermission()) {
            scanDeviceLibrary()
        } else if (hasVideoPermission()) {
            scanDeviceVideos()
        }
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                PlaybackStateHolder.update { current ->
                    current.copy(
                        shuffleEnabled = prefs.shuffleEnabled,
                        repeatMode = prefs.repeatMode,
                        playbackSpeed = prefs.playbackSpeed,
                        playbackPitch = prefs.playbackPitch,
                        overlayEnabled = prefs.dynamicIslandOverlayEnabled,
                        inAppIslandEnabled = prefs.dynamicIslandInAppEnabled,
                        islandStyle = prefs.dynamicIslandStyle,
                        islandAlwaysExtended = prefs.dynamicIslandAlwaysExtended,
                        ringLightEnabled = prefs.ringLightEnabled,
                        ringLightXFraction = prefs.ringLightXFraction,
                        ringLightYDp = prefs.ringLightYDp,
                        ringLightSizeDp = prefs.ringLightSizeDp,
                        ringLightThicknessDp = prefs.ringLightThicknessDp,
                        ringLightTheme = prefs.ringLightTheme,
                        ringLightTapAction = prefs.ringLightTapAction,
                        ringLightSwipeLeftAction = prefs.ringLightSwipeLeftAction,
                        ringLightSwipeRightAction = prefs.ringLightSwipeRightAction,
                        ringLightSwipeUpAction = prefs.ringLightSwipeUpAction,
                        ringLightSwipeDownAction = prefs.ringLightSwipeDownAction
                    )
                }
            }
        }
    }

    private fun hasAudioPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            getApplication(),
            perm
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasVideoPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            getApplication(),
            perm
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun scanDeviceLibrary() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _isScanning.value = true
            val excluded = runCatching {
                musicRepo.excludedFolders.first().map { it.folderPath }.toSet()
            }.getOrElse { excludedFolders.value.map { it.folderPath }.toSet() }
            val scanned = musicRepo.scanLocalTracks(excluded)
            _rawLocalTracks.value = scanned
            // Auto-load the queue with local tracks if nothing is currently queued (no preloaded online songs)
            if (playbackState.value.currentTrack == null && scanned.isNotEmpty()) {
                PlaybackStateHolder.update {
                    it.copy(
                        queue = scanned,
                        currentIndex = 0,
                        currentTrack = scanned.first(),
                        durationMs = scanned.first().durationMs
                    )
                }
            }
            _isScanning.value = false
        }
        scanDeviceVideos()
    }

    fun scanDeviceVideos() {
        if (!hasVideoPermission()) {
            _mediaStoreVideos.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isScanningVideos.value = true
            _mediaStoreVideos.value = musicRepo.scanMediaStoreVideos()
            _isScanningVideos.value = false
        }
    }

    fun createDeleteVideoRequestIntent(video: VideoItem): PendingIntent? =
        musicRepo.createDeleteVideoRequestIntent(video)

    fun deleteVideoLegacyOrRefresh(video: VideoItem) {
        viewModelScope.launch {
            if (video.isOnline) {
                musicRepo.removeSavedVideo(video.id)
                showMessage("Removed \"${video.title}\" from list")
                return@launch
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                val ok = musicRepo.deleteVideoLegacy(video)
                if (ok) {
                    musicRepo.removeSavedVideo(video.id)
                    showMessage("Deleted \"${video.title}\"")
                } else {
                    showMessage("Could not delete \"${video.title}\"")
                }
            } else {
                musicRepo.removeSavedVideo(video.id)
                showMessage("Deleted \"${video.title}\"")
            }
            _mediaStoreVideos.value = _mediaStoreVideos.value.filterNot { it.id == video.id }
            scanDeviceVideos()
        }
    }

    suspend fun resolveYouTubeStream(youtubeId: String): String? =
        onlineVideoRepo.resolveDirectPlayableUrl(youtubeId)

    fun toggleVideoFavorite(video: VideoItem) {
        viewModelScope.launch {
            val fav = musicRepo.toggleVideoFavorite(video)
            showMessage(if (fav) "Saved video to Favourites" else "Removed from Favourite Videos")
        }
    }

    fun playVideos(videos: List<VideoItem>, startIndex: Int = 0) {
        if (videos.isEmpty()) return
        playerController.pauseIfPlaying()
        _activeVideoQueue.value = videos
        _activeVideoIndex.value = startIndex.coerceIn(0, videos.lastIndex)
    }

    fun playDirectVideoUrl(url: String, title: String = "") {
        val clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            showMessage("Please enter a valid http:// or https:// video URL")
            return
        }
        val item = onlineVideoRepo.buildDirectUrlVideo(clean, title)
        playVideos(listOf(item), 0)
    }

    fun switchVideoQueueIndex(index: Int) {
        val q = _activeVideoQueue.value
        if (index in q.indices) {
            _activeVideoIndex.value = index
        }
    }

    fun closeVideoPlayer() {
        _activeVideoIndex.value = -1
        _activeVideoQueue.value = emptyList()
    }

    fun saveVideoProgress(video: VideoItem, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            musicRepo.saveVideoProgress(video, positionMs, durationMs)
        }
    }

    fun clearVideoHistory() {
        viewModelScope.launch {
            musicRepo.clearVideoHistory()
            showMessage("Video watch history cleared")
        }
    }

    fun buildVideoShareIntent(video: VideoItem): Intent = musicRepo.buildVideoShareIntent(video)

    fun loadTrendingVideosIfNeeded() {
        if (_onlineVideoTrendingState.value is AsyncLoadState.Idle) {
            loadTrendingVideos()
        }
    }

    fun loadTrendingVideos() {
        viewModelScope.launch {
            _onlineVideoTrendingState.value = AsyncLoadState.Loading
            onlineVideoRepo.fetchTrendingVideos()
                .onSuccess { _onlineVideoTrendingState.value = AsyncLoadState.Success(it) }
                .onFailure { _onlineVideoTrendingState.value = AsyncLoadState.Error(it.message ?: "Unable to load online videos") }
        }
    }

    fun searchOnlineVideos(query: String) {
        viewModelScope.launch {
            _onlineVideoSearchState.value = AsyncLoadState.Loading
            onlineVideoRepo.searchOnlineVideos(query)
                .onSuccess { _onlineVideoSearchState.value = AsyncLoadState.Success(it) }
                .onFailure { _onlineVideoSearchState.value = AsyncLoadState.Error(it.message ?: "Video search failed") }
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        playerController.playQueue(tracks, startIndex)
    }

    fun playSingleTrack(track: Track) {
        playerController.playQueue(listOf(track), 0)
    }

    fun addToQueueNext(track: Track) {
        playerController.addToQueueNext(track)
        showMessage("Added \"${track.title}\" to play next")
    }

    fun playFromQueue(index: Int) = playerController.playFromQueueIndex(index)
    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)
    fun togglePlayPause() = playerController.togglePlayPause()
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun seekTo(ms: Long) = playerController.seekTo(ms)
    fun toggleShuffle() {
        val next = !playbackState.value.shuffleEnabled
        PlaybackStateHolder.update { it.copy(shuffleEnabled = next) }
        viewModelScope.launch { prefsRepo.setShuffleEnabled(next) }
        playerController.setShuffleEnabled(next)
        showMessage(if (next) "Shuffle On" else "Shuffle Off")
    }

    fun cycleRepeatMode() {
        val next = when (playbackState.value.repeatMode) {
            RepeatModeState.OFF -> RepeatModeState.ALL
            RepeatModeState.ALL -> RepeatModeState.ONE
            RepeatModeState.ONE -> RepeatModeState.OFF
        }
        PlaybackStateHolder.update { it.copy(repeatMode = next) }
        viewModelScope.launch { prefsRepo.setRepeatMode(next) }
        playerController.setRepeatMode(next)
        showMessage(
            when (next) {
                RepeatModeState.OFF -> "Repeat Off"
                RepeatModeState.ALL -> "Repeat All"
                RepeatModeState.ONE -> "Repeat One"
            }
        )
    }

    fun setSleepTimer(minutes: Int) {
        playerController.setSleepTimer(minutes)
        val msg = when {
            minutes == -1 -> "Sleep timer: stop after current song"
            minutes > 0 -> "Sleep timer set for $minutes min"
            else -> "Sleep timer off"
        }
        showMessage(msg)
    }

    fun previewFloatingOverlay() {
        val ctx = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
            showMessage("Enable floating overlay access first")
            return
        }
        if (playbackState.value.currentTrack == null) {
            showMessage("Play a song first to preview Dynamic Island")
            return
        }
        playerController.previewFloatingOverlay()
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val isFav = musicRepo.toggleFavorite(track)
            showMessage(if (isFav) "Added to Favourites" else "Removed from Favourites")
        }
    }

    fun createPlaylist(name: String, initialTrack: Track? = null) {
        viewModelScope.launch {
            val id = musicRepo.createPlaylist(name)
            if (initialTrack != null) {
                musicRepo.addTrackToPlaylist(id, initialTrack.id)
            }
            showMessage("Playlist \"$name\" created")
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            musicRepo.addTrackToPlaylist(playlistId, track.id)
            showMessage("Added \"${track.title}\" to playlist")
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            musicRepo.deletePlaylistSoft(playlist)
            showMessage("Moved \"${playlist.name}\" to Recently Deleted")
        }
    }

    fun restorePlaylist(deleted: RecentlyDeletedPlaylistEntity) {
        viewModelScope.launch {
            musicRepo.restorePlaylist(deleted)
            showMessage("Restored \"${deleted.originalName}\"")
        }
    }

    fun permanentlyDeletePlaylist(id: Long) {
        viewModelScope.launch { musicRepo.permanentlyDeleteArchivedPlaylist(id) }
    }

    fun updateTrackTags(track: Track, title: String, artist: String, album: String, genre: String) {
        viewModelScope.launch {
            musicRepo.updateTrackTags(track, title, artist, album, genre)
            showMessage("Track metadata updated")
        }
    }

    fun excludeFolder(path: String) {
        viewModelScope.launch {
            musicRepo.excludeFolder(path)
            scanDeviceLibrary()
            showMessage("Excluded folder from scan")
        }
    }

    fun includeFolder(path: String) {
        viewModelScope.launch {
            musicRepo.includeFolder(path)
            scanDeviceLibrary()
        }
    }

    fun detectDuplicates(): List<List<Track>> = musicRepo.detectDuplicates(localTracks.value)

    fun openAndPlayExternalUri(uri: Uri) {
        viewModelScope.launch {
            val track = musicRepo.extractExternalTrack(uri)
            playSingleTrack(track)
        }
    }

    fun buildShareIntent(tracks: List<Track>): Intent? = scanner.buildShareIntent(tracks)

    fun createDeleteRequestIntent(tracks: List<Track>): PendingIntent? =
        scanner.createDeleteRequestIntent(tracks)

    fun deleteTracksLegacyOrRefresh(tracks: List<Track>) {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                val count = scanner.deleteTracksLegacy(tracks)
                showMessage("Deleted $count song(s)")
            } else {
                showMessage("Deleted ${tracks.size} song(s)")
            }
            val deletedIds = tracks.map { it.id }.toSet()
            _rawLocalTracks.value = _rawLocalTracks.value.filterNot { it.id in deletedIds }
            scanDeviceLibrary()
        }
    }

    fun canWriteSystemSettings(): Boolean = musicRepo.canWriteSystemSettings()

    fun setAsRingtone(track: Track) {
        val result = musicRepo.setAsRingtone(track)
        showMessage(result.getOrElse { it.message ?: "Failed to set ringtone" })
    }

    fun loadTrendingIfNeeded() {
        if (_trendingState.value is AsyncLoadState.Idle) {
            loadTrending()
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            _trendingState.value = AsyncLoadState.Loading
            val clientId = preferences.first().jamendoClientId
            onlineRepo.fetchTrendingTracks(clientId)
                .onSuccess { _trendingState.value = AsyncLoadState.Success(it) }
                .onFailure { _trendingState.value = AsyncLoadState.Error(it.message ?: "Unable to load trending") }
        }
    }

    fun searchOnline(query: String) {
        viewModelScope.launch {
            musicRepo.saveSearchQuery(query)
            _onlineSearchState.value = AsyncLoadState.Loading
            val clientId = preferences.first().jamendoClientId
            onlineRepo.searchOnlineMusic(query, clientId)
                .onSuccess { _onlineSearchState.value = AsyncLoadState.Success(it) }
                .onFailure { _onlineSearchState.value = AsyncLoadState.Error(it.message ?: "Online search failed") }
        }
    }

    fun loadLyricsForTrack(track: Track) {
        viewModelScope.launch {
            _lyricsState.value = AsyncLoadState.Loading
            musicRepo.getOrFetchLyrics(track)
                .onSuccess { _lyricsState.value = AsyncLoadState.Success(it) }
                .onFailure { _lyricsState.value = AsyncLoadState.Error(it.message ?: "Lyrics unavailable") }
        }
    }

    fun saveManualLyrics(track: Track, text: String) {
        viewModelScope.launch {
            musicRepo.saveManualLyrics(track, text)
            loadLyricsForTrack(track)
            showMessage("Lyrics saved offline")
        }
    }

    fun importLrcFile(track: Track, uri: Uri) {
        viewModelScope.launch {
            musicRepo.importLrcFromUri(track, uri)
                .onSuccess { data ->
                    _lyricsState.value = AsyncLoadState.Success(data)
                    showMessage(
                        if (data.syncedLines.isNotEmpty()) "Synced LRC loaded (${data.syncedLines.size} lines)"
                        else "Lyrics file loaded"
                    )
                }
                .onFailure {
                    showMessage(it.message ?: "Failed to load LRC file")
                }
        }
    }

    fun markOnboardingPermissionsDone() = viewModelScope.launch {
        prefsRepo.setOnboardingPermissionsDone(true)
    }

    fun selectTheme(themeId: String) = viewModelScope.launch { prefsRepo.setTheme(themeId) }
    fun setAppearanceMode(mode: ThemeAppearanceMode) = viewModelScope.launch { prefsRepo.setAppearanceMode(mode) }
    fun setCustomAccent(argb: Long?) = viewModelScope.launch { prefsRepo.setCustomAccent(argb) }
    fun setDynamicIslandOverlay(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDynamicIslandOverlay(enabled) }
    fun setDynamicIslandInApp(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDynamicIslandInApp(enabled) }
    fun setDynamicIslandStyle(style: DynamicIslandStyle) {
        PlaybackStateHolder.update { it.copy(islandStyle = style) }
        viewModelScope.launch { prefsRepo.setDynamicIslandStyle(style) }
    }
    fun setDynamicIslandAlwaysExtended(alwaysExtended: Boolean) {
        PlaybackStateHolder.update { it.copy(islandAlwaysExtended = alwaysExtended) }
        viewModelScope.launch { prefsRepo.setDynamicIslandAlwaysExtended(alwaysExtended) }
    }
    fun setRingLightEnabled(enabled: Boolean) {
        PlaybackStateHolder.update { it.copy(ringLightEnabled = enabled) }
        viewModelScope.launch { prefsRepo.setRingLightEnabled(enabled) }
    }
    fun updateRingLightGeometry(
        xFraction: Float? = null,
        yDp: Float? = null,
        sizeDp: Float? = null,
        thicknessDp: Float? = null
    ) {
        PlaybackStateHolder.update { cur ->
            cur.copy(
                ringLightXFraction = (xFraction ?: cur.ringLightXFraction).coerceIn(0.05f, 0.95f),
                ringLightYDp = (yDp ?: cur.ringLightYDp).coerceIn(-12f, 160f),
                ringLightSizeDp = (sizeDp ?: cur.ringLightSizeDp).coerceIn(22f, 96f),
                ringLightThicknessDp = (thicknessDp ?: cur.ringLightThicknessDp).coerceIn(1.5f, 9f)
            )
        }
        viewModelScope.launch {
            prefsRepo.updateRingLightGeometry(
                xFraction = xFraction,
                yDp = yDp,
                sizeDp = sizeDp,
                thicknessDp = thicknessDp
            )
        }
    }
    fun resetRingLightPosition() {
        PlaybackStateHolder.update { cur ->
            cur.copy(
                ringLightXFraction = 0.5f,
                ringLightYDp = 10f,
                ringLightSizeDp = 42f,
                ringLightThicknessDp = 3.5f
            )
        }
        viewModelScope.launch {
            prefsRepo.updateRingLightGeometry(0.5f, 10f, 42f, 3.5f)
            showMessage("Ring Light reset to camera center")
        }
    }
    fun setRingLightTheme(theme: RingLightTheme) {
        PlaybackStateHolder.update { it.copy(ringLightTheme = theme) }
        viewModelScope.launch { prefsRepo.setRingLightTheme(theme) }
    }
    fun updateRingLightActions(
        tap: RingLightAction? = null,
        swipeLeft: RingLightAction? = null,
        swipeRight: RingLightAction? = null,
        swipeUp: RingLightAction? = null,
        swipeDown: RingLightAction? = null
    ) {
        PlaybackStateHolder.update { cur ->
            cur.copy(
                ringLightTapAction = tap ?: cur.ringLightTapAction,
                ringLightSwipeLeftAction = swipeLeft ?: cur.ringLightSwipeLeftAction,
                ringLightSwipeRightAction = swipeRight ?: cur.ringLightSwipeRightAction,
                ringLightSwipeUpAction = swipeUp ?: cur.ringLightSwipeUpAction,
                ringLightSwipeDownAction = swipeDown ?: cur.ringLightSwipeDownAction
            )
        }
        viewModelScope.launch {
            prefsRepo.updateRingLightActions(
                tap = tap,
                swipeLeft = swipeLeft,
                swipeRight = swipeRight,
                swipeUp = swipeUp,
                swipeDown = swipeDown
            )
        }
    }
    fun setGesturesEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setGesturesEnabled(enabled) }
    fun setLockScreenControls(enabled: Boolean) = viewModelScope.launch { prefsRepo.setLockScreenControls(enabled) }

    fun saveApiCredentials(jamendoId: String, customEndpoint: String) = viewModelScope.launch {
        prefsRepo.setApiCredentials(jamendoId, customEndpoint)
        showMessage("API settings saved")
    }

    fun updateAudioEngine(
        balance: Float? = null,
        speed: Float? = null,
        pitch: Float? = null,
        gapless: Boolean? = null,
        crossfadeSec: Int? = null,
        normalization: Boolean? = null,
        fadePlayPause: Boolean? = null
    ) {
        viewModelScope.launch {
            prefsRepo.updateAudioSettings(
                balance = balance,
                speed = speed,
                pitch = pitch,
                gapless = gapless,
                crossfadeSec = crossfadeSec,
                normalization = normalization,
                fadePlayPause = fadePlayPause
            )
        }
    }

    fun clearSearchHistory() = viewModelScope.launch { musicRepo.clearSearchHistory() }
    fun clearListeningHistory() = viewModelScope.launch { musicRepo.clearListeningHistory() }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val res = musicRepo.exportBackupToUri(
                uri = uri,
                playlists = playlists.value,
                refs = playlistTrackRefs.value,
                stats = trackStats.value,
                excluded = excludedFolders.value
            )
            showMessage(res.getOrElse { it.message ?: "Export failed" })
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val res = musicRepo.importBackupFromUri(uri)
            showMessage(res.getOrElse { it.message ?: "Import failed" })
        }
    }

    fun exportPlaylistM3u(uri: Uri, playlist: PlaylistEntity) {
        viewModelScope.launch {
            val ids = playlistTrackRefs.value.filter { it.playlistId == playlist.id }.map { it.trackId }.toSet()
            val tracks = localTracks.value.filter { it.id in ids }
            val res = musicRepo.exportPlaylistM3u(uri, playlist.name, tracks)
            showMessage(res.getOrElse { it.message ?: "M3U export failed" })
        }
    }

    fun showMessage(msg: String) {
        _statusBanner.value = msg
    }

    fun clearMessage() {
        _statusBanner.value = null
    }

    override fun onCleared() {
        runCatching {
            val cr = getApplication<Application>().contentResolver
            cr.unregisterContentObserver(audioMediaObserver)
            cr.unregisterContentObserver(videoMediaObserver)
        }
        playerController.release()
        super.onCleared()
    }
}
