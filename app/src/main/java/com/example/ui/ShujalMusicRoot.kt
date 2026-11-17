package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.AdMobManager
import com.example.data.local.PlaylistEntity
import com.example.model.RingLightAction
import com.example.model.Track
import com.example.model.VideoItem
import com.example.ui.components.DynamicIslandCapsule
import com.example.ui.components.InAppCameraRingLightLayer
import com.example.ui.components.LiquidAuroraBackground
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.RingLightHardwareHelper
import com.example.ui.screens.EditTrackTagsDialog
import com.example.ui.screens.ExploreOnlineScreen
import com.example.ui.screens.HomePlayerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrackOptionsSheet
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.ShujalMusicTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object LibraryRoute
@Serializable data object VideosRoute
@Serializable data object SettingsRoute

private data class NavTab(
    val route: Any,
    val routeName: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NAV_TABS = listOf(
    NavTab(HomeRoute, "HomeRoute", "Play Music", Icons.Rounded.MusicNote, Icons.Outlined.MusicNote),
    NavTab(LibraryRoute, "LibraryRoute", "Library", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic),
    NavTab(VideosRoute, "VideosRoute", "Videos", Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary),
    NavTab(SettingsRoute, "SettingsRoute", "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
)

@Composable
fun ShujalMusicRoot(
    openNowPlayingOnLaunch: Boolean = false,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistRefs by viewModel.playlistTrackRefs.collectAsStateWithLifecycle()
    val deletedPlaylists by viewModel.recentlyDeletedPlaylists.collectAsStateWithLifecycle()
    val trackStats by viewModel.trackStats.collectAsStateWithLifecycle()
    val onlineFavorites by viewModel.onlineFavorites.collectAsStateWithLifecycle()
    val onlinePlayHistory by viewModel.onlinePlayHistory.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val listeningSessions by viewModel.listeningSessions.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val smartPlaylists by viewModel.smartPlaylists.collectAsStateWithLifecycle()
    val onlineSearchState by viewModel.onlineSearchState.collectAsStateWithLifecycle()
    val trendingState by viewModel.trendingState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val isScanningVideos by viewModel.isScanningVideos.collectAsStateWithLifecycle()
    val offlineVideos by viewModel.offlineVideos.collectAsStateWithLifecycle()
    val favoriteVideoIds by viewModel.favoriteVideoIds.collectAsStateWithLifecycle()
    val favoriteVideos by viewModel.favoriteVideos.collectAsStateWithLifecycle()
    val videoWatchHistory by viewModel.videoWatchHistory.collectAsStateWithLifecycle()
    val videoProgressList by viewModel.videoProgressList.collectAsStateWithLifecycle()
    val onlineVideoSearchState by viewModel.onlineVideoSearchState.collectAsStateWithLifecycle()
    val onlineVideoTrendingState by viewModel.onlineVideoTrendingState.collectAsStateWithLifecycle()
    val activeVideoQueue by viewModel.activeVideoQueue.collectAsStateWithLifecycle()
    val activeVideoIndex by viewModel.activeVideoIndex.collectAsStateWithLifecycle()

    var hasAudioPermission by remember { mutableStateOf(checkAudioPermission(context)) }
    var hasVideoPermission by remember { mutableStateOf(checkVideoPermission(context)) }
    var showNowPlaying by remember { mutableStateOf(openNowPlayingOnLaunch && playback.currentTrack != null) }
    var optionsTrack by remember { mutableStateOf<Track?>(null) }
    var editingTagsTrack by remember { mutableStateOf<Track?>(null) }
    var pendingM3uPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var pendingDeleteTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var pendingDeleteVideo by remember { mutableStateOf<VideoItem?>(null) }
    var pendingLrcTrack by remember { mutableStateOf<Track?>(null) }
    var ringLightCursorEditing by remember { mutableStateOf(false) }
    var showRingLightSongPopup by remember { mutableStateOf(false) }
    var requestedUpgradeVideoPerm by remember { mutableStateOf(false) }

    // First-install sequential onboarding steps: 0=audio, 1=video, 2=notifications, 3=floating overlay, 4=done
    var prefsReady by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableIntStateOf(0) }
    var showOverlayPrompt by remember { mutableStateOf(false) }
    var awaitingOverlayReturn by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val audioPermissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val videoPermissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.scanDeviceLibrary()
        onboardingStep = 1
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasVideoPermission = granted
        if (granted) viewModel.scanDeviceVideos()
        if (onboardingStep == 1) {
            onboardingStep = 2
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        onboardingStep = 3
    }

    val deleteSenderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingDeleteTracks.isNotEmpty()) {
            viewModel.deleteTracksLegacyOrRefresh(pendingDeleteTracks)
        }
        pendingDeleteTracks = emptyList()
    }

    val deleteVideoSenderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val target = pendingDeleteVideo
        if (result.resultCode == Activity.RESULT_OK && target != null) {
            viewModel.deleteVideoLegacyOrRefresh(target)
        }
        pendingDeleteVideo = null
    }

    val openAudioFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.openAndPlayExternalUri(it) } }

    val exportM3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val pl = pendingM3uPlaylist
        if (uri != null && pl != null) {
            viewModel.exportPlaylistM3u(uri, pl)
        }
        pendingM3uPlaylist = null
    }

    val importLrcLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = pendingLrcTrack
        if (uri != null && target != null) {
            viewModel.importLrcFile(target, uri)
        }
        pendingLrcTrack = null
    }

    val shareTracks: (List<Track>) -> Unit = { tracks ->
        val intent = viewModel.buildShareIntent(tracks)
        if (intent != null) {
            runCatching {
                context.startActivity(Intent.createChooser(intent, "Share song(s)"))
            }.onFailure {
                viewModel.showMessage("Unable to share selected song(s)")
            }
        } else {
            viewModel.showMessage("Only downloaded/device audio files can be shared as files")
        }
    }

    val deleteTracks: (List<Track>) -> Unit = { tracks ->
        val localOnly = tracks.filter { it.isLocal }
        if (localOnly.isEmpty()) {
            viewModel.showMessage("Select device songs to delete")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = viewModel.createDeleteRequestIntent(localOnly)
            if (pi != null) {
                pendingDeleteTracks = localOnly
                deleteSenderLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            } else {
                viewModel.showMessage("Could not prepare delete request")
            }
        } else {
            viewModel.deleteTracksLegacyOrRefresh(localOnly)
        }
    }

    val deleteVideo: (VideoItem) -> Unit = { video ->
        if (video.isOnline) {
            viewModel.deleteVideoLegacyOrRefresh(video)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = viewModel.createDeleteVideoRequestIntent(video)
            if (pi != null) {
                pendingDeleteVideo = video
                deleteVideoSenderLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            } else {
                viewModel.deleteVideoLegacyOrRefresh(video)
            }
        } else {
            viewModel.deleteVideoLegacyOrRefresh(video)
        }
    }

    val performRingLightAction: (RingLightAction) -> Unit = { action ->
        when (action) {
            RingLightAction.SONG_POPUP -> showRingLightSongPopup = !showRingLightSongPopup
            RingLightAction.PLAY_PAUSE -> viewModel.togglePlayPause()
            RingLightAction.NEXT_SONG -> viewModel.skipNext()
            RingLightAction.PREVIOUS_SONG -> viewModel.skipPrevious()
            RingLightAction.VOLUME_UP -> RingLightHardwareHelper.adjustMusicVolume(context, AudioManager.ADJUST_RAISE)
            RingLightAction.VOLUME_DOWN -> RingLightHardwareHelper.adjustMusicVolume(context, AudioManager.ADJUST_LOWER)
            RingLightAction.MUTE_TOGGLE -> RingLightHardwareHelper.toggleMute(context)
            RingLightAction.FLASHLIGHT_TORCH -> {
                if (!RingLightHardwareHelper.toggleCameraTorch(context)) {
                    viewModel.showMessage("Camera flashlight unavailable")
                }
            }
            RingLightAction.OPEN_PLAYER -> {
                showRingLightSongPopup = false
                if (playback.currentTrack != null) {
                    showNowPlaying = true
                }
            }
            RingLightAction.NONE -> Unit
        }
    }

    val openOverlaySettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowAudio = checkAudioPermission(context)
                val nowVideo = checkVideoPermission(context)
                if (nowAudio) {
                    viewModel.scanDeviceLibrary()
                }
                if (nowVideo) {
                    viewModel.scanDeviceVideos()
                }
                hasAudioPermission = nowAudio
                hasVideoPermission = nowVideo
                if (awaitingOverlayReturn) {
                    awaitingOverlayReturn = false
                    showOverlayPrompt = false
                    onboardingStep = 4
                    viewModel.markOnboardingPermissionsDone()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Small settle delay so DataStore emits persisted value before launching first-time sequential flow
    LaunchedEffect(Unit) {
        delay(120)
        prefsReady = true
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.scanDeviceLibrary()
        }
    }

    LaunchedEffect(hasVideoPermission) {
        if (hasVideoPermission) {
            viewModel.scanDeviceVideos()
        }
    }

    // First-install sequential permission flow: step 0 (audio) -> 1 (video) -> 2 (notifications) -> 3 (overlay) -> done
    LaunchedEffect(prefsReady, prefs.onboardingPermissionsDone, onboardingStep) {
        if (!prefsReady || prefs.onboardingPermissionsDone) return@LaunchedEffect

        when (onboardingStep) {
            0 -> {
                if (!checkAudioPermission(context)) {
                    audioPermissionLauncher.launch(audioPermissionName)
                } else {
                    viewModel.scanDeviceLibrary()
                    onboardingStep = 1
                }
            }
            1 -> {
                if (!checkVideoPermission(context)) {
                    videoPermissionLauncher.launch(videoPermissionName)
                } else {
                    viewModel.scanDeviceVideos()
                    onboardingStep = 2
                }
            }
            2 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notifGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!notifGranted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onboardingStep = 3
                    }
                } else {
                    onboardingStep = 3
                }
            }
            3 -> {
                if (!checkOverlayPermission(context)) {
                    showOverlayPrompt = true
                } else {
                    onboardingStep = 4
                    viewModel.markOnboardingPermissionsDone()
                }
            }
        }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ShujalMusicTheme(
        themeId = prefs.themeId,
        appearanceMode = prefs.appearanceMode,
        customAccentArgb = prefs.customAccentArgb
    ) {
        val navController = rememberNavController()
        val navEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navEntry?.destination?.route.orEmpty()
        val isOnHomeTab = currentRoute.isEmpty() || currentRoute.contains("HomeRoute")
        val favTrackIds = remember(trackStats) { trackStats.filter { it.isFavorite }.map { it.trackId }.toSet() }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            LiquidAuroraBackground()

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.navigationBars,
                bottomBar = {
                    if (!useRail) {
                        Column {
                            val activeTrack = playback.currentTrack
                            if (activeTrack != null && !isOnHomeTab) {
                                MiniPlayerBar(
                                    track = activeTrack,
                                    isPlaying = playback.isPlaying,
                                    positionMs = playback.positionMs,
                                    durationMs = playback.durationMs,
                                    onPlayPause = viewModel::togglePlayPause,
                                    onNext = viewModel::skipNext,
                                    onPrevious = viewModel::skipPrevious,
                                    onOpenNowPlaying = { showNowPlaying = true },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            ) {
                                NAV_TABS.forEach { tab ->
                                    val selected = currentRoute.contains(tab.routeName) ||
                                        (currentRoute.isEmpty() && tab.route == HomeRoute)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            val navigate = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            if (!selected && tab.route != HomeRoute) {
                                                AdMobManager.showInterstitialIfReady(context as? Activity) {
                                                    navigate()
                                                }
                                            } else {
                                                navigate()
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag("nav_${tab.label.lowercase().replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            NAV_TABS.forEach { tab ->
                                val selected = currentRoute.contains(tab.routeName) ||
                                    (currentRoute.isEmpty() && tab.route == HomeRoute)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val activeTrack = playback.currentTrack
                            AnimatedVisibility(
                                visible = activeTrack != null && prefs.dynamicIslandInAppEnabled && !isOnHomeTab,
                                enter = slideInVertically { -it } + fadeIn(),
                                exit = slideOutVertically { -it } + fadeOut()
                            ) {
                                if (activeTrack != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DynamicIslandCapsule(
                                            track = activeTrack,
                                            isPlaying = playback.isPlaying,
                                            positionMs = playback.positionMs,
                                            durationMs = playback.durationMs,
                                            visualizerLevels = playback.visualizerLevels,
                                            style = prefs.dynamicIslandStyle,
                                            alwaysExtended = prefs.dynamicIslandAlwaysExtended,
                                            isFloatingOverlay = false,
                                            onPlayPause = viewModel::togglePlayPause,
                                            onNext = viewModel::skipNext,
                                            onPrevious = viewModel::skipPrevious,
                                            onOpenPlayer = { showNowPlaying = true }
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(max = 840.dp)
                                    .fillMaxWidth()
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = HomeRoute
                                ) {
                                    composable<HomeRoute> {
                                        val activeId = playback.currentTrack?.id
                                        HomePlayerScreen(
                                            snapshot = playback,
                                            isFavorite = activeId != null && activeId in favTrackIds,
                                            lyricsState = lyricsState,
                                            gesturesEnabled = prefs.gesturesEnabled,
                                            onPlayPause = viewModel::togglePlayPause,
                                            onNext = viewModel::skipNext,
                                            onPrevious = viewModel::skipPrevious,
                                            onSeekTo = viewModel::seekTo,
                                            onCycleRepeat = viewModel::cycleRepeatMode,
                                            onToggleShuffle = viewModel::toggleShuffle,
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onShareTrack = { shareTracks(listOf(it)) },
                                            onSetSleepTimer = viewModel::setSleepTimer,
                                            onPlayQueueIndex = viewModel::playFromQueue,
                                            onRemoveQueueIndex = viewModel::removeFromQueue,
                                            onLoadLyrics = viewModel::loadLyricsForTrack,
                                            onPickLrcFile = { tr ->
                                                pendingLrcTrack = tr
                                                importLrcLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                                            },
                                            onSaveManualLyrics = viewModel::saveManualLyrics,
                                            onOpenLibrary = {
                                                navController.navigate(LibraryRoute) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onOpenSafFile = { openAudioFileLauncher.launch(arrayOf("audio/*")) }
                                        )
                                    }

                                    composable<LibraryRoute> {
                                        LibraryScreen(
                                            isScanning = isScanning,
                                            localTracks = localTracks,
                                            albums = albums,
                                            artists = artists,
                                            folders = folders,
                                            genres = genres,
                                            playlists = playlists,
                                            smartPlaylists = smartPlaylists,
                                            playlistRefs = playlistRefs,
                                            trackStats = trackStats,
                                            currentTrackId = playback.currentTrack?.id,
                                            onRescan = viewModel::scanDeviceLibrary,
                                            onOpenSafFile = { openAudioFileLauncher.launch(arrayOf("audio/*")) },
                                            onPlayTracks = viewModel::playTracks,
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onOpenTrackOptions = { optionsTrack = it },
                                            onCreatePlaylist = { viewModel.createPlaylist(it) },
                                            onDeletePlaylist = viewModel::deletePlaylist,
                                            onExportPlaylistM3u = { pl ->
                                                pendingM3uPlaylist = pl
                                                exportM3uLauncher.launch("${pl.name}.m3u8")
                                            },
                                            onExcludeFolder = viewModel::excludeFolder,
                                            onShareSelectedTracks = shareTracks,
                                            onDeleteSelectedTracks = deleteTracks,
                                            onlineContent = {
                                                ExploreOnlineScreen(
                                                    onlineSearchState = onlineSearchState,
                                                    trendingState = trendingState,
                                                    onlineFavorites = onlineFavorites,
                                                    onlinePlayHistory = onlinePlayHistory,
                                                    onlinePlaylists = viewModel.onlinePlaylists,
                                                    searchHistory = searchHistory,
                                                    currentTrackId = playback.currentTrack?.id,
                                                    onInitLoad = {
                                                        viewModel.loadTrendingIfNeeded()
                                                    },
                                                    onSearchOnline = viewModel::searchOnline,
                                                    onClearSearchHistory = viewModel::clearSearchHistory,
                                                    onPlayTracks = viewModel::playTracks,
                                                    onToggleFavorite = viewModel::toggleFavorite,
                                                    onOpenTrackOptions = { optionsTrack = it }
                                                )
                                            }
                                        )
                                    }

                                    composable<VideosRoute> {
                                        LaunchedEffect(hasVideoPermission) {
                                            if (!hasVideoPermission && !requestedUpgradeVideoPerm && prefs.onboardingPermissionsDone) {
                                                requestedUpgradeVideoPerm = true
                                                videoPermissionLauncher.launch(videoPermissionName)
                                            }
                                        }
                                        VideosScreen(
                                            isScanningVideos = isScanningVideos,
                                            hasVideoPermission = hasVideoPermission,
                                            offlineVideos = offlineVideos,
                                            favoriteVideoIds = favoriteVideoIds,
                                            favoriteVideos = favoriteVideos,
                                            videoWatchHistory = videoWatchHistory,
                                            progressList = videoProgressList,
                                            onlineSearchState = onlineVideoSearchState,
                                            onlineTrendingState = onlineVideoTrendingState,
                                            onInitOnlineLoad = {
                                                viewModel.loadTrendingVideosIfNeeded()
                                            },
                                            onRequestVideoPermission = {
                                                videoPermissionLauncher.launch(videoPermissionName)
                                            },
                                            onRescanVideos = viewModel::scanDeviceVideos,
                                            onSearchOnlineVideos = viewModel::searchOnlineVideos,
                                            onPlayDirectUrl = viewModel::playDirectVideoUrl,
                                            onPlayVideos = viewModel::playVideos,
                                            onToggleFavoriteVideo = viewModel::toggleVideoFavorite,
                                            onShareVideo = { vid ->
                                                val shareIntent = viewModel.buildVideoShareIntent(vid)
                                                runCatching {
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                                }.onFailure {
                                                    viewModel.showMessage("Unable to share video")
                                                }
                                            },
                                            onDeleteVideo = deleteVideo,
                                            onClearVideoHistory = viewModel::clearVideoHistory
                                        )
                                    }

                                    composable<SettingsRoute> {
                                        SettingsScreen(
                                            prefs = prefs,
                                            sleepTimerRemainingMs = playback.sleepTimerRemainingMs,
                                            excludedFolders = excludedFolders,
                                            duplicates = viewModel.detectDuplicates(),
                                            onSelectTheme = viewModel::selectTheme,
                                            onSetAppearance = viewModel::setAppearanceMode,
                                            onSetCustomAccent = viewModel::setCustomAccent,
                                            onSetDynamicIslandOverlay = { enabled ->
                                                viewModel.setDynamicIslandOverlay(enabled)
                                                if (enabled && !checkOverlayPermission(context)) {
                                                    openOverlaySettings()
                                                }
                                            },
                                            onSetDynamicIslandInApp = viewModel::setDynamicIslandInApp,
                                            onSelectDynamicIslandStyle = viewModel::setDynamicIslandStyle,
                                            onSetDynamicIslandAlwaysExtended = viewModel::setDynamicIslandAlwaysExtended,
                                            onSetRingLightEnabled = viewModel::setRingLightEnabled,
                                            onStartRingLightCursorEdit = {
                                                if (!prefs.ringLightEnabled) {
                                                    viewModel.setRingLightEnabled(true)
                                                }
                                                ringLightCursorEditing = true
                                            },
                                            onResetRingLightPosition = viewModel::resetRingLightPosition,
                                            onUpdateRingLightGeometry = viewModel::updateRingLightGeometry,
                                            onSelectRingLightTheme = viewModel::setRingLightTheme,
                                            onUpdateRingLightActions = viewModel::updateRingLightActions,
                                            onPreviewFloatingIsland = {
                                                if (!checkOverlayPermission(context)) {
                                                    openOverlaySettings()
                                                } else {
                                                    viewModel.previewFloatingOverlay()
                                                }
                                            },
                                            onSetGestures = viewModel::setGesturesEnabled,
                                            onSetLockScreenControls = viewModel::setLockScreenControls,
                                            onSetSleepTimer = viewModel::setSleepTimer,
                                            onUpdateAudioSettings = { bal, spd, pch, gap, cf, norm, fade ->
                                                viewModel.updateAudioEngine(
                                                    balance = bal,
                                                    speed = spd,
                                                    pitch = pch,
                                                    gapless = gap,
                                                    crossfadeSec = cf,
                                                    normalization = norm,
                                                    fadePlayPause = fade
                                                )
                                            },
                                            onIncludeFolder = viewModel::includeFolder,
                                            onShowMessage = viewModel::showMessage
                                        )
                                    }
                                }
                            }

                            if (useRail && playback.currentTrack != null && !isOnHomeTab) {
                                MiniPlayerBar(
                                    track = playback.currentTrack!!,
                                    isPlaying = playback.isPlaying,
                                    positionMs = playback.positionMs,
                                    durationMs = playback.durationMs,
                                    onPlayPause = viewModel::togglePlayPause,
                                    onNext = viewModel::skipNext,
                                    onPrevious = viewModel::skipPrevious,
                                    onOpenNowPlaying = { showNowPlaying = true },
                                    modifier = Modifier
                                        .widthIn(max = 640.dp)
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Full-Screen Now Playing Sheet
            AnimatedVisibility(
                visible = showNowPlaying && playback.currentTrack != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                BackHandler { showNowPlaying = false }
                val activeId = playback.currentTrack?.id
                NowPlayingScreen(
                    snapshot = playback,
                    isFavorite = activeId != null && activeId in favTrackIds,
                    lyricsState = lyricsState,
                    gesturesEnabled = prefs.gesturesEnabled,
                    onClose = { showNowPlaying = false },
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::skipNext,
                    onPrevious = viewModel::skipPrevious,
                    onSeekTo = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSetSleepTimer = viewModel::setSleepTimer,
                    onUpdateSpeedPitch = { spd, pch ->
                        viewModel.updateAudioEngine(speed = spd, pitch = pch)
                    },
                    onPlayQueueIndex = viewModel::playFromQueue,
                    onRemoveQueueIndex = viewModel::removeFromQueue,
                    onLoadLyrics = viewModel::loadLyricsForTrack,
                    onPickLrcFile = { tr ->
                        pendingLrcTrack = tr
                        importLrcLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                    },
                    onSaveManualLyrics = viewModel::saveManualLyrics
                )
            }

            // Full-Screen Built-In Offline & Online Video Player
            AnimatedVisibility(
                visible = activeVideoIndex >= 0 && activeVideoQueue.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                BackHandler { viewModel.closeVideoPlayer() }
                VideoPlayerScreen(
                    queue = activeVideoQueue,
                    currentIndex = activeVideoIndex.coerceIn(0, (activeVideoQueue.size - 1).coerceAtLeast(0)),
                    favoriteVideoIds = favoriteVideoIds,
                    progressList = videoProgressList,
                    onSwitchVideo = viewModel::switchVideoQueueIndex,
                    onClose = viewModel::closeVideoPlayer,
                    onSaveProgress = viewModel::saveVideoProgress,
                    onToggleFavorite = viewModel::toggleVideoFavorite,
                    onShareVideo = { vid ->
                        val shareIntent = viewModel.buildVideoShareIntent(vid)
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        }.onFailure {
                            viewModel.showMessage("Unable to share video")
                        }
                    },
                    onShowMessage = viewModel::showMessage,
                    onResolveYouTubeStream = viewModel::resolveYouTubeStream
                )
            }

            // Permanent Camera Ring Light & Interactive Gesture Layer
            InAppCameraRingLightLayer(
                visible = (prefs.ringLightEnabled || ringLightCursorEditing) && activeVideoIndex < 0,
                isPositionEditing = ringLightCursorEditing,
                xFraction = prefs.ringLightXFraction,
                yDp = prefs.ringLightYDp,
                sizeDp = prefs.ringLightSizeDp,
                thicknessDp = prefs.ringLightThicknessDp,
                theme = prefs.ringLightTheme,
                currentTrack = playback.currentTrack,
                isPlaying = playback.isPlaying,
                visualizerLevel = playback.visualizerLevels.firstOrNull() ?: 0.5f,
                showSongPopup = showRingLightSongPopup,
                onTap = { performRingLightAction(prefs.ringLightTapAction) },
                onSwipeLeft = { performRingLightAction(prefs.ringLightSwipeLeftAction) },
                onSwipeRight = { performRingLightAction(prefs.ringLightSwipeRightAction) },
                onSwipeUp = { performRingLightAction(prefs.ringLightSwipeUpAction) },
                onSwipeDown = { performRingLightAction(prefs.ringLightSwipeDownAction) },
                onPositionChanged = { newX, newY ->
                    viewModel.updateRingLightGeometry(xFraction = newX, yDp = newY)
                },
                onDoneEditing = {
                    ringLightCursorEditing = false
                    viewModel.showMessage("Camera Ring Light position locked")
                },
                onPlayPause = viewModel::togglePlayPause,
                onPrevious = viewModel::skipPrevious,
                onNext = viewModel::skipNext,
                onOpenPlayer = {
                    showRingLightSongPopup = false
                    if (playback.currentTrack != null) showNowPlaying = true
                },
                onDismissPopup = { showRingLightSongPopup = false }
            )

            // Step 3 of First-Launch Onboarding: Floating Player Setup
            if (showOverlayPrompt) {
                AlertDialog(
                    onDismissRequest = {
                        showOverlayPrompt = false
                        onboardingStep = 4
                        viewModel.markOnboardingPermissionsDone()
                    },
                    title = { Text("Floating Mini Player") },
                    text = {
                        Text("Allow Shujal Music to display the Dynamic Island player & Camera Ring Light at the top of your screen when listening in the background.")
                    },
                    confirmButton = {
                        Button(onClick = {
                            awaitingOverlayReturn = true
                            openOverlaySettings()
                        }) {
                            Text("Allow")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showOverlayPrompt = false
                            onboardingStep = 4
                            viewModel.markOnboardingPermissionsDone()
                        }) {
                            Text("Later")
                        }
                    }
                )
            }

            // Track Action Sheet & Tag Editor
            optionsTrack?.let { selected ->
                TrackOptionsSheet(
                    track = selected,
                    isFavorite = selected.id in favTrackIds,
                    playlists = playlists,
                    onDismiss = { optionsTrack = null },
                    onPlayNext = { viewModel.addToQueueNext(selected) },
                    onToggleFavorite = { viewModel.toggleFavorite(selected) },
                    onShare = { shareTracks(listOf(selected)) },
                    onAddToPlaylist = { pid -> viewModel.addTrackToPlaylist(pid, selected) },
                    onCreatePlaylist = { name -> viewModel.createPlaylist(name, selected) },
                    onEditTags = { editingTagsTrack = selected },
                    onSetRingtone = {
                        if (!viewModel.canWriteSystemSettings() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                            viewModel.showMessage("Allow ringtone changes in system screen")
                        } else {
                            viewModel.setAsRingtone(selected)
                        }
                    }
                )
            }

            editingTagsTrack?.let { track ->
                EditTrackTagsDialog(
                    track = track,
                    onDismiss = { editingTagsTrack = null },
                    onSave = { title, artist, album, genre ->
                        viewModel.updateTrackTags(track, title, artist, album, genre)
                    }
                )
            }
        }
    }
}

private fun checkAudioPermission(context: Context): Boolean {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
}

private fun checkVideoPermission(context: Context): Boolean {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
}

private fun checkOverlayPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
}
