package com.example.overlay

import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.model.RingLightAction
import com.example.player.MusicPlaybackService
import com.example.player.PlaybackSnapshot
import com.example.player.PlaybackStateHolder
import com.example.ui.components.CameraRingLightGraphic
import com.example.ui.components.DynamicIslandCapsule
import com.example.ui.components.RingLightHardwareHelper
import com.example.ui.components.RingLightSongPopup
import com.example.ui.theme.ShujalMusicTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DynamicIslandOverlayService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null

    private var islandView: ComposeView? = null
    private var islandParams: WindowManager.LayoutParams? = null

    private var ringView: ComposeView? = null
    private var ringParams: WindowManager.LayoutParams? = null

    private var ringPopupView: ComposeView? = null
    private var ringPopupParams: WindowManager.LayoutParams? = null

    private var showRingSongPopup by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        lifecycleScope.launch {
            PlaybackStateHolder.state
                .distinctUntilChanged { old, new ->
                    old.isAppInForeground == new.isAppInForeground &&
                        old.overlayEnabled == new.overlayEnabled &&
                        old.currentTrack?.id == new.currentTrack?.id &&
                        old.overlayDismissedForTrackId == new.overlayDismissedForTrackId &&
                        old.ringLightEnabled == new.ringLightEnabled &&
                        old.ringLightXFraction == new.ringLightXFraction &&
                        old.ringLightYDp == new.ringLightYDp &&
                        old.ringLightSizeDp == new.ringLightSizeDp
                }
                .collect { snap ->
                    reconcileWindows(snap)
                }
        }
    }

    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun reconcileWindows(snap: PlaybackSnapshot) {
        val shouldShowIsland = !snap.isAppInForeground &&
            snap.overlayEnabled &&
            snap.currentTrack != null &&
            snap.overlayDismissedForTrackId != snap.currentTrack.id

        val shouldShowRing = !snap.isAppInForeground && snap.ringLightEnabled

        if (shouldShowIsland) {
            ensureIslandWindow()
        } else {
            removeIslandWindow()
        }

        if (shouldShowRing) {
            ensureOrUpdateRingWindow(snap)
        } else {
            showRingSongPopup = false
            removeRingPopupWindow()
            removeRingWindow()
        }

        if (!shouldShowIsland && !shouldShowRing) {
            stopSelf()
        }
    }

    private fun ensureIslandWindow() {
        if (islandView != null) return
        val statusBarMarginPx = (14 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = statusBarMarginPx
        }
        islandParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                ShujalMusicTheme(darkTheme = true) {
                    val snapshot by PlaybackStateHolder.state.collectAsStateWithLifecycle()
                    val track = snapshot.currentTrack
                    if (track != null) {
                        DynamicIslandCapsule(
                            track = track,
                            isPlaying = snapshot.isPlaying,
                            positionMs = snapshot.positionMs,
                            durationMs = snapshot.durationMs,
                            visualizerLevels = snapshot.visualizerLevels,
                            style = snapshot.islandStyle,
                            alwaysExtended = snapshot.islandAlwaysExtended,
                            isFloatingOverlay = true,
                            onPlayPause = { sendPlaybackAction(MusicPlaybackService.ACTION_TOGGLE_PLAY_PAUSE) },
                            onNext = { sendPlaybackAction(MusicPlaybackService.ACTION_SKIP_NEXT) },
                            onPrevious = { sendPlaybackAction(MusicPlaybackService.ACTION_SKIP_PREVIOUS) },
                            onOpenPlayer = { openMainPlayer() },
                            onDismiss = {
                                PlaybackStateHolder.dismissOverlayForCurrentTrack()
                                removeIslandWindow()
                            },
                            onDragDelta = { dx, dy ->
                                islandParams?.let { lp ->
                                    lp.x += dx.toInt()
                                    lp.y = (lp.y + dy.toInt()).coerceAtLeast(0)
                                    runCatching { windowManager?.updateViewLayout(this@apply, lp) }
                                }
                            }
                        )
                    }
                }
            }
        }
        islandView = view
        runCatching { windowManager?.addView(view, params) }
    }

    private fun ensureOrUpdateRingWindow(snap: PlaybackSnapshot) {
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels
        val touchSizeDp = (snap.ringLightSizeDp + 20f).coerceAtLeast(52f)
        val halfBoxPx = (touchSizeDp * density / 2f)
        val targetX = ((snap.ringLightXFraction * screenWidthPx) - halfBoxPx)
            .coerceIn(0f, (screenWidthPx - halfBoxPx * 2f).coerceAtLeast(0f))
            .roundToInt()
        val targetY = (snap.ringLightYDp * density).roundToInt()

        val existing = ringView
        val existingParams = ringParams
        if (existing != null && existingParams != null) {
            existingParams.x = targetX
            existingParams.y = targetY
            runCatching { windowManager?.updateViewLayout(existing, existingParams) }
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = targetX
            y = targetY
        }
        ringParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                ShujalMusicTheme(darkTheme = true) {
                    val snapshot by PlaybackStateHolder.state.collectAsStateWithLifecycle()
                    CameraRingLightGraphic(
                        theme = snapshot.ringLightTheme,
                        sizeDp = snapshot.ringLightSizeDp.dp,
                        thicknessDp = snapshot.ringLightThicknessDp.dp,
                        isPlaying = snapshot.isPlaying,
                        visualizerLevel = snapshot.visualizerLevels.firstOrNull() ?: 0.5f,
                        isPositionEditing = false,
                        onTap = { executeRingAction(snapshot.ringLightTapAction, snapshot) },
                        onSwipeLeft = { executeRingAction(snapshot.ringLightSwipeLeftAction, snapshot) },
                        onSwipeRight = { executeRingAction(snapshot.ringLightSwipeRightAction, snapshot) },
                        onSwipeUp = { executeRingAction(snapshot.ringLightSwipeUpAction, snapshot) },
                        onSwipeDown = { executeRingAction(snapshot.ringLightSwipeDownAction, snapshot) }
                    )
                }
            }
        }
        ringView = view
        runCatching { windowManager?.addView(view, params) }
    }

    private fun toggleSongPopupWindow(snap: PlaybackSnapshot) {
        if (showRingSongPopup) {
            showRingSongPopup = false
            removeRingPopupWindow()
            return
        }
        showRingSongPopup = true
        val density = resources.displayMetrics.density
        val topMarginPx = ((snap.ringLightYDp + snap.ringLightSizeDp + 18f).coerceAtLeast(64f) * density).toInt()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = topMarginPx
        }
        ringPopupParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                ShujalMusicTheme(darkTheme = true) {
                    val snapshot by PlaybackStateHolder.state.collectAsStateWithLifecycle()
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        RingLightSongPopup(
                            track = snapshot.currentTrack,
                            isPlaying = snapshot.isPlaying,
                            onPlayPause = { sendPlaybackAction(MusicPlaybackService.ACTION_TOGGLE_PLAY_PAUSE) },
                            onPrevious = { sendPlaybackAction(MusicPlaybackService.ACTION_SKIP_PREVIOUS) },
                            onNext = { sendPlaybackAction(MusicPlaybackService.ACTION_SKIP_NEXT) },
                            onOpenPlayer = {
                                showRingSongPopup = false
                                removeRingPopupWindow()
                                openMainPlayer()
                            },
                            onClose = {
                                showRingSongPopup = false
                                removeRingPopupWindow()
                            }
                        )
                    }
                }
            }
        }
        ringPopupView = view
        runCatching { windowManager?.addView(view, params) }
    }

    private fun executeRingAction(action: RingLightAction, snap: PlaybackSnapshot) {
        when (action) {
            RingLightAction.SONG_POPUP -> toggleSongPopupWindow(snap)
            RingLightAction.PLAY_PAUSE -> sendPlaybackAction(MusicPlaybackService.ACTION_TOGGLE_PLAY_PAUSE)
            RingLightAction.NEXT_SONG -> sendPlaybackAction(MusicPlaybackService.ACTION_SKIP_NEXT)
            RingLightAction.PREVIOUS_SONG -> sendPlaybackAction(MusicPlaybackService.ACTION_SKIP_PREVIOUS)
            RingLightAction.VOLUME_UP -> RingLightHardwareHelper.adjustMusicVolume(this, AudioManager.ADJUST_RAISE)
            RingLightAction.VOLUME_DOWN -> RingLightHardwareHelper.adjustMusicVolume(this, AudioManager.ADJUST_LOWER)
            RingLightAction.MUTE_TOGGLE -> RingLightHardwareHelper.toggleMute(this)
            RingLightAction.FLASHLIGHT_TORCH -> RingLightHardwareHelper.toggleCameraTorch(this)
            RingLightAction.OPEN_PLAYER -> openMainPlayer()
            RingLightAction.NONE -> Unit
        }
    }

    private fun sendPlaybackAction(action: String) {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        runCatching { ContextCompat.startForegroundService(this, intent) }
    }

    private fun openMainPlayer() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_now_playing", true)
        }
        startActivity(intent)
    }

    private fun removeIslandWindow() {
        islandView?.let { runCatching { windowManager?.removeView(it) } }
        islandView = null
        islandParams = null
    }

    private fun removeRingWindow() {
        ringView?.let { runCatching { windowManager?.removeView(it) } }
        ringView = null
        ringParams = null
    }

    private fun removeRingPopupWindow() {
        ringPopupView?.let { runCatching { windowManager?.removeView(it) } }
        ringPopupView = null
        ringPopupParams = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        reconcileWindows(PlaybackStateHolder.state.value)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val snap = PlaybackStateHolder.state.value
        if ((snap.currentTrack != null && snap.overlayEnabled) || snap.ringLightEnabled) {
            return
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        removeIslandWindow()
        removeRingPopupWindow()
        removeRingWindow()
        super.onDestroy()
    }
}
