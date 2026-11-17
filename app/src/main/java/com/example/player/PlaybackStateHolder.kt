package com.example.player

import com.example.model.DynamicIslandStyle
import com.example.model.RepeatModeState
import com.example.model.RingLightAction
import com.example.model.RingLightTheme
import com.example.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackSnapshot(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val sleepTimerRemainingMs: Long = 0L,
    val isAppInForeground: Boolean = true,
    val overlayEnabled: Boolean = true,
    val inAppIslandEnabled: Boolean = true,
    val islandStyle: DynamicIslandStyle = DynamicIslandStyle.CLASSIC_PILL,
    val islandAlwaysExtended: Boolean = false,
    val ringLightEnabled: Boolean = false,
    val ringLightXFraction: Float = 0.5f,
    val ringLightYDp: Float = 10f,
    val ringLightSizeDp: Float = 42f,
    val ringLightThicknessDp: Float = 3.5f,
    val ringLightTheme: RingLightTheme = RingLightTheme.RAINBOW_FLOW,
    val ringLightTapAction: RingLightAction = RingLightAction.SONG_POPUP,
    val ringLightSwipeLeftAction: RingLightAction = RingLightAction.PREVIOUS_SONG,
    val ringLightSwipeRightAction: RingLightAction = RingLightAction.NEXT_SONG,
    val ringLightSwipeUpAction: RingLightAction = RingLightAction.VOLUME_UP,
    val ringLightSwipeDownAction: RingLightAction = RingLightAction.VOLUME_DOWN,
    val overlayDismissedForTrackId: String? = null,
    val visualizerLevels: List<Float> = List(24) { 0.15f }
)

object PlaybackStateHolder {
    private val _state = MutableStateFlow(PlaybackSnapshot())
    val state: StateFlow<PlaybackSnapshot> = _state.asStateFlow()

    fun update(transform: (PlaybackSnapshot) -> PlaybackSnapshot) {
        _state.update(transform)
    }

    fun setAppForeground(inForeground: Boolean) {
        _state.update { it.copy(isAppInForeground = inForeground) }
    }

    fun dismissOverlayForCurrentTrack() {
        _state.update { it.copy(overlayDismissedForTrackId = it.currentTrack?.id) }
    }

    fun resetOverlayDismissal() {
        _state.update { it.copy(overlayDismissedForTrackId = null) }
    }
}
