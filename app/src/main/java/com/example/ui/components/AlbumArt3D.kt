package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun AlbumArt3D(
    artworkUri: String?,
    isRadio: Boolean,
    isPlaying: Boolean,
    title: String,
    modifier: Modifier = Modifier
) {
    var dragRotX by remember { mutableFloatStateOf(0f) }
    var dragRotY by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "3d_art")
    val ambientFloat by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levitate"
    )
    val autoTiltY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auto_tilt"
    )
    val vinylSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_spin"
    )

    val rotX by animateFloatAsState(
        targetValue = if (isPlaying) dragRotX + 3f else dragRotX,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.7f),
        label = "rotX"
    )
    val rotY by animateFloatAsState(
        targetValue = if (isPlaying) dragRotY + autoTiltY else dragRotY,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.7f),
        label = "rotY"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 0.94f,
        animationSpec = spring(stiffness = 200f, dampingRatio = 0.75f),
        label = "art_scale"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        dragRotX = 0f
                        dragRotY = 0f
                    },
                    onDragCancel = {
                        dragRotX = 0f
                        dragRotY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragRotY = (dragRotY + dragAmount.x * 0.25f).coerceIn(-22f, 22f)
                    dragRotX = (dragRotX - dragAmount.y * 0.25f).coerceIn(-22f, 22f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Ambient radial aura behind 3D sleeve
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isPlaying) 0.36f else 0.16f),
                        secondary.copy(alpha = if (isPlaying) 0.18f else 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension * 0.56f
                )
            )
        }

        // Peeking spinning vinyl disc behind sleeve when playing
        Box(
            modifier = Modifier
                .size(210.dp)
                .offset { IntOffset(x = if (isPlaying) 48 else 0, y = 0) }
                .graphicsLayer {
                    rotationZ = if (isPlaying) vinylSpin else 0f
                    cameraDistance = 14f * density
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF222A3D),
                            Color(0xFF0A0D14),
                            Color(0xFF1A2234),
                            Color(0xFF070A10)
                        )
                    )
                )
                .border(1.5.dp, primary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(primary, secondary)))
                    .border(4.dp, Color(0xFF080B12), CircleShape)
            )
        }

        // 3D Glassmorphic Album Card
        Box(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = rotX
                    rotationY = rotY
                    scaleX = scale
                    scaleY = scale
                    translationY = if (isPlaying) ambientFloat else 0f
                    cameraDistance = 14f * density
                }
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = primary.copy(alpha = 0.55f),
                    spotColor = secondary.copy(alpha = 0.65f)
                )
                .clip(RoundedCornerShape(30.dp))
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            primary.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
        ) {
            TrackArtworkImage(
                artworkUri = artworkUri,
                isRadio = isRadio,
                contentDescription = title,
                modifier = Modifier.fillMaxSize()
            )

            // Glossy diagonal liquid glass reflection overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f)
                            )
                        )
                    )
            )
        }
    }
}
