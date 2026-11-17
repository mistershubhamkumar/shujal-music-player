package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LyricsData
import com.example.model.RepeatModeState
import com.example.model.Track
import com.example.player.PlaybackSnapshot
import com.example.ui.AsyncLoadState
import com.example.ui.components.FrostedArtworkBackdrop
import com.example.ui.components.GlassCircleIconButton
import com.example.ui.components.GlassmorphicSurface
import com.example.ui.components.SyncedLyricsDisplay
import com.example.ui.components.TrackArtworkImage
import com.example.ui.components.formatTimeMs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePlayerScreen(
    snapshot: PlaybackSnapshot,
    isFavorite: Boolean,
    lyricsState: AsyncLoadState<LyricsData>,
    gesturesEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onShareTrack: (Track) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onRemoveQueueIndex: (Int) -> Unit,
    onLoadLyrics: (Track) -> Unit,
    onPickLrcFile: (Track) -> Unit,
    onSaveManualLyrics: (Track, String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSafFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = snapshot.currentTrack
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var lyricsMode by remember { mutableStateOf(false) }
    var showManualLyricsEditor by remember { mutableStateOf(false) }

    LaunchedEffect(track?.id, lyricsMode) {
        if (track != null && lyricsMode) {
            onLoadLyrics(track)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_rotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (track != null) {
            FrostedArtworkBackdrop(
                artworkUri = track.artworkUri,
                isRadio = track.isRadio
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Frosted Glass Top Bar ("Play Music" + Music/Lyrics badge + Sleep + Queue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircleIconButton(
                onClick = { if (track != null) lyricsMode = !lyricsMode },
                sizeDp = 46.dp,
                isHighlighted = lyricsMode
            ) {
                Icon(
                    imageVector = if (lyricsMode) Icons.Rounded.Lyrics else Icons.Rounded.MusicNote,
                    contentDescription = "Lyrics mode",
                    tint = if (lyricsMode) primary else onSurface
                )
            }

            Text(
                text = "Play Music",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { showSleepDialog = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("home_sleep_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = "Sleep timer",
                        tint = if (snapshot.sleepTimerRemainingMs > 0L) primary else onSurface
                    )
                }
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("home_queue_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = onSurface
                    )
                }
            }
        }

        if (track == null) {
            // Clean empty state when user has no songs scanned yet
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .shadow(16.dp, CircleShape, ambientColor = primary.copy(alpha = 0.25f))
                        .clip(CircleShape)
                        .background(surface)
                        .border(8.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text("No Track Selected", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Choose a song from your Library or open any audio file to play inside the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onOpenLibrary, modifier = Modifier.testTag("home_open_library")) {
                        Icon(Icons.Rounded.LibraryMusic, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("My Songs")
                    }
                    OutlinedButton(onClick = onOpenSafFile, modifier = Modifier.testTag("home_open_file")) {
                        Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick File")
                    }
                }
            }
        } else {
            // Center Circular Sculpted Vinyl / 3D Album Art or Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = lyricsMode, label = "center_stage") { showLyrics ->
                    if (!showLyrics) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .shadow(
                                    elevation = 22.dp,
                                    shape = CircleShape,
                                    ambientColor = primary.copy(alpha = 0.3f),
                                    spotColor = Color.Black.copy(alpha = 0.45f)
                                )
                                .clip(CircleShape)
                                .background(surface)
                                .border(10.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .padding(14.dp)
                                .clip(CircleShape)
                                .graphicsLayer {
                                    rotationZ = if (snapshot.isPlaying) discRotation else 0f
                                    cameraDistance = 14f * density
                                }
                        ) {
                            TrackArtworkImage(
                                artworkUri = track.artworkUri,
                                isRadio = track.isRadio,
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Center vinyl spindle accent
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(surface.copy(alpha = 0.9f))
                                    .border(2.dp, primary.copy(alpha = 0.6f), CircleShape)
                            )
                        }
                    } else {
                        SyncedLyricsDisplay(
                            track = track,
                            positionMs = snapshot.positionMs,
                            lyricsState = lyricsState,
                            onRetryFetch = { onLoadLyrics(track) },
                            onPickLrcFile = { onPickLrcFile(track) },
                            onOpenManualEditor = { showManualLyricsEditor = true },
                            onSeekToLine = onSeekTo,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Frosted Glass Bottom Control Deck (Title, Progress Bar, Share/Fav, Transport Controls)
            GlassmorphicSurface(
                cornerRadius = 30.dp,
                contentPadding = 16.dp,
                tintAlpha = 0.28f,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (snapshot.sleepTimerRemainingMs > 0L) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sleep in ${formatTimeMs(snapshot.sleepTimerRemainingMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = primary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Frosted Pill Progress Bar + Timestamps
                val duration = snapshot.durationMs.coerceAtLeast(1L)
                var scrubbing by remember { mutableStateOf(false) }
                var scrubFraction by remember(track.id) { mutableFloatStateOf(0f) }
                val currentFraction = if (scrubbing) scrubFraction else {
                    (snapshot.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                }
                val displayMs = (currentFraction * duration).toLong()

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimeMs(displayMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = muted
                        )
                        Text(
                            text = if (track.isRadio) "LIVE" else formatTimeMs(duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = muted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                            .pointerInput(track.id, duration) {
                                detectTapGestures { offset ->
                                    if (!track.isRadio && size.width > 0) {
                                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        onSeekTo((frac * duration).toLong())
                                    }
                                }
                            }
                            .pointerInput(track.id, duration) {
                                detectHorizontalDragGestures(
                                    onDragStart = { scrubbing = true },
                                    onDragEnd = {
                                        if (!track.isRadio) {
                                            onSeekTo((scrubFraction * duration).toLong())
                                        }
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
                                .fillMaxWidth(if (track.isRadio) 1f else currentFraction.coerceAtLeast(0.03f))
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(primary, MaterialTheme.colorScheme.secondary)
                                    )
                                )
                        )
                    }
                }

                // Share & Favourite Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onShareTrack(track) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("home_share_button")
                    ) {
                        Icon(Icons.Rounded.IosShare, contentDescription = "Share song", tint = onSurface)
                    }
                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("home_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favourite",
                            tint = if (isFavorite) primary else onSurface
                        )
                    }
                }

                // Glassmorphic Circular Transport Controls (Repeat, Prev, Play/Pause, Next, Shuffle)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val repeatActive = snapshot.repeatMode != RepeatModeState.OFF
                    val repeatDesc = when (snapshot.repeatMode) {
                        RepeatModeState.OFF -> "Repeat Off"
                        RepeatModeState.ALL -> "Repeat All"
                        RepeatModeState.ONE -> "Repeat One"
                    }
                    GlassCircleIconButton(
                        onClick = onCycleRepeat,
                        isHighlighted = repeatActive,
                        modifier = Modifier.testTag("home_repeat_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (snapshot.repeatMode == RepeatModeState.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                contentDescription = repeatDesc,
                                tint = if (repeatActive) primary else muted,
                                modifier = Modifier.size(21.dp)
                            )
                            if (snapshot.repeatMode == RepeatModeState.ALL) {
                                Text("ALL", style = MaterialTheme.typography.labelSmall, color = primary)
                            }
                        }
                    }

                    SculptedCircleButton(
                        sizeDp = 54,
                        onClick = onPrevious,
                        testTag = "home_prev_button"
                    ) {
                        Icon(Icons.Rounded.FastRewind, contentDescription = "Previous", tint = onSurface, modifier = Modifier.size(25.dp))
                    }

                    SculptedCircleButton(
                        sizeDp = 70,
                        onClick = onPlayPause,
                        isAccent = true,
                        testTag = "home_play_pause_button"
                    ) {
                        Icon(
                            imageVector = if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (snapshot.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    SculptedCircleButton(
                        sizeDp = 54,
                        onClick = onNext,
                        testTag = "home_next_button"
                    ) {
                        Icon(Icons.Rounded.FastForward, contentDescription = "Next", tint = onSurface, modifier = Modifier.size(25.dp))
                    }

                    GlassCircleIconButton(
                        onClick = onToggleShuffle,
                        isHighlighted = snapshot.shuffleEnabled,
                        modifier = Modifier.testTag("home_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = if (snapshot.shuffleEnabled) "Shuffle On" else "Shuffle Off",
                            tint = if (snapshot.shuffleEnabled) primary else muted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
    }

    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            Text("Up Next Queue", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            if (snapshot.queue.isEmpty()) {
                Text(
                    "Queue is empty.",
                    modifier = Modifier.padding(20.dp),
                    color = muted
                )
            } else {
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
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 30, 45, 60).forEach { min ->
                        Button(
                            onClick = { onSetSleepTimer(min); showSleepDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("$min minutes") }
                    }
                    OutlinedButton(
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

    if (showManualLyricsEditor && track != null) {
        val initialText = (lyricsState as? AsyncLoadState.Success)?.data?.plainLyrics.orEmpty()
        var text by remember(initialText) { mutableStateOf(initialText) }
        AlertDialog(
            onDismissRequest = { showManualLyricsEditor = false },
            title = { Text("Edit Lyrics") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Plain or [mm:ss.xx] lyrics") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                Button(onClick = { onSaveManualLyrics(track, text); showManualLyricsEditor = false }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualLyricsEditor = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SculptedCircleButton(
    sizeDp: Int,
    onClick: () -> Unit,
    isAccent: Boolean = false,
    testTag: String,
    content: @Composable () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .shadow(
                elevation = if (isAccent) 16.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (isAccent) primary.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .background(
                if (isAccent) {
                    Brush.linearGradient(listOf(primary, secondary))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), surface.copy(alpha = 0.32f)))
                }
            )
            .border(
                width = if (isAccent) 1.5.dp else 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (isAccent) 0.7f else 0.45f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
