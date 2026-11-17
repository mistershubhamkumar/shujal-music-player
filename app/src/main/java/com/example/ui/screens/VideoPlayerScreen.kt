package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.local.VideoPlaybackProgressEntity
import com.example.model.VideoItem
import com.example.model.VideoResizeOption
import com.example.ui.components.GlassCircleIconButton
import com.example.ui.components.GlassmorphicSurface
import com.example.ui.components.formatTimeMs
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private enum class ActiveGestureHud {
    NONE, BRIGHTNESS, VOLUME, SEEK, DOUBLE_TAP_REWIND, DOUBLE_TAP_FORWARD
}

@androidx.annotation.OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    queue: List<VideoItem>,
    currentIndex: Int,
    favoriteVideoIds: Set<String>,
    progressList: List<VideoPlaybackProgressEntity>,
    onSwitchVideo: (Int) -> Unit,
    onClose: () -> Unit,
    onSaveProgress: (VideoItem, Long, Long) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onShareVideo: (VideoItem) -> Unit,
    onShowMessage: (String) -> Unit,
    onResolveYouTubeStream: suspend (String) -> String? = { null }
) {
    val video = queue.getOrNull(currentIndex) ?: return
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    var resizeOption by remember { mutableStateOf(VideoResizeOption.FIT) }
    var speed by remember { mutableFloatStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }
    var loopSingle by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }

    var sleepMinutesLeft by remember { mutableIntStateOf(0) }
    var sleepAfterVideo by remember { mutableStateOf(false) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }

    // Gesture overlay state
    var gestureHud by remember { mutableStateOf(ActiveGestureHud.NONE) }
    var brightnessFraction by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.55f
        )
    }
    var volumeFraction by remember {
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
        mutableFloatStateOf(cur.toFloat() / max.toFloat())
    }
    var gestureSeekTargetMs by remember { mutableLongStateOf(0L) }
    var gestureSeekDeltaMs by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember(context) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(25_000)
            .setUserAgent("ShujalMusicPlayer/1.0 (Android)")
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    var useYouTubeEmbed by remember(video.id) { mutableStateOf(false) }
    var ytWebView by remember { mutableStateOf<WebView?>(null) }
    var ytStartSec by remember(video.id) { mutableIntStateOf(0) }

    // Load selected video & resume from saved progress if available
    val savedProgressForCurrent = remember(video.id, progressList) {
        progressList.firstOrNull { it.videoId == video.id }
    }

    LaunchedEffect(video.id) {
        playbackError = null
        useYouTubeEmbed = false
        isBuffering = true
        val startMs = savedProgressForCurrent?.let { prog ->
            val dur = prog.durationMs.coerceAtLeast(video.durationMs)
            if (prog.positionMs > 4000L && (dur <= 0L || prog.positionMs < dur - 5000L)) {
                prog.positionMs
            } else 0L
        } ?: 0L
        ytStartSec = (startMs / 1000L).toInt()

        val targetUri = if (!video.youtubeId.isNullOrBlank()) {
            onResolveYouTubeStream(video.youtubeId)
        } else {
            video.uri
        }

        if (targetUri.isNullOrBlank() && !video.youtubeId.isNullOrBlank()) {
            exoPlayer.stop()
            useYouTubeEmbed = true
            isBuffering = false
            isPlaying = true
            controlsVisible = true
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(targetUri ?: video.uri), startMs)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            if (startMs > 0L) {
                onShowMessage("Resumed from ${formatTimeMs(startMs)}")
            }
        }
    }

    LaunchedEffect(speed, useYouTubeEmbed) {
        exoPlayer.playbackParameters = PlaybackParameters(speed, 1f)
        if (useYouTubeEmbed) {
            ytWebView?.evaluateJavascript("window.shujalSetSpeed && window.shujalSetSpeed($speed);", null)
        }
    }

    LaunchedEffect(isMuted, useYouTubeEmbed) {
        exoPlayer.volume = if (isMuted) 0f else 1f
        if (useYouTubeEmbed) {
            ytWebView?.evaluateJavascript("window.shujalSetMuted && window.shujalSetMuted($isMuted);", null)
        }
    }

    LaunchedEffect(loopSingle, useYouTubeEmbed) {
        exoPlayer.repeatMode = if (loopSingle) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        if (useYouTubeEmbed) {
            ytWebView?.evaluateJavascript("window.shujalSetLoop && window.shujalSetLoop($loopSingle);", null)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                val dur = exoPlayer.duration
                if (dur > 0L) durationMs = dur
                if (state == Player.STATE_ENDED) {
                    onSaveProgress(video, 0L, durationMs)
                    if (sleepAfterVideo) {
                        sleepAfterVideo = false
                        exoPlayer.pause()
                    } else if (!loopSingle && currentIndex < queue.lastIndex) {
                        onSwitchVideo(currentIndex + 1)
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                if (!video.youtubeId.isNullOrBlank()) {
                    useYouTubeEmbed = true
                    playbackError = null
                } else {
                    playbackError = error.localizedMessage ?: "Unable to play this video format or stream."
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            onSaveProgress(video, positionMs, durationMs)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            runCatching { ytWebView?.destroy() }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Periodic progress polling + database progress saving
    LaunchedEffect(video.id, exoPlayer, useYouTubeEmbed) {
        var counter = 0
        while (true) {
            if (!useYouTubeEmbed) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(video.durationMs).coerceAtLeast(1L)
                positionMs = pos
                durationMs = dur
                if (isPlaying && counter % 6 == 0) {
                    onSaveProgress(video, pos, dur)
                }
            } else {
                ytWebView?.evaluateJavascript("window.shujalPollState && window.shujalPollState();", null)
                if (isPlaying && counter % 6 == 0) {
                    onSaveProgress(video, positionMs, durationMs)
                }
            }
            counter++
            delay(500L)
        }
    }

    // Video Sleep Timer countdown
    LaunchedEffect(sleepMinutesLeft) {
        if (sleepMinutesLeft > 0) {
            delay(60_000L)
            val next = sleepMinutesLeft - 1
            sleepMinutesLeft = next
            if (next == 0) {
                exoPlayer.pause()
                if (useYouTubeEmbed) {
                    ytWebView?.evaluateJavascript("window.shujalPause && window.shujalPause();", null)
                    isPlaying = false
                }
                onShowMessage("Video sleep timer ended playback")
            }
        }
    }

    // Auto-hide HUD after 4 seconds while playing
    LaunchedEffect(controlsVisible, isPlaying, isLocked, useYouTubeEmbed) {
        if (controlsVisible && isPlaying && !isLocked && !useYouTubeEmbed) {
            delay(4200L)
            controlsVisible = false
        }
    }

    // Auto-dismiss transient gesture indicator
    LaunchedEffect(gestureHud) {
        if (gestureHud == ActiveGestureHud.DOUBLE_TAP_REWIND || gestureHud == ActiveGestureHud.DOUBLE_TAP_FORWARD) {
            delay(750L)
            gestureHud = ActiveGestureHud.NONE
        }
    }

    val isInPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        activity?.isInPictureInPictureMode == true
    } else false

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        if (useYouTubeEmbed && !video.youtubeId.isNullOrBlank()) {
            // Built-In Error-153-Proof YouTube IFrame Player with JS Controls Bridge
            val ytId = video.youtubeId
            @SuppressLint("SetJavaScriptEnabled")
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onState(playing: Boolean, posSec: Double, durSec: Double) {
                                    post {
                                        isPlaying = playing
                                        positionMs = (posSec * 1000.0).toLong().coerceAtLeast(0L)
                                        if (durSec > 1.0) durationMs = (durSec * 1000.0).toLong()
                                    }
                                }

                                @JavascriptInterface
                                fun onEnded() {
                                    post {
                                        if (sleepAfterVideo) {
                                            sleepAfterVideo = false
                                            isPlaying = false
                                        } else if (loopSingle) {
                                            evaluateJavascript("window.shujalSeekTo && window.shujalSeekTo(0); window.shujalPlay && window.shujalPlay();", null)
                                        } else if (currentIndex < queue.lastIndex) {
                                            onSwitchVideo(currentIndex + 1)
                                        }
                                    }
                                }
                            },
                            "AndroidYtBridge"
                        )
                        tag = ytId
                        loadDataWithBaseURL(
                            "https://com.shujal.musicplayer/",
                            buildYouTubePlayerHtml(ytId, ytStartSec),
                            "text/html",
                            "utf-8",
                            null
                        )
                        ytWebView = this
                    }
                },
                update = { wv ->
                    ytWebView = wv
                    if (wv.tag != ytId) {
                        wv.tag = ytId
                        wv.loadDataWithBaseURL(
                            "https://com.shujal.musicplayer/",
                            buildYouTubePlayerHtml(ytId, ytStartSec),
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Media3 Hardware Video Surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        keepScreenOn = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = exoPlayer
                    }
                },
                update = { pv ->
                    pv.player = exoPlayer
                    pv.resizeMode = when (resizeOption) {
                        VideoResizeOption.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoResizeOption.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        VideoResizeOption.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Interactive Gesture Layer (Full surface for ExoPlayer; side brightness/volume edges + lock support for YouTube Embed)
        if (!isInPip && !useYouTubeEmbed) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked, video.id) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                if (isLocked) return@detectTapGestures
                                val isLeft = offset.x < size.width / 2f
                                val cur = exoPlayer.currentPosition
                                if (isLeft) {
                                    exoPlayer.seekTo((cur - 10_000L).coerceAtLeast(0L))
                                    gestureHud = ActiveGestureHud.DOUBLE_TAP_REWIND
                                } else {
                                    val target = (cur + 10_000L).coerceAtMost(durationMs)
                                    exoPlayer.seekTo(target)
                                    gestureHud = ActiveGestureHud.DOUBLE_TAP_FORWARD
                                }
                            }
                        )
                    }
                    .then(
                        if (!isLocked) {
                            Modifier
                                .pointerInput(video.id) {
                                    var activeSideLeft = true
                                    detectVerticalDragGestures(
                                        onDragStart = { startOffset ->
                                            activeSideLeft = startOffset.x < size.width / 2f
                                        },
                                        onDragEnd = { gestureHud = ActiveGestureHud.NONE },
                                        onDragCancel = { gestureHud = ActiveGestureHud.NONE }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val delta = -dragAmount / size.height.toFloat().coerceAtLeast(1f) * 1.4f
                                        if (activeSideLeft) {
                                            brightnessFraction = (brightnessFraction + delta).coerceIn(0.04f, 1f)
                                            activity?.window?.let { win ->
                                                val attrs = win.attributes
                                                attrs.screenBrightness = brightnessFraction
                                                win.attributes = attrs
                                            }
                                            gestureHud = ActiveGestureHud.BRIGHTNESS
                                        } else {
                                            volumeFraction = (volumeFraction + delta).coerceIn(0f, 1f)
                                            audioManager?.let { am ->
                                                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                                am.setStreamVolume(
                                                    AudioManager.STREAM_MUSIC,
                                                    (volumeFraction * max).roundToInt().coerceIn(0, max),
                                                    0
                                                )
                                            }
                                            gestureHud = ActiveGestureHud.VOLUME
                                        }
                                    }
                                }
                                .pointerInput(video.id, durationMs) {
                                    var basePos = 0L
                                    var accumX = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            basePos = exoPlayer.currentPosition
                                            accumX = 0f
                                            gestureSeekTargetMs = basePos
                                            gestureSeekDeltaMs = 0L
                                            gestureHud = ActiveGestureHud.SEEK
                                        },
                                        onDragEnd = {
                                            exoPlayer.seekTo(gestureSeekTargetMs)
                                            gestureHud = ActiveGestureHud.NONE
                                        },
                                        onDragCancel = { gestureHud = ActiveGestureHud.NONE }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        accumX += dragAmount
                                        val deltaMs = ((accumX / size.width.toFloat().coerceAtLeast(1f)) * 90_000f).toLong()
                                        gestureSeekDeltaMs = deltaMs
                                        gestureSeekTargetMs = (basePos + deltaMs).coerceIn(0L, durationMs)
                                    }
                                }
                        } else Modifier
                    )
            ) {}
        } else if (!isInPip && useYouTubeEmbed) {
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                        }
                )
            } else {
                // Edge gesture strips on left (brightness) & right (volume) without blocking center WebView video taps
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight()
                            .pointerInput(video.id) {
                                detectVerticalDragGestures(
                                    onDragEnd = { gestureHud = ActiveGestureHud.NONE },
                                    onDragCancel = { gestureHud = ActiveGestureHud.NONE }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount / size.height.toFloat().coerceAtLeast(1f) * 1.4f
                                    brightnessFraction = (brightnessFraction + delta).coerceIn(0.04f, 1f)
                                    activity?.window?.let { win ->
                                        val attrs = win.attributes
                                        attrs.screenBrightness = brightnessFraction
                                        win.attributes = attrs
                                    }
                                    gestureHud = ActiveGestureHud.BRIGHTNESS
                                }
                            }
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight()
                            .pointerInput(video.id) {
                                detectVerticalDragGestures(
                                    onDragEnd = { gestureHud = ActiveGestureHud.NONE },
                                    onDragCancel = { gestureHud = ActiveGestureHud.NONE }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount / size.height.toFloat().coerceAtLeast(1f) * 1.4f
                                    volumeFraction = (volumeFraction + delta).coerceIn(0f, 1f)
                                    audioManager?.let { am ->
                                        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                        am.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            (volumeFraction * max).roundToInt().coerceIn(0, max),
                                            0
                                        )
                                    }
                                    gestureHud = ActiveGestureHud.VOLUME
                                }
                            }
                    )
                }
            }
        }

        // Buffering / Error Center Status
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        playbackError?.let { errMsg ->
            GlassmorphicSurface(
                cornerRadius = 24.dp,
                contentPadding = 18.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Text("Playback Issue", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(errMsg, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        playbackError = null
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }) { Text("Retry") }
                    TextButton(onClick = onClose) { Text("Close") }
                }
            }
        }

        // Gesture Live HUD Overlay (Brightness / Volume / Seek / Double-Tap)
        if (gestureHud != ActiveGestureHud.NONE && !isInPip) {
            GlassmorphicSurface(
                cornerRadius = 22.dp,
                contentPadding = 14.dp,
                tintAlpha = 0.45f,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    when (gestureHud) {
                        ActiveGestureHud.BRIGHTNESS -> {
                            Icon(Icons.Rounded.Brightness6, null, tint = Color.White)
                            Text("Brightness ${(brightnessFraction * 100).roundToInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        ActiveGestureHud.VOLUME -> {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White)
                            Text("Volume ${(volumeFraction * 100).roundToInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        ActiveGestureHud.SEEK -> {
                            val sign = if (gestureSeekDeltaMs >= 0) "+" else "-"
                            val absSec = kotlin.math.abs(gestureSeekDeltaMs) / 1000L
                            Text(
                                "${formatTimeMs(gestureSeekTargetMs)} ($sign${absSec}s)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        ActiveGestureHud.DOUBLE_TAP_REWIND -> {
                            Icon(Icons.Rounded.Replay10, null, tint = Color.White)
                            Text("-10 seconds", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        ActiveGestureHud.DOUBLE_TAP_FORWARD -> {
                            Icon(Icons.Rounded.Forward10, null, tint = Color.White)
                            Text("+10 seconds", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        ActiveGestureHud.NONE -> Unit
                    }
                }
            }
        }

        // Locked Screen Unlock Button
        if (isLocked && controlsVisible && !isInPip) {
            GlassCircleIconButton(
                onClick = { isLocked = false },
                isHighlighted = true,
                sizeDp = 54.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
                    .testTag("video_unlock_button")
            ) {
                Icon(Icons.Rounded.LockOpen, contentDescription = "Unlock screen", tint = Color.White)
            }
        }

        // Frosted Glass Video Player HUD Controls
        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !isInPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top Glass Bar
                GlassmorphicSurface(
                    cornerRadius = 24.dp,
                    contentPadding = 10.dp,
                    tintAlpha = 0.34f,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    onClose()
                                },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("video_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${video.subtitle} • ${video.resolutionLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primary,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                IconButton(
                                    onClick = {
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        runCatching { activity.enterPictureInPictureMode(params) }
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("video_pip_button")
                                ) {
                                    Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Picture in Picture", tint = Color.White)
                                }
                            }
                            IconButton(
                                onClick = { showTracksSheet = true },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("video_tracks_button")
                            ) {
                                Icon(Icons.Rounded.ClosedCaption, contentDescription = "Audio & Subtitles", tint = Color.White)
                            }
                            IconButton(
                                onClick = { onToggleFavorite(video) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (video.id in favoriteVideoIds) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Favourite video",
                                    tint = if (video.id in favoriteVideoIds) primary else Color.White
                                )
                            }
                            IconButton(
                                onClick = { onShareVideo(video) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.IosShare, contentDescription = "Share video", tint = Color.White)
                            }
                            IconButton(
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Info, contentDescription = "Video info", tint = Color.White)
                            }
                        }
                    }
                }

                // Center Glass Transport Controls (shown for ExoPlayer; for YouTube embed user taps directly on embed or bottom pills)
                if (!useYouTubeEmbed) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassCircleIconButton(
                            onClick = {
                                if (currentIndex > 0) onSwitchVideo(currentIndex - 1)
                                else exoPlayer.seekTo(0L)
                            },
                            sizeDp = 48.dp
                        ) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous video", tint = Color.White)
                        }

                        GlassCircleIconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                            sizeDp = 50.dp,
                            modifier = Modifier.testTag("video_rewind_10")
                        ) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(primary, secondary)))
                                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                .clickable {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                                .testTag("video_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF040710),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        GlassCircleIconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs)) },
                            sizeDp = 50.dp,
                            modifier = Modifier.testTag("video_forward_10")
                        ) {
                            Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                        }

                        GlassCircleIconButton(
                            onClick = {
                                if (currentIndex < queue.lastIndex) onSwitchVideo(currentIndex + 1)
                            },
                            sizeDp = 48.dp
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next video", tint = Color.White)
                        }
                    }
                }

                // Bottom Glass Seekbar + All Video Feature Chips
                GlassmorphicSurface(
                    cornerRadius = 26.dp,
                    contentPadding = 14.dp,
                    tintAlpha = 0.36f,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                ) {
                    if (!useYouTubeEmbed) {
                        val progressFraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(formatTimeMs(positionMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
                            Text(
                                text = if (showRemainingTime) "-${formatTimeMs((durationMs - positionMs).coerceAtLeast(0L))}"
                                else formatTimeMs(durationMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
                            )
                        }
                        Spacer(Modifier.height(6.dp))

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                                .pointerInput(video.id, durationMs) {
                                    detectTapGestures { offset ->
                                        if (size.width > 0) {
                                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            exoPlayer.seekTo((frac * durationMs).toLong())
                                        }
                                    }
                                }
                                .pointerInput(video.id, durationMs) {
                                    detectHorizontalDragGestures { change, _ ->
                                        change.consume()
                                        if (size.width > 0) {
                                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            exoPlayer.seekTo((frac * durationMs).toLong())
                                        }
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Brush.horizontalGradient(listOf(primary, secondary)))
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassCircleIconButton(
                                onClick = { if (currentIndex > 0) onSwitchVideo(currentIndex - 1) },
                                sizeDp = 38.dp
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "YouTube India Player • Swipe edges for Brightness & Volume",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            GlassCircleIconButton(
                                onClick = { if (currentIndex < queue.lastIndex) onSwitchVideo(currentIndex + 1) },
                                sizeDp = 38.dp
                            ) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        item {
                            VideoFeaturePill(
                                icon = Icons.Rounded.Lock,
                                label = "Lock",
                                onClick = { isLocked = true }
                            )
                        }
                        item {
                            VideoFeaturePill(
                                icon = Icons.Rounded.ScreenRotation,
                                label = if (isLandscape) "Portrait" else "Landscape",
                                active = isLandscape,
                                onClick = {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                }
                            )
                        }
                        item {
                            VideoFeaturePill(
                                icon = Icons.Rounded.AspectRatio,
                                label = resizeOption.label,
                                onClick = {
                                    val entries = VideoResizeOption.entries
                                    resizeOption = entries[(entries.indexOf(resizeOption) + 1) % entries.size]
                                }
                            )
                        }
                        item {
                            VideoFeaturePill(
                                icon = Icons.Rounded.Speed,
                                label = "${speed}x",
                                active = speed != 1f,
                                onClick = { showSpeedDialog = true }
                            )
                        }
                        item {
                            VideoFeaturePill(
                                icon = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                label = if (isMuted) "Muted" else "Audio",
                                active = isMuted,
                                onClick = { isMuted = !isMuted }
                            )
                        }
                        item {
                            VideoFeaturePill(
                                icon = if (loopSingle) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                label = if (loopSingle) "Loop On" else "Loop Off",
                                active = loopSingle,
                                onClick = { loopSingle = !loopSingle }
                            )
                        }
                        item {
                            VideoFeaturePill(
                                icon = Icons.Rounded.Bedtime,
                                label = when {
                                    sleepAfterVideo -> "End"
                                    sleepMinutesLeft > 0 -> "${sleepMinutesLeft}m"
                                    else -> "Sleep"
                                },
                                active = sleepAfterVideo || sleepMinutesLeft > 0,
                                onClick = { showSleepDialog = true }
                            )
                        }
                        if (queue.size > 1) {
                            item {
                                VideoFeaturePill(
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    label = "Queue (${queue.size})",
                                    onClick = { showQueueSheet = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(speeds) { s ->
                        FilterChip(
                            selected = speed == s,
                            onClick = {
                                speed = s
                                showSpeedDialog = false
                            },
                            label = { Text("${s}x") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Video Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { min ->
                        Button(
                            onClick = {
                                sleepAfterVideo = false
                                sleepMinutesLeft = min
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Stop in $min minutes") }
                    }
                    Button(
                        onClick = {
                            sleepMinutesLeft = 0
                            sleepAfterVideo = true
                            showSleepDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Stop at end of video") }
                    TextButton(
                        onClick = {
                            sleepMinutesLeft = 0
                            sleepAfterVideo = false
                            showSleepDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Turn Off") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) { Text("Close") }
            }
        )
    }

    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            Text("Up Next Videos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                itemsIndexed(queue) { idx, item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                item.title,
                                color = if (idx == currentIndex) primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = { Text("${item.subtitle} • ${formatTimeMs(item.durationMs)}") },
                        modifier = Modifier.clickable {
                            onSwitchVideo(idx)
                            showQueueSheet = false
                        }
                    )
                }
            }
        }
    }

    if (showTracksSheet) {
        ModalBottomSheet(onDismissRequest = { showTracksSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Audio & Subtitle Tracks", style = MaterialTheme.typography.titleLarge)

                val audioGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                val textGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

                Text("Audio Tracks", style = MaterialTheme.typography.titleSmall, color = primary)
                if (audioGroups.isEmpty()) {
                    Text("Default Audio Stream", style = MaterialTheme.typography.bodyMedium)
                } else {
                    audioGroups.forEachIndexed { gIdx, group ->
                        for (tIdx in 0 until group.length) {
                            val format = group.getTrackFormat(tIdx)
                            val label = format.label ?: format.language ?: "Audio Track ${gIdx + 1}"
                            val selected = group.isTrackSelected(tIdx)
                            ListItem(
                                headlineContent = { Text(label) },
                                supportingContent = { Text(if (selected) "Active" else "Tap to select") },
                                modifier = Modifier.clickable {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, tIdx))
                                        .build()
                                    showTracksSheet = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Subtitles / Captions", style = MaterialTheme.typography.titleSmall, color = primary)
                if (textGroups.isEmpty()) {
                    Text("No embedded subtitle tracks in this video", style = MaterialTheme.typography.bodyMedium)
                } else {
                    ListItem(
                        headlineContent = { Text("Subtitles Off") },
                        modifier = Modifier.clickable {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                            showTracksSheet = false
                        }
                    )
                    textGroups.forEachIndexed { gIdx, group ->
                        for (tIdx in 0 until group.length) {
                            val format = group.getTrackFormat(tIdx)
                            val label = format.label ?: format.language ?: "Subtitle ${gIdx + 1}"
                            val selected = group.isTrackSelected(tIdx)
                            ListItem(
                                headlineContent = { Text(label) },
                                supportingContent = { Text(if (selected) "Active" else "Tap to enable") },
                                modifier = Modifier.clickable {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, tIdx))
                                        .build()
                                    showTracksSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Video Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Title: ${video.title}")
                    Text("Source: ${if (video.isOnline) "Online Stream" else video.subtitle}")
                    Text("Resolution: ${video.resolutionLabel}")
                    Text("Duration: ${formatTimeMs(durationMs)}")
                    if (video.formattedSize.isNotBlank()) Text("Size: ${video.formattedSize}")
                    Text("Format: ${video.mimeType}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun VideoFeaturePill(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) primary.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.14f))
            .border(
                width = 1.dp,
                color = if (active) primary.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.28f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (active) primary else Color.White, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
    }
}

private fun buildYouTubePlayerHtml(videoId: String, startSec: Int): String = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
      <style>
        html, body, #player { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
      </style>
    </head>
    <body>
      <div id="player"></div>
      <script src="https://www.youtube.com/iframe_api"></script>
      <script>
        var ytPlayer = null;
        var ytLoop = false;
        function onYouTubeIframeAPIReady() {
          ytPlayer = new YT.Player('player', {
            videoId: '$videoId',
            playerVars: {
              autoplay: 1,
              playsinline: 1,
              controls: 1,
              rel: 0,
              modestbranding: 1,
              start: $startSec
            },
            events: {
              onReady: function(e) {
                e.target.playVideo();
              },
              onStateChange: function(e) {
                if (e.data === YT.PlayerState.ENDED) {
                  if (ytLoop) { ytPlayer.seekTo(0, true); ytPlayer.playVideo(); }
                  else { window.AndroidYtBridge && window.AndroidYtBridge.onEnded(); }
                }
                window.shujalPollState();
              }
            }
          });
        }
        window.shujalPollState = function() {
          if (!ytPlayer || !ytPlayer.getPlayerState) return;
          var playing = ytPlayer.getPlayerState() === YT.PlayerState.PLAYING;
          var cur = ytPlayer.getCurrentTime ? ytPlayer.getCurrentTime() : 0;
          var dur = ytPlayer.getDuration ? ytPlayer.getDuration() : 0;
          window.AndroidYtBridge && window.AndroidYtBridge.onState(playing, cur, dur);
        };
        window.shujalPlay = function() { ytPlayer && ytPlayer.playVideo && ytPlayer.playVideo(); };
        window.shujalPause = function() { ytPlayer && ytPlayer.pauseVideo && ytPlayer.pauseVideo(); };
        window.shujalSeekTo = function(s) { ytPlayer && ytPlayer.seekTo && ytPlayer.seekTo(s, true); };
        window.shujalSetSpeed = function(r) { ytPlayer && ytPlayer.setPlaybackRate && ytPlayer.setPlaybackRate(r); };
        window.shujalSetMuted = function(m) {
          if (!ytPlayer) return;
          if (m) ytPlayer.mute && ytPlayer.mute(); else ytPlayer.unMute && ytPlayer.unMute();
        };
        window.shujalSetLoop = function(l) { ytLoop = !!l; };
      </script>
    </body>
    </html>
""".trimIndent()
