package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Track

@Composable
fun LiquidAuroraBackground(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val bg = MaterialTheme.colorScheme.background

    val transition = rememberInfiniteTransition(label = "liquid_aurora")
    val drift1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1"
    )
    val drift2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2"
    )

    Canvas(modifier = modifier.fillMaxSize().background(bg)) {
        val c1 = Offset(size.width * (0.2f + 0.25f * drift1), size.height * (0.15f + 0.1f * drift2))
        val c2 = Offset(size.width * (0.85f - 0.2f * drift2), size.height * (0.42f + 0.15f * drift1))
        val c3 = Offset(size.width * 0.45f, size.height * (0.85f - 0.12f * drift1))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                center = c1,
                radius = size.maxDimension * 0.55f
            ),
            center = c1,
            radius = size.maxDimension * 0.55f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.18f), Color.Transparent),
                center = c2,
                radius = size.maxDimension * 0.5f
            ),
            center = c2,
            radius = size.maxDimension * 0.5f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.12f), Color.Transparent),
                center = c3,
                radius = size.maxDimension * 0.48f
            ),
            center = c3,
            radius = size.maxDimension * 0.48f
        )
    }
}

@Composable
fun FrostedArtworkBackdrop(
    artworkUri: String?,
    isRadio: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bg = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize().background(bg)) {
        if (!artworkUri.isNullOrBlank()) {
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.fillMaxSize().blur(56.dp).alpha(0.55f)
            } else {
                Modifier.fillMaxSize().alpha(0.28f)
            }
            Box(modifier = blurModifier) {
                TrackArtworkImage(
                    artworkUri = artworkUri,
                    isRadio = isRadio,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // Atmospheric translucent tint & specular gradient over blurred artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            bg.copy(alpha = 0.58f),
                            primary.copy(alpha = 0.16f),
                            secondary.copy(alpha = 0.14f),
                            bg.copy(alpha = 0.84f)
                        )
                    )
                )
        )
    }
}

@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: Dp = 16.dp,
    tintAlpha: Float = 0.30f,
    content: @Composable ColumnScope.() -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        surface.copy(alpha = tintAlpha + 0.10f),
                        surface.copy(alpha = tintAlpha),
                        primary.copy(alpha = 0.08f)
                    )
                )
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                width = 1.15.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.48f),
                        primary.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassCircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp,
    isHighlighted: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = if (isHighlighted) {
                        listOf(primary.copy(alpha = 0.36f), surface.copy(alpha = 0.42f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.16f), surface.copy(alpha = 0.28f))
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isHighlighted) primary.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        surface.copy(alpha = 0.78f),
                        surface.copy(alpha = 0.58f)
                    )
                )
            )
            .border(1.dp, outline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun LiquidSpectrumVisualizer(
    levels: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val animatedLevels = levels.mapIndexed { index, target ->
        val animated by animateFloatAsState(
            targetValue = if (isPlaying) target else 0.12f,
            animationSpec = spring(stiffness = 380f, dampingRatio = 0.65f),
            label = "bar_$index"
        )
        animated
    }

    Canvas(modifier = modifier.fillMaxWidth().height(42.dp)) {
        val count = animatedLevels.size.coerceAtLeast(1)
        val gap = 4.dp.toPx()
        val totalGap = gap * (count - 1)
        val barWidth = ((size.width - totalGap) / count).coerceAtLeast(3f)

        animatedLevels.forEachIndexed { idx, value ->
            val barHeight = (size.height * value).coerceAtLeast(6.dp.toPx())
            val x = idx * (barWidth + gap)
            val y = (size.height - barHeight) / 2f
            val brush = Brush.verticalGradient(
                colors = listOf(primary, secondary, tertiary)
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(surface.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .pointerInput(track.id) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDrag < -90f) onNext()
                        else if (totalDrag > 90f) onPrevious()
                        totalDrag = 0f
                    }
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                }
            }
            .clickable(onClick = onOpenNowPlaying)
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                TrackArtworkImage(
                    artworkUri = track.artworkUri,
                    isRadio = track.isRadio,
                    contentDescription = track.title
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = primary
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mini_player_next")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next track"
                )
            }
        }

        LinearProgressIndicator(
            progress = { if (track.isRadio) 1f else progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp),
            color = primary,
            trackColor = primary.copy(alpha = 0.14f)
        )
    }
}
