package com.bishify.player.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.model.RepeatState
import com.bishify.player.model.Song
import com.bishify.player.ui.components.VinylDisc
import com.bishify.player.ui.theme.LocalSoundWaveColors
import com.bishify.player.ui.viewmodel.MusicViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToDrivingMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundWaveColors.current
    val playbackManager = viewModel.playbackManager
    val currentSong by playbackManager.currentSong.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    val playbackSpeed by playbackManager.playbackSpeed.collectAsState()
    val repeatState by playbackManager.repeatState.collectAsState()
    val isShuffle by playbackManager.isShuffle.collectAsState()
    val queue by playbackManager.queue.collectAsState()
    val currentIndex by playbackManager.currentIndex.collectAsState()
    val sleepTimerMins by playbackManager.sleepTimerRemainingMinutes.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val isFavorite = currentSong?.let { favoriteIds.contains(it.id) } ?: false

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // User dragging the seek slider state
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderTempPosition by remember { mutableFloatStateOf(0f) }

    // Swipe to change songs: Left -> Next, Right -> Previous
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipe_track_offset"
    )

    val activePosition = if (isDraggingSlider) {
        sliderTempPosition.toLong()
    } else {
        currentPosition
    }

    val progress = if (duration > 0) {
        (activePosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PLAYING FROM LIBRARY",
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentSong?.album ?: "SoundWave",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                // Driving Mode Shortcut
                IconButton(
                    onClick = onNavigateToDrivingMode,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Driving Mode",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Audio Effects / Equalizer Shortcut
                IconButton(
                    onClick = onNavigateToEqualizer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Central Vinyl Disc Animation with Horizontal Swipe Gesture (Left -> Next, Right -> Previous)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeOffsetX = 0f },
                        onDragEnd = {
                            if (swipeOffsetX < -80f) {
                                playbackManager.skipNext()
                            } else if (swipeOffsetX > 80f) {
                                playbackManager.skipPrevious()
                            }
                            swipeOffsetX = 0f
                        },
                        onDragCancel = { swipeOffsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-250f, 250f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            VinylDisc(
                isPlaying = isPlaying,
                artworkUri = currentSong?.artworkUri,
                size = 250.dp,
                animated = preferences.vinylAnimationEnabled
            )
        }

        Spacer(modifier = Modifier.weight(0.7f))

        // Song Title, Artist and Favorite
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "No track selected",
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentSong?.artist ?: "Select a song from library",
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    currentSong?.let { viewModel.toggleFavorite(it.id) }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Seek Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    isDraggingSlider = true
                    sliderTempPosition = newProgress * duration.toFloat()
                },
                onValueChangeFinished = {
                    playbackManager.seekTo(sliderTempPosition.toLong())
                    isDraggingSlider = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.surfaceElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Song.formatDuration(activePosition),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = Song.formatDuration(duration),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = { playbackManager.toggleShuffle() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) colors.primary else colors.textTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = { playbackManager.skipPrevious() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Central Play/Pause Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(16.dp, CircleShape, spotColor = colors.primary.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primary, colors.secondary)
                        )
                    )
                    .clickable { playbackManager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Next
            IconButton(
                onClick = { playbackManager.skipNext() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = { playbackManager.cycleRepeatMode() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = when (repeatState) {
                        RepeatState.ONE -> Icons.Default.RepeatOne
                        RepeatState.ALL -> Icons.Default.Repeat
                        RepeatState.OFF -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatState != RepeatState.OFF) colors.primary else colors.textTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Bottom Auxiliary Bar: Speed Chip, Sleep Timer, Queue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Chip
            AssistChip(
                onClick = { showSpeedDialog = true },
                label = {
                    Text(
                        text = "${String.format("%.2f", playbackSpeed)}x",
                        color = if (playbackSpeed != 1.0f) colors.primary else colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (playbackSpeed != 1.0f) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = colors.surfaceElevated),
                border = AssistChipDefaults.assistChipBorder(borderColor = colors.border.copy(alpha = 0.5f), enabled = true)
            )

            // Sleep Timer Chip
            AssistChip(
                onClick = { showSleepTimerDialog = true },
                label = {
                    Text(
                        text = if (sleepTimerMins > 0) "${sleepTimerMins}m" else "Timer",
                        color = if (sleepTimerMins > 0) colors.primary else colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (sleepTimerMins > 0) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = colors.surfaceElevated),
                border = AssistChipDefaults.assistChipBorder(borderColor = colors.border.copy(alpha = 0.5f), enabled = true)
            )

            // Queue Sheet Button
            IconButton(
                onClick = { showQueueSheet = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        val speeds = listOf(0.50f, 0.75f, 1.00f, 1.25f, 1.50f, 1.75f, 2.00f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", color = colors.textPrimary) },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    playbackManager.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${String.format("%.2f", speed)}x ${if (speed == 1.0f) "(Normal)" else ""}",
                                color = if (playbackSpeed == speed) colors.primary else colors.textPrimary,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                            )
                            if (playbackSpeed == speed) {
                                Icon(Icons.Default.Check, null, tint = colors.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = colors.primary)
                }
            },
            containerColor = colors.surfaceElevated
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        val timerPresets = listOf(15, 30, 45, 60, 90)
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", color = colors.textPrimary) },
            text = {
                Column {
                    if (sleepTimerMins > 0) {
                        Text(
                            text = "Timer active: $sleepTimerMins minutes remaining",
                            color = colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                playbackManager.cancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn Off Timer")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Set timer duration:", color = colors.textSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    timerPresets.forEach { mins ->
                        TextButton(
                            onClick = {
                                playbackManager.startSleepTimer(mins)
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$mins minutes", color = colors.textPrimary)
                                if (sleepTimerMins == mins) {
                                    Icon(Icons.Default.Check, null, tint = colors.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel", color = colors.primary)
                }
            },
            containerColor = colors.surfaceElevated
        )
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = colors.surfaceElevated,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.border) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Queue (${queue.size})",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = {
                        playbackManager.clearQueue()
                        showQueueSheet = false
                    }) {
                        Text("Clear All", color = colors.secondary)
                    }
                }

                HorizontalDivider(
                    color = colors.border.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Queue is empty", color = colors.textSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        itemsIndexed(queue) { idx, song ->
                            val isCurrent = idx == currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isCurrent) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                    .clickable {
                                        playbackManager.mediaController?.seekTo(idx, 0L)
                                        playbackManager.mediaController?.play()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = if (isCurrent) colors.primary else colors.textTertiary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) colors.primary else colors.textPrimary,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { playbackManager.removeFromQueue(idx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

