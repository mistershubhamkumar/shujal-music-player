package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LyricsData
import com.example.model.RepeatModeState
import com.example.model.Track
import com.example.player.PlaybackSnapshot
import com.example.ui.AsyncLoadState
import com.example.ui.components.AlbumArt3D
import com.example.ui.components.FrostedArtworkBackdrop
import com.example.ui.components.GlassCircleIconButton
import com.example.ui.components.GlassmorphicSurface
import com.example.ui.components.LiquidSpectrumVisualizer
import com.example.ui.components.SyncedLyricsDisplay
import com.example.ui.components.formatTimeMs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    snapshot: PlaybackSnapshot,
    isFavorite: Boolean,
    lyricsState: AsyncLoadState<LyricsData>,
    gesturesEnabled: Boolean,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onUpdateSpeedPitch: (Float, Float) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onRemoveQueueIndex: (Int) -> Unit,
    onLoadLyrics: (Track) -> Unit,
    onPickLrcFile: (Track) -> Unit,
    onSaveManualLyrics: (Track, String) -> Unit
) {
    val track = snapshot.currentTrack ?: return
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var lyricsMode by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showManualLyricsEditor by remember { mutableStateOf(false) }

    LaunchedEffect(track.id, lyricsMode) {
        if (lyricsMode) {
            onLoadLyrics(track)
        }
    }

    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember(track.id) { mutableFloatStateOf(0f) }
    val duration = snapshot.durationMs.coerceAtLeast(1L)
    val currentFraction = if (scrubbing) scrubFraction else {
        (snapshot.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
    val displayPosMs = (currentFraction * duration).toLong()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (gesturesEnabled && !lyricsMode) {
                    Modifier.pointerInput(track.id) {
                        var dragSum = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragSum < -110f) onNext()
                                else if (dragSum > 110f) onPrevious()
                                dragSum = 0f
                            }
                        ) { _, dragAmount -> dragSum += dragAmount }
                    }
                } else Modifier
            )
    ) {
        // Blurred dynamic album art frosted backdrop
        FrostedArtworkBackdrop(
            artworkUri = track.artworkUri,
            isRadio = track.isRadio
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Frosted Glass Top Header Bar
            GlassmorphicSurface(
                cornerRadius = 26.dp,
                contentPadding = 10.dp,
                tintAlpha = 0.26f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCircleIconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("now_playing_close")
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimize player",
                            modifier = Modifier.size(28.dp),
                            tint = onSurface
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(primary.copy(alpha = 0.18f))
                                .border(1.dp, primary.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when {
                                    track.isOnlineFull -> "ONLINE FULL"
                                    track.isRadio -> "LIVE BROADCAST"
                                    else -> "NOW PLAYING"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurface.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    GlassCircleIconButton(
                        onClick = { lyricsMode = !lyricsMode },
                        isHighlighted = lyricsMode,
                        modifier = Modifier.testTag("toggle_lyrics_button")
                    ) {
                        Icon(
                            Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics mode",
                            tint = if (lyricsMode) primary else onSurface
                        )
                    }
                }
            }

            // Center Stage: 3D Album Art with Glass Aura OR Frosted Lyrics
            AnimatedContent(
                targetState = lyricsMode,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "now_playing_center"
            ) { showLyrics ->
                if (!showLyrics) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumArt3D(
                            artworkUri = track.artworkUri,
                            isRadio = track.isRadio,
                            isPlaying = snapshot.isPlaying,
                            title = track.title,
                            modifier = Modifier.fillMaxWidth(0.84f)
                        )
                    }
                } else {
                    GlassmorphicSurface(
                        cornerRadius = 28.dp,
                        contentPadding = 6.dp,
                        tintAlpha = 0.28f,
                        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)
                    ) {
                        SyncedLyricsDisplay(
                            track = track,
                            positionMs = snapshot.positionMs,
                            lyricsState = lyricsState,
                            onRetryFetch = { onLoadLyrics(track) },
                            onPickLrcFile = { onPickLrcFile(track) },
                            onOpenManualEditor = { showManualLyricsEditor = true },
                            onSeekToLine = onSeekTo,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Frosted Glass Player Control Deck
            GlassmorphicSurface(
                cornerRadius = 32.dp,
                contentPadding = 18.dp,
                tintAlpha = 0.30f
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LiquidSpectrumVisualizer(
                        levels = snapshot.visualizerLevels,
                        isPlaying = snapshot.isPlaying
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        GlassCircleIconButton(
                            onClick = { onToggleFavorite(track) },
                            isHighlighted = isFavorite,
                            modifier = Modifier.testTag("now_playing_favorite")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = if (isFavorite) primary else onSurface
                            )
                        }
                    }

                    // Frosted Pill Progress Track
                    if (!track.isRadio) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                                    .pointerInput(track.id, duration) {
                                        detectTapGestures { offset ->
                                            if (size.width > 0) {
                                                val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                onSeekTo((frac * duration).toLong())
                                            }
                                        }
                                    }
                                    .pointerInput(track.id, duration) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { scrubbing = true },
                                            onDragEnd = {
                                                onSeekTo((scrubFraction * duration).toLong())
                                                scrubbing = false
                                            },
                                            onDragCancel = { scrubbing = false }
                                        ) { change, _ ->
                                            change.consume()
                                            if (size.width > 0) {
                                                scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            }
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(currentFraction.coerceAtLeast(0.03f))
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Brush.horizontalGradient(listOf(primary, secondary)))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(formatTimeMs(displayPosMs), style = MaterialTheme.typography.labelMedium, color = muted)
                                Text(formatTimeMs(duration), style = MaterialTheme.typography.labelMedium, color = muted)
                            }
                        }
                    }

                    // Glassmorphic Primary Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassCircleIconButton(
                            onClick = onToggleShuffle,
                            isHighlighted = snapshot.shuffleEnabled,
                            modifier = Modifier.testTag("now_playing_shuffle")
                        ) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = if (snapshot.shuffleEnabled) "Shuffle On" else "Shuffle Off",
                                tint = if (snapshot.shuffleEnabled) primary else muted,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        GlassCircleIconButton(
                            onClick = onPrevious,
                            sizeDp = 54.dp,
                            modifier = Modifier.testTag("now_playing_prev")
                        ) {
                            Icon(
                                Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(30.dp),
                                tint = onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    elevation = 18.dp,
                                    shape = CircleShape,
                                    ambientColor = primary.copy(alpha = 0.65f),
                                    spotColor = secondary.copy(alpha = 0.65f)
                                )
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(primary, secondary)))
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.14f))
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(onClick = onPlayPause)
                                .testTag("now_playing_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (snapshot.isPlaying) "Pause" else "Play",
                                tint = Color(0xFF040710),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        GlassCircleIconButton(
                            onClick = onNext,
                            sizeDp = 54.dp,
                            modifier = Modifier.testTag("now_playing_next")
                        ) {
                            Icon(
                                Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(30.dp),
                                tint = onSurface
                            )
                        }

                        val repeatActive = snapshot.repeatMode != RepeatModeState.OFF
                        val repeatLabel = when (snapshot.repeatMode) {
                            RepeatModeState.OFF -> "Repeat Off"
                            RepeatModeState.ALL -> "Repeat All"
                            RepeatModeState.ONE -> "Repeat One"
                        }
                        GlassCircleIconButton(
                            onClick = onCycleRepeat,
                            isHighlighted = repeatActive,
                            modifier = Modifier.testTag("now_playing_repeat")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (snapshot.repeatMode == RepeatModeState.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    contentDescription = repeatLabel,
                                    tint = if (repeatActive) primary else muted,
                                    modifier = Modifier.size(21.dp)
                                )
                                if (snapshot.repeatMode == RepeatModeState.ALL) {
                                    Text("ALL", style = MaterialTheme.typography.labelSmall, color = primary)
                                }
                            }
                        }
                    }

                    // Bottom Glass Action Pills: Speed, Sleep Timer, Queue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassActionChip(
                            icon = Icons.Rounded.Speed,
                            label = "${"%.2f".format(snapshot.playbackSpeed)}x",
                            onClick = { showSpeedDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        GlassActionChip(
                            icon = Icons.Rounded.Bedtime,
                            label = if (snapshot.sleepTimerRemainingMs > 0) {
                                "${(snapshot.sleepTimerRemainingMs / 60_000L).toInt().coerceAtLeast(1)}m"
                            } else "Sleep",
                            isActive = snapshot.sleepTimerRemainingMs > 0,
                            onClick = { showSleepDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        GlassActionChip(
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            label = "Queue (${snapshot.queue.size})",
                            onClick = { showQueueSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ) {
            Text("Up Next Queue", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                itemsIndexed(snapshot.queue) { idx, item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                item.title,
                                color = if (idx == snapshot.currentIndex) primary else onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = { Text(item.artist, maxLines = 1) },
                        trailingContent = {
                            if (idx != snapshot.currentIndex) {
                                IconButton(onClick = { onRemoveQueueIndex(idx) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                                }
                            }
                        },
                        modifier = Modifier.clickable { onPlayQueueIndex(idx) }
                    )
                }
            }
        }
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Sleep & Auto Shutdown Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 30, 45, 60).forEach { min ->
                        Button(
                            onClick = { onSetSleepTimer(min); showSleepDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Stop playback in $min minutes") }
                    }
                    Button(
                        onClick = { onSetSleepTimer(-1); showSleepDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("End of current track") }
                    TextButton(
                        onClick = { onSetSleepTimer(0); showSleepDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Turn off timer") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSpeedDialog) {
        var speed by remember { mutableFloatStateOf(snapshot.playbackSpeed) }
        var pitch by remember { mutableFloatStateOf(snapshot.playbackPitch) }
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Speed & Pitch Control") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Speed: ${"%.2f".format(speed)}x")
                    Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2.0f)
                    Text("Pitch: ${"%.2f".format(pitch)}x")
                    Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2.0f)
                    TextButton(onClick = { speed = 1f; pitch = 1f }) { Text("Reset to 1.00x") }
                }
            },
            confirmButton = {
                Button(onClick = { onUpdateSpeedPitch(speed, pitch); showSpeedDialog = false }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showManualLyricsEditor) {
        val initialText = (lyricsState as? AsyncLoadState.Success)?.data?.plainLyrics.orEmpty()
        var text by remember(initialText) { mutableStateOf(initialText) }
        AlertDialog(
            onDismissRequest = { showManualLyricsEditor = false },
            title = { Text("Manual / LRC Lyrics Editor") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Paste plain or [mm:ss.xx] lyrics") },
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            },
            confirmButton = {
                Button(onClick = { onSaveManualLyrics(track, text); showManualLyricsEditor = false }) {
                    Text("Save Offline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualLyricsEditor = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GlassActionChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isActive) primary.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.12f)
            )
            .border(
                width = 1.dp,
                color = if (isActive) primary.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.24f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) primary else onSurface,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) primary else onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
