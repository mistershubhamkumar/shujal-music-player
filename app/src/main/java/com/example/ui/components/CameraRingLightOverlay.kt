package com.example.ui.components

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.RingLightTheme
import com.example.model.Track
import kotlin.math.abs
import kotlin.math.roundToInt

fun ringLightThemePalette(theme: RingLightTheme, primary: Color, secondary: Color): List<Color> = when (theme) {
    RingLightTheme.RAINBOW_FLOW -> listOf(
        Color(0xFF00F5D4), Color(0xFF38BDF8), Color(0xFFA855F7),
        Color(0xFFFF2E93), Color(0xFFFBBF24), Color(0xFF00F5D4)
    )
    RingLightTheme.NEON_CYAN -> listOf(Color(0xFF00F5D4), Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF00F5D4))
    RingLightTheme.SOLAR_GOLD -> listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFF97316), Color(0xFFFDE047))
    RingLightTheme.CRIMSON_PULSE -> listOf(Color(0xFFFA2D48), Color(0xFFFF6B81), Color(0xFFEC4899), Color(0xFFFA2D48))
    RingLightTheme.EMERALD_BEAM -> listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF2DD4BF), Color(0xFF10B981))
    RingLightTheme.ARCTIC_WHITE -> listOf(Color.White, Color(0xFFBAE6FD), Color(0xFFE0F2FE), Color.White)
    RingLightTheme.VIOLET_NEBULA -> listOf(Color(0xFFC084FC), Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFFC084FC))
    RingLightTheme.MUSIC_SYNC -> listOf(primary, secondary, Color(0xFFFFFFFF), primary)
}

object RingLightHardwareHelper {
    private var torchEnabled = false

    fun adjustMusicVolume(context: Context, direction: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun toggleMute(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isMuted = am.isStreamMute(AudioManager.STREAM_MUSIC)
            am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI
            )
        } else {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
    }

    fun toggleCameraTorch(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return runCatching {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            torchEnabled = !torchEnabled
            cm.setTorchMode(cameraId, torchEnabled)
            true
        }.getOrDefault(false)
    }
}

