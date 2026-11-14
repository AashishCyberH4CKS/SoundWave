package com.bishify.player.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bishify.player.download.ActiveDownload
import com.bishify.player.download.DownloadStatus
import com.bishify.player.local.DownloadedSongEntity
import com.bishify.player.model.Song
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.*
import com.bishify.player.ui.viewmodel.MusicViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class DownloadSortOption(val displayName: String) {
    DATE_DOWNLOADED("Date Downloaded"),
    TITLE("Song Title"),
    ARTIST("Artist Name"),
    SIZE("File Size")
}

@Composable
fun DownloadsScreen(
    viewModel: MusicViewModel,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeDownloadsMap by viewModel.activeDownloads.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val totalStorageBytes by viewModel.totalDownloadStorageBytes.collectAsState()
    val currentSong by viewModel.playbackManager.currentSong.collectAsState()
    val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(DownloadSortOption.DATE_DOWNLOADED) }
    var showSortMenu by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val filteredSongs = remember(downloadedSongs, searchQuery, sortOption) {
        var list = if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            downloadedSongs.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
        } else {
            downloadedSongs
        }

        when (sortOption) {
            DownloadSortOption.DATE_DOWNLOADED -> list.sortedByDescending { it.dateAdded }
            DownloadSortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            DownloadSortOption.ARTIST -> list.sortedBy { it.artist.lowercase() }
            DownloadSortOption.SIZE -> list.sortedByDescending { it.size }
        }
    }

    val activeDownloadsList = remember(activeDownloadsMap) {
        activeDownloadsMap.values.toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        text = "OFFLINE STORAGE",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Downloads",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Storage pill & Clear All
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = formatStorageSize(totalStorageBytes),
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (downloadedSongs.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearAllConfirm = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear All", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar & Filter Controls
        if (downloadedSongs.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Box
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search offline downloads...", color = TextTertiary, fontSize = 13.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF141416),
                        unfocusedContainerColor = Color(0xFF141416),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF38BDF8),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                )

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141416))
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF18181B))
                    ) {
                        DownloadSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        color = if (sortOption == option) Color(0xFF38BDF8) else Color.White,
                                        fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    sortOption = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Play All & Shuffle All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.playAll(filteredSongs, 0) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All (${filteredSongs.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.shuffleAll(filteredSongs) },
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(GlassBorderSubtle)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // List Content: Active Downloads + Completed Downloads
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active Downloads Section
            if (activeDownloadsList.isNotEmpty()) {
                item {
                    Text(
                        text = "DOWNLOADING (${activeDownloadsList.size})",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                itemsIndexed(activeDownloadsList, key = { index, item -> "act_dl_${index}_${item.songId}" }) { _, download ->
                    ActiveDownloadRow(
                        download = download,
                        onPause = { viewModel.pauseDownload(download.songId) },
                        onResume = {
                            val song = Song(
                                id = download.songId,
                                title = download.title,
                                artist = download.artist,
                                album = "",
                                durationMs = 0L,
                                dataPath = "",
                                albumId = 0L,
                                artworkUri = null,
                                dateAdded = System.currentTimeMillis()
                            )
                            viewModel.resumeDownload(song)
                        },
                        onCancel = { viewModel.cancelDownload(download.songId) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Completed Downloads Section
            if (filteredSongs.isEmpty() && activeDownloadsList.isEmpty()) {
                item {
                    EmptyDownloadsView()
                }
            } else if (filteredSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "DOWNLOADED TRACKS (${filteredSongs.size})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                itemsIndexed(filteredSongs, key = { index, song -> "comp_dl_${index}_${song.id}" }) { index, song ->
                    DownloadedSongRow(
                        song = song,
                        isCurrent = currentSong?.id == song.id,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        onClick = { viewModel.playAll(filteredSongs, index) },
                        onDelete = { songToDelete = song }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (songToDelete != null) {
        val target = songToDelete!!
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            containerColor = Color(0xFF18181B),
            title = { Text("Delete Download?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete \"${target.title}\" from offline storage? The private audio file will be permanently removed.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDownloadedSong(target.id)
                        songToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            containerColor = Color(0xFF18181B),
            title = { Text("Clear All Offline Downloads?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will delete all ${downloadedSongs.size} downloaded audio files from SoundWave's private storage.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllDownloads()
                        showClearAllConfirm = false
                    }
                ) {
                    Text("Clear All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
fun ActiveDownloadRow(
    download: ActiveDownload,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color(0xFF141416)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = download.artist,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (download.status == DownloadStatus.DOWNLOADING) {
                        IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    } else if (download.status == DownloadStatus.PAUSED || download.status == DownloadStatus.FAILED) {
                        IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { download.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (download.status == DownloadStatus.FAILED) Color(0xFFEF4444) else Color(0xFF38BDF8),
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val percent = (download.progress * 100).toInt()
                val statusText = when (download.status) {
                    DownloadStatus.DOWNLOADING -> "$percent% • ${download.speedKbps} KB/s"
                    DownloadStatus.PAUSED -> "Paused ($percent%)"
                    DownloadStatus.FAILED -> download.errorMessage ?: "Failed"
                    DownloadStatus.QUEUED -> "Queued..."
                    DownloadStatus.COMPLETED -> "Finished"
                }

                Text(statusText, color = TextSecondary, fontSize = 10.sp)
                if (download.totalBytes > 0) {
                    val downloadedMb = download.bytesDownloaded / (1024f * 1024f)
                    val totalMb = download.totalBytes / (1024f * 1024f)
                    Text(String.format("%.1f / %.1f MB", downloadedMb, totalMb), color = TextTertiary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun DownloadedSongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) Color.White.copy(alpha = 0.10f) else GlassSurfaceDark.copy(alpha = 0.5f))
            .border(1.dp, if (isCurrent) Color.White.copy(alpha = 0.20f) else GlassBorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Artwork with Offline Badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E2026)),
            contentAlignment = Alignment.Center
        ) {
            if (!song.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title, Artist, Size
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) Color(0xFF38BDF8) else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.OfflinePin, contentDescription = "Offline", tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${song.artist} • ${formatStorageSize(song.size)}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EmptyDownloadsView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, GlassBorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("No Offline Downloads", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap the Download button on any online song, album, or search result to save it privately for offline playback.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    val gb = mb / 1024f
    return if (gb >= 1.0f) {
        String.format("%.2f GB", gb)
    } else {
        String.format("%.1f MB", mb)
    }
}
