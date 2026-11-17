package com.example.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.RepeatModeState
import com.example.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class MusicPlayerController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    PlaybackStateHolder.update { snap ->
                        snap.copy(
                            isPlaying = controller.isPlaying,
                            positionMs = controller.currentPosition.coerceAtLeast(0L),
                            durationMs = controller.duration.coerceAtLeast(snap.durationMs)
                        )
                    }
                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            PlaybackStateHolder.update { it.copy(isPlaying = isPlaying) }
                        }
                    })
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        PlaybackStateHolder.update {
            it.copy(
                queue = tracks,
                currentIndex = safeIndex,
                currentTrack = tracks[safeIndex],
                overlayDismissedForTrackId = null
            )
        }
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PLAY_QUEUE
            putExtra(MusicPlaybackService.EXTRA_START_INDEX, safeIndex)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun addToQueueNext(track: Track) {
        PlaybackStateHolder.update { snap ->
            if (snap.queue.isEmpty()) {
                snap.copy(queue = listOf(track), currentIndex = 0, currentTrack = track)
            } else {
                val mutable = snap.queue.toMutableList()
                val insertAt = (snap.currentIndex + 1).coerceIn(0, mutable.size)
                mutable.add(insertAt, track)
                snap.copy(queue = mutable)
            }
        }
    }

    fun playFromQueueIndex(index: Int) {
        val queue = PlaybackStateHolder.state.value.queue
        if (index in queue.indices) {
            playQueue(queue, index)
        }
    }

    fun removeFromQueue(index: Int) {
        PlaybackStateHolder.update { snap ->
            if (index !in snap.queue.indices || index == snap.currentIndex) return@update snap
            val mutable = snap.queue.toMutableList()
            mutable.removeAt(index)
            val newIdx = if (index < snap.currentIndex) snap.currentIndex - 1 else snap.currentIndex
            snap.copy(queue = mutable, currentIndex = newIdx)
        }
    }

    fun togglePlayPause() = sendForegroundAction(MusicPlaybackService.ACTION_TOGGLE_PLAY_PAUSE)

    fun pauseIfPlaying() {
        if (PlaybackStateHolder.state.value.isPlaying) {
            sendAction(MusicPlaybackService.ACTION_PAUSE)
        }
    }

    fun skipNext() = sendForegroundAction(MusicPlaybackService.ACTION_SKIP_NEXT)

    fun skipPrevious() = sendForegroundAction(MusicPlaybackService.ACTION_SKIP_PREVIOUS)

    fun seekTo(positionMs: Long) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SEEK_TO
            putExtra(MusicPlaybackService.EXTRA_SEEK_POSITION, positionMs)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun setSleepTimer(minutes: Int) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SET_SLEEP_TIMER
            putExtra(MusicPlaybackService.EXTRA_SLEEP_MINUTES, minutes)
        }
        context.startService(intent)
    }

    fun toggleShuffle() = sendAction(MusicPlaybackService.ACTION_TOGGLE_SHUFFLE)

    fun setShuffleEnabled(enabled: Boolean) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SET_SHUFFLE
            putExtra(MusicPlaybackService.EXTRA_SHUFFLE_ENABLED, enabled)
        }
        context.startService(intent)
    }

    fun cycleRepeatMode() = sendAction(MusicPlaybackService.ACTION_CYCLE_REPEAT)

    fun setRepeatMode(mode: RepeatModeState) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SET_REPEAT_MODE
            putExtra(MusicPlaybackService.EXTRA_REPEAT_MODE, mode.name)
        }
        context.startService(intent)
    }

    fun previewFloatingOverlay() = sendAction(MusicPlaybackService.ACTION_SHOW_OVERLAY_PREVIEW)

    private fun sendForegroundAction(action: String) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun sendAction(action: String) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }
}