@Composable
fun CameraRingLightGraphic(
    theme: RingLightTheme,
    sizeDp: Dp,
    thicknessDp: Dp,
    isPlaying: Boolean,
    visualizerLevel: Float = 0.5f,
    isPositionEditing: Boolean = false,
    onTap: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onDragCursorDelta: (dxPx: Float, dyPx: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val palette = remember(theme, primary, secondary) {
        ringLightThemePalette(theme, primary, secondary)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ring_light_anim")
    val spinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 2400 else 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_spin"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    val effectiveAlpha = if (theme == RingLightTheme.MUSIC_SYNC && isPlaying) {
        (0.65f + visualizerLevel * 0.35f).coerceIn(0.65f, 1f)
    } else {
        pulse
    }

    // Touch box slightly larger than ring for crisp tap & swipe detection around camera cutout
    val touchBoxSize = (sizeDp + 20.dp).coerceAtLeast(52.dp)

    Box(
        modifier = modifier
            .size(touchBoxSize)
            .pointerInput(isPositionEditing) {
                if (isPositionEditing) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragCursorDelta(dragAmount.x, dragAmount.y)
                    }
                } else {
                    var totalDx = 0f
                    var totalDy = 0f
                    detectDragGestures(
                        onDragStart = {
                            totalDx = 0f
                            totalDy = 0f
                        },
                        onDragEnd = {
                            val threshold = 26f
                            if (abs(totalDx) >= abs(totalDy) && abs(totalDx) > threshold) {
                                if (totalDx < 0f) onSwipeLeft() else onSwipeRight()
                            } else if (abs(totalDy) > threshold) {
                                if (totalDy < 0f) onSwipeUp() else onSwipeDown()
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        totalDx += dragAmount.x
                        totalDy += dragAmount.y
                    }
                }
            }
            .pointerInput(isPositionEditing) {
                if (!isPositionEditing) {
                    detectTapGestures(onTap = { onTap() })
                }
            }
            .testTag("camera_ring_light"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val strokePx = thicknessDp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)
            val brush = Brush.sweepGradient(palette)

            // Outer ambient halo glow
            drawCircle(
                color = palette.first().copy(alpha = 0.24f * effectiveAlpha),
                radius = (size.minDimension / 2f) + strokePx * 0.9f
            )

            rotate(degrees = spinDeg) {
                drawArc(
                    brush = brush,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    alpha = effectiveAlpha,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            if (isPositionEditing) {
                // Crosshair cursor guides so user can align precisely around camera lens
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawLine(
                    color = Color.White,
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun RingLightSongPopup(
    track: Track?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    GlassmorphicSurface(
        cornerRadius = 26.dp,
        contentPadding = 12.dp,
        tintAlpha = 0.88f,
        modifier = modifier
            .widthIn(min = 250.dp, max = 330.dp)
            .shadow(22.dp, RoundedCornerShape(26.dp))
            .testTag("ring_light_song_popup")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                TrackArtworkImage(
                    artworkUri = track?.artworkUri,
                    isRadio = track?.isRadio == true,
                    contentDescription = track?.title ?: "Song",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "Shujal Music Ready",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.artist ?: "Tap Play to start music",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onOpenPlayer,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.OpenInFull, contentDescription = "Open player", modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close popup", modifier = Modifier.size(16.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("ring_popup_prev")
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous song", modifier = Modifier.size(26.dp))
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(primary, secondary)))
                    .clickable(onClick = onPlayPause)
                    .testTag("ring_popup_play_pause"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF040710),
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("ring_popup_next")
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next song", modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
fun InAppCameraRingLightLayer(
    visible: Boolean,
    isPositionEditing: Boolean,
    xFraction: Float,
    yDp: Float,
    sizeDp: Float,
    thicknessDp: Float,
    theme: RingLightTheme,
    currentTrack: Track?,
    isPlaying: Boolean,
    visualizerLevel: Float,
    showSongPopup: Boolean,
    onTap: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onPositionChanged: (newXFraction: Float, newYDp: Float) -> Unit,
    onDoneEditing: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    onDismissPopup: () -> Unit
) {
    if (!visible && !isPositionEditing) return
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val touchSizeDp = (sizeDp + 20f).coerceAtLeast(52f)
        val touchHalfPx = with(density) { (touchSizeDp / 2f).dp.toPx() }
        val ringTopPx = with(density) { yDp.dp.toPx() }
        val ringCenterX = (xFraction * screenWidthPx).coerceIn(touchHalfPx, screenWidthPx - touchHalfPx)

        CameraRingLightGraphic(
            theme = theme,
            sizeDp = sizeDp.dp,
            thicknessDp = thicknessDp.dp,
            isPlaying = isPlaying,
            visualizerLevel = visualizerLevel,
            isPositionEditing = isPositionEditing,
            onTap = onTap,
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            onSwipeUp = onSwipeUp,
            onSwipeDown = onSwipeDown,
            onDragCursorDelta = { dxPx, dyPx ->
                val updatedX = ((xFraction * screenWidthPx) + dxPx) / screenWidthPx
                val dyDp = with(density) { dyPx.toDp().value }
                onPositionChanged(
                    updatedX.coerceIn(0.05f, 0.95f),
                    (yDp + dyDp).coerceIn(-12f, 160f)
                )
            },
            modifier = Modifier.offset {
                IntOffset(
                    x = (ringCenterX - touchHalfPx).roundToInt(),
                    y = ringTopPx.roundToInt()
                )
            }
        )

        // Song Control Popup anchored cleanly below the Camera Ring Light
        AnimatedVisibility(
            visible = showSongPopup && !isPositionEditing,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (yDp + sizeDp + 18f).coerceAtLeast(64f).dp, start = 16.dp, end = 16.dp)
        ) {
            RingLightSongPopup(
                track = currentTrack,
                isPlaying = isPlaying,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onOpenPlayer = onOpenPlayer,
                onClose = onDismissPopup
            )
        }

        // Cursor Positioning Helper Bar
        AnimatedVisibility(
            visible = isPositionEditing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp, start = 20.dp, end = 20.dp)
        ) {
            GlassmorphicSurface(
                cornerRadius = 24.dp,
                contentPadding = 14.dp,
                tintAlpha = 0.90f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Move Ring Light to Camera",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Drag the crosshair ring directly onto your front camera cutout, then lock it.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = onDoneEditing,
                        modifier = Modifier.testTag("ring_cursor_done_button")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lock")
                    }
                }
            }
        }
    }
}
