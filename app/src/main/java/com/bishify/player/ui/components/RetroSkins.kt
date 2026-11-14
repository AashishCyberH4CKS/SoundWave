package com.bishify.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bishify.player.model.Song
import kotlin.math.atan2

enum class PlayerSkin(val displayName: String) {
    MODERN_GLASS("Liquid Glass"),
    IPOD_CLASSIC("iPod Click-Wheel"),
    RETRO_CASSETTE("90s Cassette Tape"),
    CYBERPUNK("Cyberpunk Neon")
}

/**
 * Classic iPod Click-Wheel Skin with interactive rotary scrubbing and haptic feedback.
 */
@Composable
fun IpodClickWheelSkin(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var lastAngle by remember { mutableFloatStateOf(0f) }

    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE2E8F0)) // Classic silver/light grey body
            .border(2.dp, Color(0xFFCBD5E1), RoundedCornerShape(32.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Classic LCD Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1F5F9))
                .border(2.dp, Color(0xFF94A3B8), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Album Art
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                ) {
                    if (!song?.artworkUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song?.artworkUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "No Track",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song?.artist ?: "Unknown Artist",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Retro LCD Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFCBD5E1))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(Color(0xFF0284C7))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Song.formatDuration(currentPositionMs), color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(Song.formatDuration(durationMs), color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Classic Rotating Click-Wheel
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            lastAngle = atan2(offset.y - center.y, offset.x - center.x)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val currentAngle = atan2(change.position.y - center.y, change.position.x - center.x)
                            var delta = currentAngle - lastAngle
                            if (delta > Math.PI) delta -= (2 * Math.PI).toFloat()
                            if (delta < -Math.PI) delta += (2 * Math.PI).toFloat()

                            if (Math.abs(delta) > 0.12f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val seekStepMs = (delta * 4000L).toLong()
                                onSeekTo((currentPositionMs + seekStepMs).coerceIn(0L, durationMs))
                                lastAngle = currentAngle
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // "MENU" text at Top
            Text(
                text = "MENU",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            )

            // Previous Button at Left
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color(0xFF64748B))
            }

            // Next Button at Right
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color(0xFF64748B))
            }

            // Play/Pause at Bottom
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color(0xFF64748B)
                )
            }

            // Center Action Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
                    .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                    .clickable { onPlayPause() }
            )
        }
    }
}

/**
 * Retro 90s Cassette Tape Skin with animated revolving gear spools.
 */
@Composable
fun RetroCassetteSkin(
    song: Song?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cassette_spool")
    val spoolAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 2000 else Int.MAX_VALUE, easing = LinearEasing)
        ),
        label = "spool_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF18181B)) // Matte black vintage shell
            .border(2.dp, Color(0xFF3F3F46), RoundedCornerShape(16.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cassette Header Label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFACC15).copy(alpha = 0.9f)) // Vintage yellow paper label
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "${song?.title ?: "Tape 1"} - ${song?.artist ?: "Side A"}",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transparent Window with Twin Rotating Spools
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF09090B))
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Spool
                    CassetteSpool(rotation = if (isPlaying) spoolAngle else 0f)

                    // Magnetic Tape Bridge
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .padding(horizontal = 8.dp)
                            .background(Color(0xFF451A03)) // Dark tape brown
                    )

                    // Right Spool
                    CassetteSpool(rotation = if (isPlaying) spoolAngle else 0f)
                }
            }
        }
    }
}

@Composable
private fun CassetteSpool(rotation: Float) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .rotate(rotation)
            .clip(CircleShape)
            .background(Color.White)
            .border(3.dp, Color(0xFFE4E4E7), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(color = Color(0xFF18181B), radius = 10.dp.toPx(), center = center)
            // 3 spokes
            for (i in 0..2) {
                val rad = Math.toRadians((i * 120).toDouble())
                val endX = center.x + (16.dp.toPx() * Math.cos(rad)).toFloat()
                val endY = center.y + (16.dp.toPx() * Math.sin(rad)).toFloat()
                drawLine(
                    color = Color(0xFF71717A),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}

/**
 * Cyberpunk Turntable Skin with glowing neon ring and pulsing holographic vinyl.
 */
@Composable
fun CyberpunkTurntableSkin(
    song: Song?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyberpunk_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_pulse"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 3500 else Int.MAX_VALUE, easing = LinearEasing)
        ),
        label = "neon_spin"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            )
            .border(1.5.dp, Color(0xFF06B6D4).copy(alpha = pulseAlpha), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Holographic Vinyl Disc
        Box(
            modifier = Modifier
                .size(200.dp)
                .rotate(if (isPlaying) spinAngle else 0f)
                .clip(CircleShape)
                .background(Color(0xFF020617))
                .border(2.dp, Color(0xFFF43F5E).copy(alpha = pulseAlpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Grooves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(color = Color(0xFF06B6D4).copy(alpha = 0.35f), radius = 80.dp.toPx(), center = center, style = Stroke(1.5f))
                drawCircle(color = Color(0xFFF43F5E).copy(alpha = 0.35f), radius = 60.dp.toPx(), center = center, style = Stroke(1.5f))
                drawCircle(color = Color(0xFF06B6D4).copy(alpha = 0.5f), radius = 40.dp.toPx(), center = center, style = Stroke(2f))
            }

            // Center Artwork
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF06B6D4), CircleShape)
            ) {
                if (!song?.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song?.artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

