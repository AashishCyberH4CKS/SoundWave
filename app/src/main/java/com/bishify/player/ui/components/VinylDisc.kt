package com.bishify.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bishify.player.ui.theme.LocalSoundWaveColors

@Composable
fun VinylDisc(
    isPlaying: Boolean,
    artworkUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    animated: Boolean = true
) {
    val bishifyColors = LocalSoundWaveColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    // Freeze rotation angle when paused
    var lastAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            lastAngle = (lastAngle + rotation) % 360f
        }
    }

    val currentRotation = if (animated && isPlaying) (rotation + lastAngle) % 360f else lastAngle

    Box(
        modifier = modifier
            .size(size)
            .shadow(24.dp, shape = CircleShape, spotColor = bishifyColors.primary.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(Color(0xFF0D0E12))
            .border(2.dp, Color(0xFF282B35), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Vinyl Grooves & Sheen
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(currentRotation)
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // Concentric groove rings
            val grooveRadii = listOf(0.92f, 0.86f, 0.80f, 0.74f, 0.68f, 0.62f, 0.56f)
            grooveRadii.forEach { factor ->
                drawCircle(
                    color = Color(0x18FFFFFF),
                    radius = radius * factor,
                    center = center,
                    style = Stroke(width = 1.2f)
                )
            }

            // Radial Sheen reflection highlight
            val sheenBrush = Brush.sweepGradient(
                0.0f to Color.Transparent,
                0.15f to Color(0x22FFFFFF),
                0.25f to Color.Transparent,
                0.50f to Color.Transparent,
                0.65f to Color(0x22FFFFFF),
                0.75f to Color.Transparent,
                1.0f to Color.Transparent,
                center = center
            )
            drawCircle(brush = sheenBrush, radius = radius, center = center)
        }

        // Center Album Artwork Label
        val centerSize = size * 0.46f
        Box(
            modifier = Modifier
                .size(centerSize)
                .rotate(currentRotation)
                .clip(CircleShape)
                .border(3.dp, bishifyColors.primary, CircleShape)
                .background(bishifyColors.surface),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Vinyl Label Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.bishify.player.R.drawable.soundwave_logo_branded),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Center Spindle Hole
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D0E12))
                    .border(1.5.dp, Color(0xFF383C49), CircleShape)
            )
        }
    }
}

