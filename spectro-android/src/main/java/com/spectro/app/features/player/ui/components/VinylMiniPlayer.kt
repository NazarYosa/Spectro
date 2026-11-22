package com.spectro.app.features.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.playback.PlayerConnection
import kotlinx.coroutines.isActive

/**
 * Floating Vinyl Mini Player (Piringan Hitam)
 * - Black vinyl record disc with concentric grooves and realistic sheen
 * - Album art of currently playing track in the center
 * - Continuously rotates 360° when music is playing (freezes angle on pause)
 * - Surrounded by a circular seekbar with Spectro gradient
 * - Positioned at bottom right
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VinylMiniPlayer(
    coverUrl: String?,
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // 360-degree rotation that spins when playing and holds angle when paused
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastTime = withFrameMillis { it }
            while (isActive) {
                val currentTime = withFrameMillis { it }
                val delta = (currentTime - lastTime) / 1000f
                lastTime = currentTime
                // 45 degrees per second = 8 seconds per full 360 rotation
                rotationAngle = (rotationAngle + delta * 45f) % 360f
            }
        }
    }

    val gradientColors = remember {
        listOf(
            Color(0xFF8A4AF3), // Electric Violet
            Color(0xFFFF4B72), // Radiant Rose Pink
            Color(0xFF00C2FF), // Vibrant Cyan
            Color(0xFF8A4AF3)  // Loop back to Violet
        )
    }

    Box(
        modifier = modifier
            .size(66.dp)
            .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    PlayerConnection.togglePlayPause()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Circular Seekbar Ring around the vinyl record
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Background inactive track ring
            drawCircle(
                color = Color(0x35FFFFFF),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Foreground active progress arc with sweep gradient
            val sweepGradient = Brush.sweepGradient(
                colors = gradientColors,
                center = center
            )
            drawArc(
                brush = sweepGradient,
                startAngle = -90f,
                sweepAngle = (progress.coerceIn(0f, 1f) * 360f),
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // 2. Vinyl Disc (Piringan Hitam) that rotates
        Box(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer { rotationZ = rotationAngle }
                .clip(CircleShape)
                .background(Color(0xFF101013)),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl grooves & gloss sheen
            Canvas(modifier = Modifier.fillMaxSize()) {
                val discRadius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Concentric groove lines
                val grooveColor = Color(0x22FFFFFF)
                val grooveStroke = Stroke(width = 0.8.dp.toPx())
                drawCircle(color = grooveColor, radius = discRadius * 0.92f, center = center, style = grooveStroke)
                drawCircle(color = grooveColor, radius = discRadius * 0.84f, center = center, style = grooveStroke)
                drawCircle(color = grooveColor, radius = discRadius * 0.76f, center = center, style = grooveStroke)
                drawCircle(color = grooveColor, radius = discRadius * 0.68f, center = center, style = grooveStroke)

                // Vinyl sheen reflection (butterfly sheen effect)
                val sheenBrush = Brush.sweepGradient(
                    0.0f to Color(0x18FFFFFF),
                    0.25f to Color.Transparent,
                    0.5f to Color(0x18FFFFFF),
                    0.75f to Color.Transparent,
                    1.0f to Color(0x18FFFFFF)
                )
                drawCircle(brush = sheenBrush, radius = discRadius, center = center)
            }

            // 3. Center Label: Circular Album Artwork
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0x40FFFFFF), CircleShape)
                    .background(Color(0xFF1A1A1E)),
                contentAlignment = Alignment.Center
            ) {
                if (!coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = SpectroIcons.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // 4. Center Spindle Hole of the vinyl
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF08080A))
                        .border(0.75.dp, Color(0x99FFFFFF), CircleShape)
                )
            }
        }
    }
}
