package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.network.OnlineMusicRepository
import com.example.data.repository.MusicRepository
import com.example.data.scanner.LocalMusicScanner
import com.example.model.RepeatModeState
import com.example.model.Track
import com.example.overlay.DynamicIslandOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val audioProcessor = BalanceAndNormalizationAudioProcessor()
    private val effectsController = AudioEffectsController()
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var musicRepo: MusicRepository

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var fadeJob: Job? = null
    private var fadeEnabled: Boolean = true
    private var crossfadeSec: Int = 3
    private var sleepAfterCurrentTrack: Boolean = false
    private var lastRecordedTrackId: String? = null
    private var outroFadeTriggeredForIndex: Int = -1
    private var streamRetryCount: Int = 0
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        prefsRepo = PreferencesRepository(this)
        val dao = AppDatabase.getInstance(this).musicDao()
        musicRepo = MusicRepository(this, dao, LocalMusicScanner(this), OnlineMusicRepository())

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(audioProcessor))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setUserAgent("ShujalMusicPlayer/1.0 (Android)")
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,
                90_000,
                2_000,
                5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.pauseAtEndOfMediaItems = false

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.playback_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_music_notification)
        setMediaNotificationProvider(notificationProvider)

        attachPlayerListener()
        observePreferences()
        observeOverlayVisibility()
        startPositionTicker()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.playback_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Ongoing music playback controls"
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun promoteToForeground(track: Track? = PlaybackStateHolder.state.value.currentTrack) {
        ensureNotificationChannel()
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_notification)
            .setContentTitle(track?.title ?: getString(R.string.app_name))
            .setContentText(track?.artist ?: "Playing music")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
        }
    }

    private fun attachPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PlaybackStateHolder.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    streamRetryCount = 0
                    acquirePlaybackLocks()
                    promoteToForeground()
                    if (player.audioSessionId > 0) {
                        effectsController.attachToSession(player.audioSessionId)
                    }
                } else {
                    releasePlaybackLocks()
                }
                syncOverlayService()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                PlaybackStateHolder.update {
                    it.copy(
                        isBuffering = buffering,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                if (playbackState == Player.STATE_READY && player.audioSessionId > 0) {
                    effectsController.attachToSession(player.audioSessionId)
                }
                if (playbackState == Player.STATE_ENDED && !sleepAfterCurrentTrack) {
                    handleLocalQueueCompleted()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (sleepAfterCurrentTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    sleepAfterCurrentTrack = false
                    player.pause()
                    player.volume = 1f
                    PlaybackStateHolder.update { it.copy(sleepTimerRemainingMs = 0L) }
                }

                val index = player.currentMediaItemIndex
                outroFadeTriggeredForIndex = -1
                streamRetryCount = 0
                PlaybackStateHolder.update { snapshot ->
                    val track = snapshot.queue.getOrNull(index) ?: snapshot.currentTrack
                    snapshot.copy(
                        currentIndex = index,
                        currentTrack = track,
                        durationMs = player.duration.coerceAtLeast(track?.durationMs ?: 0L),
                        positionMs = 0L,
                        overlayDismissedForTrackId = null
                    )
                }
                val activeTrack = PlaybackStateHolder.state.value.currentTrack
                if (activeTrack != null && activeTrack.id != lastRecordedTrackId) {
                    lastRecordedTrackId = activeTrack.id
                    serviceScope.launch { musicRepo.recordTrackPlayed(activeTrack) }
                }
                if (crossfadeSec > 0 && player.isPlaying) {
                    val fadeMs = (crossfadeSec * 500L).coerceIn(400L, 4000L)
                    runVolumeFade(from = 0.12f, to = 1f, durationMs = fadeMs)
                } else {
                    player.volume = 1f
                }
                if (player.isPlaying) {
                    promoteToForeground(activeTrack)
                }
                syncOverlayService()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) {
                    effectsController.attachToSession(audioSessionId)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (streamRetryCount < 1 && player.mediaItemCount > 0) {
                    streamRetryCount++
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    player.seekTo(player.currentMediaItemIndex, pos)
                    player.prepare()
                    player.play()
                } else if (player.hasNextMediaItem()) {
                    streamRetryCount = 0
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                } else {
                    releasePlaybackLocks()
                    PlaybackStateHolder.update { it.copy(isPlaying = false, isBuffering = false) }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                PlaybackStateHolder.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
                serviceScope.launch { prefsRepo.setShuffleEnabled(shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mapped = repeatMode.toRepeatModeState()
                PlaybackStateHolder.update { it.copy(repeatMode = mapped) }
                serviceScope.launch { prefsRepo.setRepeatMode(mapped) }
            }
        })
    }

    private fun observePreferences() {
        serviceScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                audioProcessor.stereoBalance = prefs.stereoBalance
                audioProcessor.normalizationEnabled = prefs.volumeNormalization
                fadeEnabled = prefs.fadeOnPlayPause
                crossfadeSec = prefs.crossfadeSeconds
                player.pauseAtEndOfMediaItems = !prefs.gaplessEnabled
                player.playbackParameters = PlaybackParameters(
                    prefs.playbackSpeed.coerceIn(0.5f, 2f),
                    prefs.playbackPitch.coerceIn(0.5f, 2f)
                )
                if (player.shuffleModeEnabled != prefs.shuffleEnabled) {
                    player.shuffleModeEnabled = prefs.shuffleEnabled
                }
                val targetExoRepeat = prefs.repeatMode.toExoRepeatMode()
                if (player.repeatMode != targetExoRepeat) {
                    player.repeatMode = targetExoRepeat
                }
                PlaybackStateHolder.update {
                    it.copy(
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

    private fun observeOverlayVisibility() {
        serviceScope.launch {
            PlaybackStateHolder.state
                .distinctUntilChanged { old, new ->
                    old.isAppInForeground == new.isAppInForeground &&
                        old.currentTrack?.id == new.currentTrack?.id &&
                        old.overlayEnabled == new.overlayEnabled &&
                        old.ringLightEnabled == new.ringLightEnabled &&
                        old.overlayDismissedForTrackId == new.overlayDismissedForTrackId &&
                        old.isPlaying == new.isPlaying
                }
                .collect { syncOverlayService() }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun handleLocalQueueCompleted() {
        val snapshot = PlaybackStateHolder.state.value
        val finishedLocal = snapshot.currentTrack?.isLocal == true ||
            (snapshot.queue.isNotEmpty() && snapshot.queue.all { it.isLocal })
        if (!finishedLocal || !hasInternetConnection()) return

        serviceScope.launch {
            val onlineHistory = musicRepo.getOnlinePlayHistoryOnce()
            if (onlineHistory.isNotEmpty()) {
                PlaybackStateHolder.update {
                    it.copy(
                        queue = onlineHistory,
                        currentIndex = 0,
                        currentTrack = onlineHistory.first(),
                        overlayDismissedForTrackId = null
                    )
                }
                playCurrentHolderQueue(0)
            }
        }
    }

    private fun acquirePlaybackLocks() {
        runCatching {
            if (cpuWakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                cpuWakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShujalMusic::PlaybackWakeLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (cpuWakeLock?.isHeld == false) {
                cpuWakeLock?.acquire(12 * 60 * 60 * 1000L)
            }
        }
        runCatching {
            if (wifiLock == null) {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm?.createWifiLock(mode, "ShujalMusic::WifiLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        }
    }

    private fun releasePlaybackLocks() {
        runCatching { if (cpuWakeLock?.isHeld == true) cpuWakeLock?.release() }
        runCatching { if (wifiLock?.isHeld == true) wifiLock?.release() }
    }

    private fun syncOverlayService() {
        val snap = PlaybackStateHolder.state.value
        val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        val showIsland = !snap.isAppInForeground &&
            snap.overlayEnabled &&
            snap.currentTrack != null &&
            snap.overlayDismissedForTrackId != snap.currentTrack.id &&
            canDraw
        val showRingLight = !snap.isAppInForeground && snap.ringLightEnabled && canDraw

        val intent = Intent(this, DynamicIslandOverlayService::class.java)
        if (showIsland || showRingLight) {
            runCatching { startService(intent) }
        } else {
            runCatching { stopService(intent) }
        }
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = serviceScope.launch {
            var tick = 0
            while (true) {
                if (::player.isInitialized) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val idx = player.currentMediaItemIndex

                    // Smooth crossfade outro fade-out near end of track before transitioning to next
                    if (crossfadeSec > 0 && player.isPlaying && dur > crossfadeSec * 1500L && outroFadeTriggeredForIndex != idx) {
                        val remaining = dur - pos
                        val fadeWindowMs = (crossfadeSec * 500L).coerceIn(400L, 4000L)
                        if (remaining in 150L..fadeWindowMs && player.hasNextMediaItem()) {
                            outroFadeTriggeredForIndex = idx
                            runVolumeFade(from = player.volume, to = 0.15f, durationMs = remaining.coerceAtLeast(300L))
                        }
                    }

                    val fallbackBars = if (player.isPlaying) {
                        List(24) { barIdx ->
                            val wave = 0.48f + 0.42f * sin((tick * 0.45f) + barIdx * 0.55f)
                            wave.coerceIn(0.14f, 0.96f)
                        }
                    } else {
                        List(24) { 0.14f }
                    }
                    PlaybackStateHolder.update { current ->
                        current.copy(
                            positionMs = pos,
                            durationMs = if (dur > 0L) dur else (current.currentTrack?.durationMs ?: 0L),
                            sleepTimerRemainingMs = if (sleepAfterCurrentTrack && dur > pos) (dur - pos) else current.sleepTimerRemainingMs,
                            visualizerLevels = fallbackBars
                        )
                    }
                }
                tick++
                delay(250L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_QUEUE,
            ACTION_TOGGLE_PLAY_PAUSE,
            ACTION_SKIP_NEXT,
            ACTION_SKIP_PREVIOUS,
            ACTION_SEEK_TO -> promoteToForeground()
        }
        when (intent?.action) {
            ACTION_TOGGLE_PLAY_PAUSE -> togglePlayPause()
            ACTION_PAUSE -> {
                fadeJob?.cancel()
                if (player.isPlaying) {
                    player.pause()
                }
            }
            ACTION_SKIP_NEXT -> skipNext()
            ACTION_SKIP_PREVIOUS -> skipPrevious()
            ACTION_PLAY_QUEUE -> {
                val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
                playCurrentHolderQueue(startIndex)
            }
            ACTION_SEEK_TO -> {
                val pos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                player.seekTo(pos)
            }
            ACTION_SET_SLEEP_TIMER -> {
                val minutes = intent.getIntExtra(EXTRA_SLEEP_MINUTES, 0)
                startSleepTimer(minutes)
            }
            ACTION_TOGGLE_SHUFFLE -> toggleShuffle()
            ACTION_SET_SHUFFLE -> {
                val enabled = intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, false)
                applyShuffleEnabled(enabled)
            }
            ACTION_CYCLE_REPEAT -> cycleRepeatMode()
            ACTION_SET_REPEAT_MODE -> {
                val modeName = intent.getStringExtra(EXTRA_REPEAT_MODE)
                val mode = runCatching { RepeatModeState.valueOf(modeName.orEmpty()) }
                    .getOrDefault(RepeatModeState.OFF)
                applyRepeatMode(mode)
            }
            ACTION_SHOW_OVERLAY_PREVIEW -> {
                val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
                if (canDraw) {
                    runCatching { startService(Intent(this, DynamicIslandOverlayService::class.java)) }
                }
            }
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun playCurrentHolderQueue(startIndex: Int) {
        val queue = PlaybackStateHolder.state.value.queue
        if (queue.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, queue.lastIndex)
        outroFadeTriggeredForIndex = -1
        val mediaItems = queue.map { track -> track.toMediaItem() }
        player.setMediaItems(mediaItems, safeIdx, 0L)
        player.prepare()
        player.play()
        if (fadeEnabled) {
            runVolumeFade(from = 0.15f, to = 1f, durationMs = 500L)
        } else {
            player.volume = 1f
        }
        val activeTrack = queue[safeIdx]
        lastRecordedTrackId = activeTrack.id
        serviceScope.launch { musicRepo.recordTrackPlayed(activeTrack) }
        PlaybackStateHolder.update {
            it.copy(
                currentIndex = safeIdx,
                currentTrack = activeTrack,
                isPlaying = true,
                durationMs = activeTrack.durationMs,
                overlayDismissedForTrackId = null
            )
        }
    }

    private fun togglePlayPause() {
        if (player.isPlaying) {
            if (fadeEnabled) {
                fadeJob?.cancel()
                fadeJob = serviceScope.launch {
                    runVolumeFade(from = player.volume, to = 0.05f, durationMs = 260L)
                    player.pause()
                    player.volume = 1f
                }
            } else {
                player.pause()
            }
        } else {
            if (player.mediaItemCount == 0 && PlaybackStateHolder.state.value.queue.isNotEmpty()) {
                val idx = PlaybackStateHolder.state.value.currentIndex.coerceAtLeast(0)
                playCurrentHolderQueue(idx)
                return
            }
            player.play()
            if (fadeEnabled) {
                runVolumeFade(from = 0.1f, to = 1f, durationMs = 350L)
            } else {
                player.volume = 1f
            }
        }
    }

    private fun skipNext() {
        outroFadeTriggeredForIndex = -1
        if (player.mediaItemCount == 0) {
            val queue = PlaybackStateHolder.state.value.queue
            if (queue.isNotEmpty()) {
                val nextIdx = (PlaybackStateHolder.state.value.currentIndex + 1) % queue.size
                playCurrentHolderQueue(nextIdx)
            }
            return
        }
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        } else if (player.mediaItemCount > 0) {
            player.seekTo(0, 0L)
            player.play()
        }
    }

    private fun skipPrevious() {
        outroFadeTriggeredForIndex = -1
        if (player.mediaItemCount == 0) {
            val queue = PlaybackStateHolder.state.value.queue
            if (queue.isNotEmpty()) {
                val cur = PlaybackStateHolder.state.value.currentIndex
                val prevIdx = if (cur > 0) cur - 1 else queue.lastIndex
                playCurrentHolderQueue(prevIdx)
            }
            return
        }
        if (player.currentPosition > 4000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        } else {
            player.seekTo(0L)
        }
    }

    private fun toggleShuffle() {
        applyShuffleEnabled(!player.shuffleModeEnabled)
    }

    private fun applyShuffleEnabled(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
        PlaybackStateHolder.update { it.copy(shuffleEnabled = enabled) }
        serviceScope.launch { prefsRepo.setShuffleEnabled(enabled) }
    }

    private fun cycleRepeatMode() {
        val nextState = when (player.repeatMode.toRepeatModeState()) {
            RepeatModeState.OFF -> RepeatModeState.ALL
            RepeatModeState.ALL -> RepeatModeState.ONE
            RepeatModeState.ONE -> RepeatModeState.OFF
        }
        applyRepeatMode(nextState)
    }

    private fun applyRepeatMode(mode: RepeatModeState) {
        player.repeatMode = mode.toExoRepeatMode()
        PlaybackStateHolder.update { it.copy(repeatMode = mode) }
        serviceScope.launch { prefsRepo.setRepeatMode(mode) }
    }

    private fun RepeatModeState.toExoRepeatMode(): Int = when (this) {
        RepeatModeState.OFF -> Player.REPEAT_MODE_OFF
        RepeatModeState.ALL -> Player.REPEAT_MODE_ALL
        RepeatModeState.ONE -> Player.REPEAT_MODE_ONE
    }

    private fun Int.toRepeatModeState(): RepeatModeState = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatModeState.ALL
        Player.REPEAT_MODE_ONE -> RepeatModeState.ONE
        else -> RepeatModeState.OFF
    }

    private fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepAfterCurrentTrack = false
        if (minutes == -1) {
            sleepAfterCurrentTrack = true
            val rem = (player.duration - player.currentPosition).coerceAtLeast(1000L)
            PlaybackStateHolder.update { it.copy(sleepTimerRemainingMs = rem) }
            return
        }
        if (minutes <= 0) {
            PlaybackStateHolder.update { it.copy(sleepTimerRemainingMs = 0L) }
            return
        }
        sleepTimerJob = serviceScope.launch {
            var remaining = minutes * 60_000L
            while (remaining > 0L) {
                PlaybackStateHolder.update { it.copy(sleepTimerRemainingMs = remaining) }
                delay(1_000L)
                remaining -= 1_000L
            }
            PlaybackStateHolder.update { it.copy(sleepTimerRemainingMs = 0L) }
            if (player.isPlaying) {
                runVolumeFade(from = player.volume, to = 0f, durationMs = 1500L)
                player.pause()
                player.volume = 1f
            }
        }
    }

    private fun runVolumeFade(from: Float, to: Float, durationMs: Long) {
        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val steps = 14
            val stepDelay = (durationMs / steps).coerceAtLeast(16L)
            for (i in 0..steps) {
                val fraction = i / steps.toFloat()
                player.volume = (from + (to - from) * fraction).coerceIn(0f, 1f)
                delay(stepDelay)
            }
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let { Uri.parse(it) })
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val activePlayer = session.player
        val keepForeground = startInForegroundRequired ||
            (activePlayer.mediaItemCount > 0 &&
                activePlayer.playbackState != Player.STATE_ENDED &&
                (activePlayer.isPlaying || activePlayer.playWhenReady))
        super.onUpdateNotification(session, keepForeground)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        PlaybackStateHolder.setAppForeground(false)
        val activePlayer = mediaSession?.player
        val keepAlive = activePlayer != null &&
            activePlayer.mediaItemCount > 0 &&
            activePlayer.playbackState != Player.STATE_ENDED &&
            (activePlayer.isPlaying || activePlayer.playWhenReady || activePlayer.playbackState == Player.STATE_BUFFERING)
        if (keepAlive) {
            promoteToForeground()
            syncOverlayService()
            return
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        positionJob?.cancel()
        sleepTimerJob?.cancel()
        fadeJob?.cancel()
        releasePlaybackLocks()
        effectsController.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        runCatching { stopService(Intent(this, DynamicIslandOverlayService::class.java)) }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "shujal_music_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_SKIP_NEXT = "com.example.action.SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.example.action.SKIP_PREVIOUS"
        const val ACTION_PLAY_QUEUE = "com.example.action.PLAY_QUEUE"
        const val ACTION_SEEK_TO = "com.example.action.SEEK_TO"
        const val ACTION_SET_SLEEP_TIMER = "com.example.action.SET_SLEEP_TIMER"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.action.TOGGLE_SHUFFLE"
        const val ACTION_SET_SHUFFLE = "com.example.action.SET_SHUFFLE"
        const val ACTION_CYCLE_REPEAT = "com.example.action.CYCLE_REPEAT"
        const val ACTION_SET_REPEAT_MODE = "com.example.action.SET_REPEAT_MODE"
        const val ACTION_SHOW_OVERLAY_PREVIEW = "com.example.action.SHOW_OVERLAY_PREVIEW"
        const val EXTRA_START_INDEX = "extra_start_index"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"
        const val EXTRA_SLEEP_MINUTES = "extra_sleep_minutes"
        const val EXTRA_SHUFFLE_ENABLED = "extra_shuffle_enabled"
        const val EXTRA_REPEAT_MODE = "extra_repeat_mode"
    }
}
