package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.DynamicIslandStyle
import com.example.model.Track

private data class IslandVisualSpec(
    val containerBrush: Brush,
    val borderBrush: Brush,
    val borderWidth: Dp,
    val cornerRadiusCollapsed: Dp,
    val cornerRadiusExpanded: Dp,
    val accentColor: Color,
    val secondaryAccent: Color,
    val textColor: Color,
    val subTextColor: Color,
    val shadowColor: Color,
    val playBgBrush: Brush,
    val playIconTint: Color,
    val showBottomProgressLine: Boolean,
    val showVinylRing: Boolean
)

@Composable
fun DynamicIslandCapsule(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    visualizerLevels: List<Float>,
    style: DynamicIslandStyle = DynamicIslandStyle.CLASSIC_PILL,
    alwaysExtended: Boolean = false,
    isFloatingOverlay: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenPlayer: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    onDragDelta: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isWideMode = alwaysExtended || style == DynamicIslandStyle.WIDE_CAPSULE
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition(label = "island_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mini_art_spin"
    )

    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val spec = remember(style, primary, secondary, isPlaying, glowAlpha) {
        buildIslandVisualSpec(style, primary, secondary, isPlaying, glowAlpha)
    }

    val shape = RoundedCornerShape(
        if (expanded) spec.cornerRadiusExpanded else spec.cornerRadiusCollapsed
    )

    val dragModifier = if (onDragDelta != null) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount.x, dragAmount.y)
            }
        }
    } else Modifier

    Box(
        modifier = modifier
            .then(dragModifier)
            .then(if (isWideMode) Modifier.fillMaxWidth() else Modifier)
            .widthIn(min = if (isWideMode) 336.dp else 248.dp, max = if (expanded || isWideMode) 388.dp else 326.dp)
            .shadow(
                elevation = if (style == DynamicIslandStyle.NEON_PULSE) 26.dp else 20.dp,
                shape = shape,
                ambientColor = spec.shadowColor,
                spotColor = spec.shadowColor
            )
            .clip(shape)
            .background(spec.containerBrush)
            .border(width = spec.borderWidth, brush = spec.borderBrush, shape = shape)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clickable { expanded = !expanded }
            .padding(
                horizontal = if (expanded) 16.dp else if (isWideMode) 14.dp else 10.dp,
                vertical = if (expanded) 14.dp else if (isWideMode) 9.dp else 7.dp
            )
            .testTag(if (isFloatingOverlay) "floating_dynamic_island" else "in_app_dynamic_island")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Artwork (standard rounded pill, glowing badge, or grooved turntable vinyl)
                val artSize = when {
                    expanded -> 48.dp
                    spec.showVinylRing -> 36.dp
                    else -> 32.dp
                }
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .clip(
                            RoundedCornerShape(
                                if (expanded && !spec.showVinylRing) 14.dp else 999.dp
                            )
                        )
                        .then(
                            if (spec.showVinylRing) {
                                Modifier
                                    .background(Color(0xFF0E0C0B))
                                    .border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.65f), CircleShape)
                                    .padding(4.dp)
                            } else Modifier
                        )
                        .graphicsLayer {
                            if (isPlaying && (!expanded || spec.showVinylRing)) rotationZ = spinAngle
                        },
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtworkImage(
                        artworkUri = track.artworkUri,
                        isRadio = track.isRadio,
                        contentDescription = track.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                    if (spec.showVinylRing) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF181412))
                                .border(1.dp, Color(0xFFFBBF24), CircleShape)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        style = if (expanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                        fontWeight = if (style == DynamicIslandStyle.NEON_PULSE || style == DynamicIslandStyle.AURORA_WAVE) {
                            FontWeight.SemiBold
                        } else FontWeight.Medium,
                        color = spec.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (expanded) {
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = spec.subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MiniIslandEqualizerBars(
                                levels = visualizerLevels.take(if (style == DynamicIslandStyle.AURORA_WAVE) 7 else 5),
                                isPlaying = isPlaying,
                                color = spec.accentColor
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = spec.subTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Compact controls or expand/dismiss controls
                if (!expanded) {
                    if (style != DynamicIslandStyle.MINIMAL_NOTCH || isWideMode) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .size(32.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("island_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous track",
                                tint = spec.textColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(spec.playBgBrush)
                            .clickable(onClick = onPlayPause)
                            .testTag("island_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "island_play_icon"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = spec.playIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(32.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("island_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next track",
                            tint = spec.textColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onOpenPlayer,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("island_open_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = "Open full player",
                            tint = spec.textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .testTag("island_dismiss_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Hide overlay",
                                tint = spec.textColor.copy(alpha = 0.78f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Sleek hairline progress bar in collapsed view for Frost Glass, Minimal Notch, or Long Capsule mode
            if (!expanded && (spec.showBottomProgressLine || isWideMode)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(2.dp)
                        .clip(CircleShape),
                    color = spec.accentColor,
                    trackColor = Color.White.copy(alpha = 0.14f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = spec.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        LinearProgressIndicator(
                            progress = { if (track.isRadio) 1f else progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = spec.accentColor,
                            trackColor = Color.White.copy(alpha = 0.16f)
                        )
                        Text(
                            text = if (track.isRadio) "LIVE" else formatTimeMs(positionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = spec.subTextColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("island_expanded_prev")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous track",
                                tint = spec.textColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(spec.accentColor, spec.secondaryAccent)))
                                .clickable(onClick = onPlayPause)
                                .testTag("island_expanded_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF040710),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("island_expanded_next")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next track",
                                tint = spec.textColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildIslandVisualSpec(
    style: DynamicIslandStyle,
    primary: Color,
    secondary: Color,
    isPlaying: Boolean,
    glowAlpha: Float
): IslandVisualSpec {
    return when (style) {
        DynamicIslandStyle.CLASSIC_PILL -> IslandVisualSpec(
            containerBrush = Brush.verticalGradient(
                listOf(Color(0xFF0A0D16).copy(alpha = 0.96f), Color(0xFF121828).copy(alpha = 0.95f))
            ),
            borderBrush = Brush.linearGradient(
                listOf(
                    primary.copy(alpha = if (isPlaying) glowAlpha else 0.30f),
                    secondary.copy(alpha = if (isPlaying) 0.65f else 0.25f),
                    Color.White.copy(alpha = 0.20f)
                )
            ),
            borderWidth = 1.dp,
            cornerRadiusCollapsed = 999.dp,
            cornerRadiusExpanded = 28.dp,
            accentColor = primary,
            secondaryAccent = secondary,
            textColor = Color.White,
            subTextColor = Color.White.copy(alpha = 0.68f),
            shadowColor = primary.copy(alpha = 0.45f),
            playBgBrush = Brush.linearGradient(listOf(primary.copy(alpha = 0.24f), primary.copy(alpha = 0.16f))),
            playIconTint = primary,
            showBottomProgressLine = false,
            showVinylRing = false
        )

        DynamicIslandStyle.WIDE_CAPSULE -> IslandVisualSpec(
            containerBrush = Brush.horizontalGradient(
                listOf(
                    Color(0xFF0B111E).copy(alpha = 0.96f),
                    Color(0xFF152036).copy(alpha = 0.96f),
                    Color(0xFF0B111E).copy(alpha = 0.96f)
                )
            ),
            borderBrush = Brush.linearGradient(
                listOf(
                    primary.copy(alpha = if (isPlaying) glowAlpha else 0.45f),
                    Color.White.copy(alpha = 0.40f),
                    secondary.copy(alpha = if (isPlaying) glowAlpha else 0.40f)
                )
            ),
            borderWidth = 1.3.dp,
            cornerRadiusCollapsed = 999.dp,
            cornerRadiusExpanded = 28.dp,
            accentColor = primary,
            secondaryAccent = secondary,
            textColor = Color.White,
            subTextColor = Color.White.copy(alpha = 0.74f),
            shadowColor = primary.copy(alpha = 0.55f),
            playBgBrush = Brush.linearGradient(listOf(primary, secondary)),
            playIconTint = Color(0xFF040710),
            showBottomProgressLine = true,
            showVinylRing = false
        )

        DynamicIslandStyle.LIQUID_GLASS -> IslandVisualSpec(
            containerBrush = Brush.verticalGradient(
                listOf(Color(0xFF28354D).copy(alpha = 0.86f), Color(0xFF101929).copy(alpha = 0.90f))
            ),
            borderBrush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.55f), Color(0xFF7DD3FC).copy(alpha = 0.35f))
            ),
            borderWidth = 1.2.dp,
            cornerRadiusCollapsed = 26.dp,
            cornerRadiusExpanded = 30.dp,
            accentColor = Color(0xFF7DD3FC),
            secondaryAccent = Color(0xFFE0F2FE),
            textColor = Color(0xFFF8FAFC),
            subTextColor = Color(0xFFCBD5E1),
            shadowColor = Color(0xFF38BDF8).copy(alpha = 0.45f),
            playBgBrush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.24f), Color(0xFF7DD3FC).copy(alpha = 0.24f))),
            playIconTint = Color.White,
            showBottomProgressLine = true,
            showVinylRing = false
        )

        DynamicIslandStyle.NEON_PULSE -> IslandVisualSpec(
            containerBrush = Brush.horizontalGradient(
                listOf(Color(0xFF0E0720).copy(alpha = 0.97f), Color(0xFF170B2E).copy(alpha = 0.97f))
            ),
            borderBrush = Brush.linearGradient(
                listOf(
                    Color(0xFFFF2E93).copy(alpha = if (isPlaying) glowAlpha else 0.50f),
                    Color(0xFF00F5D4).copy(alpha = if (isPlaying) glowAlpha else 0.45f),
                    Color(0xFF8B5CF6).copy(alpha = 0.70f)
                )
            ),
            borderWidth = 1.8.dp,
            cornerRadiusCollapsed = 999.dp,
            cornerRadiusExpanded = 26.dp,
            accentColor = Color(0xFF00F5D4),
            secondaryAccent = Color(0xFFFF2E93),
            textColor = Color.White,
            subTextColor = Color(0xFF67E8F9),
            shadowColor = Color(0xFFFF2E93).copy(alpha = 0.65f),
            playBgBrush = Brush.linearGradient(listOf(Color(0xFFFF2E93), Color(0xFF00F5D4))),
            playIconTint = Color(0xFF090414),
            showBottomProgressLine = false,
            showVinylRing = false
        )

        DynamicIslandStyle.VINYL_DISC -> IslandVisualSpec(
            containerBrush = Brush.verticalGradient(
                listOf(Color(0xFF1F1916).copy(alpha = 0.97f), Color(0xFF120E0C).copy(alpha = 0.97f))
            ),
            borderBrush = Brush.linearGradient(
                listOf(Color(0xFFF59E0B).copy(alpha = 0.72f), Color(0xFF78350F).copy(alpha = 0.55f))
            ),
            borderWidth = 1.2.dp,
            cornerRadiusCollapsed = 22.dp,
            cornerRadiusExpanded = 24.dp,
            accentColor = Color(0xFFFBBF24),
            secondaryAccent = Color(0xFFD97706),
            textColor = Color(0xFFFEF3C7),
            subTextColor = Color(0xFFD6B98D),
            shadowColor = Color(0xFFF59E0B).copy(alpha = 0.42f),
            playBgBrush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
            playIconTint = Color(0xFF1A120B),
            showBottomProgressLine = false,
            showVinylRing = true
        )

        DynamicIslandStyle.AURORA_WAVE -> IslandVisualSpec(
            containerBrush = Brush.horizontalGradient(
                listOf(
                    Color(0xFF132A2E).copy(alpha = 0.95f),
                    Color(0xFF1D1B38).copy(alpha = 0.95f),
                    Color(0xFF2B1835).copy(alpha = 0.95f)
                )
            ),
            borderBrush = Brush.linearGradient(
                listOf(
                    Color(0xFF34D399).copy(alpha = glowAlpha),
                    Color(0xFFA78BFA).copy(alpha = 0.80f),
                    Color(0xFFF472B6).copy(alpha = glowAlpha)
                )
            ),
            borderWidth = 1.4.dp,
            cornerRadiusCollapsed = 999.dp,
            cornerRadiusExpanded = 30.dp,
            accentColor = Color(0xFF34D399),
            secondaryAccent = Color(0xFFA78BFA),
            textColor = Color.White,
            subTextColor = Color(0xFFA7F3D0),
            shadowColor = Color(0xFF34D399).copy(alpha = 0.48f),
            playBgBrush = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFFA78BFA))),
            playIconTint = Color(0xFF061613),
            showBottomProgressLine = false,
            showVinylRing = false
        )

        DynamicIslandStyle.MINIMAL_NOTCH -> IslandVisualSpec(
            containerBrush = Brush.verticalGradient(
                listOf(Color(0xFF000000).copy(alpha = 0.98f), Color(0xFF070709).copy(alpha = 0.98f))
            ),
            borderBrush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f))
            ),
            borderWidth = 0.8.dp,
            cornerRadiusCollapsed = 18.dp,
            cornerRadiusExpanded = 22.dp,
            accentColor = Color.White,
            secondaryAccent = primary,
            textColor = Color.White,
            subTextColor = Color.White.copy(alpha = 0.55f),
            shadowColor = Color.Black.copy(alpha = 0.65f),
            playBgBrush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.08f))),
            playIconTint = Color.White,
            showBottomProgressLine = true,
            showVinylRing = false
        )
    }
}

@Composable
private fun MiniIslandEqualizerBars(
    levels: List<Float>,
    isPlaying: Boolean,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(12.dp)
    ) {
        levels.forEachIndexed { idx, level ->
            val h = if (isPlaying) (3f + level * 9f).dp else (3 + (idx % 2) * 2).dp
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(h)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun TrackArtworkImage(
    artworkUri: String?,
    isRadio: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val fallbackRes = if (isRadio) R.drawable.img_radio_waves else R.drawable.img_default_album
    if (!artworkUri.isNullOrBlank()) {
        AsyncImage(
            model = artworkUri,
            contentDescription = contentDescription,
            placeholder = painterResource(fallbackRes),
            error = painterResource(fallbackRes),
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxWidth()
        )
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
