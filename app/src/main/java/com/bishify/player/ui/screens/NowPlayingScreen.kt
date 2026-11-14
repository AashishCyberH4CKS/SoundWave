package com.bishify.player.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.model.RepeatState
import com.bishify.player.model.Song
import com.bishify.player.ui.components.CyberpunkTurntableSkin
import com.bishify.player.ui.components.IpodClickWheelSkin
import com.bishify.player.ui.components.LyricsView
import com.bishify.player.ui.components.PlayerSkin
import com.bishify.player.ui.components.RetroCassetteSkin
import com.bishify.player.ui.components.VinylDisc
import com.bishify.player.ui.theme.*
import com.bishify.player.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
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
    val downloadedSongIds by viewModel.downloadedSongIds.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()

    val isFavorite = currentSong?.let { favoriteIds.contains(it.id) } ?: false
    val isDownloaded = currentSong?.let { downloadedSongIds.contains(it.id) } ?: false
    val activeDownload = currentSong?.let { activeDownloads[it.id] }
    val isDownloading = activeDownload?.status == com.bishify.player.download.DownloadStatus.DOWNLOADING

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showLyricsView by remember { mutableStateOf(false) }
    var showSkinMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            .background(AmoledBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Action Bar with Liquid Glass circular buttons
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, GlassBorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentSong?.album ?: "SoundWave",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Driving Mode Shortcut
                IconButton(
                    onClick = onNavigateToDrivingMode,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, GlassBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Driving Mode",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Retro Skin Switcher
                Box {
                    IconButton(
                        onClick = { showSkinMenu = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "Retro Skin",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSkinMenu,
                        onDismissRequest = { showSkinMenu = false },
                        modifier = Modifier.background(Color(0xFF18181B))
                    ) {
                        PlayerSkin.values().forEach { skin ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = skin.displayName,
                                        color = if (preferences.playerSkin == skin) Color(0xFF38BDF8) else Color.White,
                                        fontWeight = if (preferences.playerSkin == skin) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    showSkinMenu = false
                                    scope.launch {
                                        viewModel.preferencesManager.setPlayerSkin(skin)
                                    }
                                }
                            )
                        }
                    }
                }

                // Audio Effects / Equalizer Shortcut
                IconButton(
                    onClick = onNavigateToEqualizer,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, GlassBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Equalizer",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Central Area: Toggle between Vinyl Disc and Real-Time Lyrics View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.8f)
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
            AnimatedContent(
                targetState = showLyricsView,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "vinyl_lyrics_anim"
            ) { isLyrics ->
                if (isLyrics) {
                    LyricsView(
                        lyrics = currentLyrics,
                        isLoading = isLyricsLoading,
                        currentPositionMs = activePosition,
                        onSeekTo = { targetMs -> playbackManager.seekTo(targetMs) },
                        onClose = { showLyricsView = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    when (preferences.playerSkin) {
                        PlayerSkin.MODERN_GLASS -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        showLyricsView = true
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
                        }
                        PlayerSkin.IPOD_CLASSIC -> {
                            IpodClickWheelSkin(
                                song = currentSong,
                                isPlaying = isPlaying,
                                currentPositionMs = activePosition,
                                durationMs = duration,
                                onPlayPause = { playbackManager.togglePlayPause() },
                                onSkipNext = { playbackManager.skipNext() },
                                onSkipPrevious = { playbackManager.skipPrevious() },
                                onSeekTo = { playbackManager.seekTo(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        PlayerSkin.RETRO_CASSETTE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showLyricsView = true },
                                contentAlignment = Alignment.Center
                            ) {
                                RetroCassetteSkin(
                                    song = currentSong,
                                    isPlaying = isPlaying,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        PlayerSkin.CYBERPUNK -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showLyricsView = true },
                                contentAlignment = Alignment.Center
                            ) {
                                CyberpunkTurntableSkin(
                                    song = currentSong,
                                    isPlaying = isPlaying,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Song Title, Artist and Favorite Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "No track selected",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentSong?.artist ?: "Select a song from library",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // In-App Download Button (Stores in private app storage context.filesDir/downloads)
            IconButton(
                onClick = {
                    currentSong?.let { song ->
                        if (!isDownloaded && !isDownloading) {
                            viewModel.downloadSong(song)
                        }
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDownloaded) Color(0xFF10B981).copy(alpha = 0.15f)
                        else if (isDownloading) Color(0xFF38BDF8).copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.08f)
                    )
                    .border(
                        1.dp,
                        if (isDownloaded) Color(0xFF10B981).copy(alpha = 0.6f)
                        else if (isDownloading) Color(0xFF38BDF8).copy(alpha = 0.6f)
                        else GlassBorderSubtle,
                        CircleShape
                    )
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        progress = { activeDownload?.progress ?: 0f },
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Default.Download,
                        contentDescription = if (isDownloaded) "Downloaded (Private)" else "Download Offline",
                        tint = if (isDownloaded) Color(0xFF10B981) else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    currentSong?.let { viewModel.toggleFavorite(it.id) }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, GlassBorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF4D6D) else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Interactive Liquid Glass Seek Bar
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
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Song.formatDuration(activePosition),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = Song.formatDuration(duration),
                    color = TextSecondary,
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
                    tint = if (isShuffle) Color.White else TextTertiary,
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
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Central Liquid Glass Play/Pause Orb
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(16.dp, CircleShape, spotColor = Color.White.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
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
                    tint = TextPrimary,
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
                    tint = if (repeatState != RepeatState.OFF) Color.White else TextTertiary,
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
            // Speed Chip (Liquid Glass Capsule)
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(17.dp))
                    .clickable { showSpeedDialog = true }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (playbackSpeed != 1.0f) Color.White else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${String.format("%.2f", playbackSpeed)}x",
                        color = if (playbackSpeed != 1.0f) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Sleep Timer Chip (Liquid Glass Capsule)
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(17.dp))
                    .clickable { showSleepTimerDialog = true }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (sleepTimerMins > 0) Color.White else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sleepTimerMins > 0) "${sleepTimerMins}m" else "Timer",
                        color = if (sleepTimerMins > 0) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Lyrics Toggle Chip (Liquid Glass Capsule)
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (showLyricsView) Color(0xFF38BDF8).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                    .border(1.dp, if (showLyricsView) Color(0xFF38BDF8) else GlassBorderSubtle, RoundedCornerShape(17.dp))
                    .clickable { showLyricsView = !showLyricsView }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lyrics,
                        contentDescription = null,
                        tint = if (showLyricsView) Color(0xFF38BDF8) else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lyrics",
                        color = if (showLyricsView) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Queue Sheet Button (Glass Circle)
            IconButton(
                onClick = { showQueueSheet = true },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, GlassBorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue",
                    tint = Color.White,
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
            title = { Text("Playback Speed", color = TextPrimary) },
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
                                color = if (playbackSpeed == speed) Color.White else TextSecondary,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                            )
                            if (playbackSpeed == speed) {
                                Icon(Icons.Default.Check, null, tint = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = GlassSurfaceElevated
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        val timerPresets = listOf(15, 30, 45, 60, 90)
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", color = TextPrimary) },
            text = {
                Column {
                    if (sleepTimerMins > 0) {
                        Text(
                            text = "Timer active: $sleepTimerMins minutes remaining",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                playbackManager.cancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn Off Timer", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Set timer duration:", color = TextSecondary, fontSize = 13.sp)
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
                                Text("$mins minutes", color = TextPrimary)
                                if (sleepTimerMins == mins) {
                                    Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = GlassSurfaceElevated
        )
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = AmoledBackground,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
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
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = {
                        playbackManager.clearQueue()
                        showQueueSheet = false
                    }) {
                        Text("Clear All", color = TextSecondary)
                    }
                }

                HorizontalDivider(
                    color = GlassBorderSubtle,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Queue is empty", color = TextSecondary)
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
                                        if (isCurrent) Color.White.copy(alpha = 0.10f) else Color.Transparent
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
                                    color = if (isCurrent) Color.White else TextTertiary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) Color.White else TextPrimary,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = TextSecondary,
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
                                        tint = TextTertiary,
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
