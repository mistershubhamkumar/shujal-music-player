package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LyricsData
import com.example.model.Track
import com.example.ui.AsyncLoadState

@Composable
fun SyncedLyricsDisplay(
    track: Track,
    positionMs: Long,
    lyricsState: AsyncLoadState<LyricsData>,
    onRetryFetch: () -> Unit,
    onPickLrcFile: () -> Unit,
    onOpenManualEditor: () -> Unit,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        surface.copy(alpha = 0.92f),
                        primary.copy(alpha = 0.08f),
                        surface.copy(alpha = 0.94f)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("synced_lyrics_display")
    ) {
        // Header with Track Meta + .LRC import + Manual Edit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                Column {
                    Text(
                        text = if ((lyricsState as? AsyncLoadState.Success)?.data?.syncedLines?.isNotEmpty() == true)
                            "Synced LRC Lyrics" else "Lyrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onPickLrcFile,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.testTag("pick_lrc_button")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Load LRC file", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("LRC")
                }
                IconButton(
                    onClick = onOpenManualEditor,
                    modifier = Modifier.testTag("edit_lyrics_button")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit lyrics")
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        when (lyricsState) {
            is AsyncLoadState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(34.dp))
                        Text("Fetching synced LRC lyrics...", style = MaterialTheme.typography.bodyMedium, color = muted)
                    }
                }
            }

            is AsyncLoadState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = lyricsState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = muted
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onRetryFetch) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Retry")
                        }
                        OutlinedButton(onClick = onPickLrcFile) {
                            Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Load .LRC")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onOpenManualEditor) {
                        Text("Paste Lyrics Manually")
                    }
                }
            }

            is AsyncLoadState.Success -> {
                val data = lyricsState.data
                if (data.syncedLines.isNotEmpty()) {
                    val activeIdx = data.syncedLines.indexOfLast { it.timeMs <= positionMs }
                    LaunchedEffect(activeIdx, data.trackKey) {
                        val scrollTarget = if (activeIdx >= 0) (activeIdx - 2).coerceAtLeast(0) else 0
                        runCatching { listState.animateScrollToItem(scrollTarget) }
                    }

                    // Prelude dots before the first lyric starts
                    if (activeIdx < 0 && data.syncedLines.first().timeMs > 1500L) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { idx ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(primary.copy(alpha = if ((positionMs / 450L) % 3 == idx.toLong()) 1f else 0.3f))
                                )
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(data.syncedLines, key = { i, l -> "${i}_${l.timeMs}" }) { idx, line ->
                            val isActive = idx == activeIdx
                            val isPast = idx < activeIdx
                            val nextTimeMs = data.syncedLines.getOrNull(idx + 1)?.timeMs ?: (line.timeMs + 4500L)
                            val lineSpan = (nextTimeMs - line.timeMs).coerceAtLeast(600L)
                            val lineProgress = if (isActive) {
                                ((positionMs - line.timeMs).toFloat() / lineSpan.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            val targetColor by animateColorAsState(
                                targetValue = when {
                                    isActive -> onSurface
                                    isPast -> muted.copy(alpha = 0.45f)
                                    else -> muted.copy(alpha = 0.65f)
                                },
                                label = "lyric_color"
                            )
                            val targetScale by animateFloatAsState(
                                targetValue = if (isActive) 1.03f else 0.97f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "lyric_scale"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = targetScale
                                        scaleY = targetScale
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isActive) primary.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable { onSeekToLine(line.timeMs) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .alpha(if (isActive) 1f else 0.72f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(22.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Brush.verticalGradient(listOf(primary, secondary)))
                                        )
                                    }
                                    Text(
                                        text = line.text,
                                        style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        color = targetColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatTimeMs(line.timeMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) primary else muted.copy(alpha = 0.45f)
                                    )
                                }

                                if (isActive) {
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(primary.copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(lineProgress.coerceAtLeast(0.05f))
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Brush.horizontalGradient(listOf(primary, secondary)))
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            Text(
                                text = data.plainLyrics.ifBlank { "No lyrics stored yet." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = onSurface
                            )
                        }
                    }
                }
            }

            AsyncLoadState.Idle -> Unit
        }
    }
}
