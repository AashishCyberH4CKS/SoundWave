package com.bishify.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bishify.player.model.Song
import com.bishify.player.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    isFavorite: Boolean = song.isFavorite,
    onFavoriteToggle: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onRingtoneClick: () -> Unit = {},
    onTrimRingtone: () -> Unit = onRingtoneClick,
    onEditMetadataClick: () -> Unit = {},
    onEditMetadata: () -> Unit = onEditMetadataClick,
    onDownload: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "song_item_swipe"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Background swipe action indicators
        if (swipeOffset > 10f) {
            // Swiping right: Play Next (Emerald Green)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF10B981).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlaylistPlay, contentDescription = "Play Next", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Next", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else if (swipeOffset < -10f) {
            // Swiping left: Add to Queue (Sky Blue)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF38BDF8).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Queue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = Color.White)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeOffset = 0f },
                        onDragEnd = {
                            if (swipeOffset > 70f) {
                                onPlayNext()
                            } else if (swipeOffset < -70f) {
                                onAddToQueue()
                            }
                            swipeOffset = 0f
                        },
                        onDragCancel = { swipeOffset = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffset = (swipeOffset + dragAmount).coerceIn(-120f, 120f)
                        }
                    )
                }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isCurrent) Color.White.copy(alpha = 0.12f)
                    else GlassSurfaceDark.copy(alpha = 0.5f)
                )
                .border(
                    1.dp,
                    if (isCurrent) Color.White.copy(alpha = 0.22f) else GlassBorderSubtle,
                    RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Thumbnail Artwork
        Box(
            modifier = Modifier
                .size(46.dp)
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
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = if (isCurrent) Color.White else TextPrimary,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Offline Available",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(11.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${song.artist} • ${song.formattedDuration}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Quick Favorite Icon
        IconButton(
            onClick = onFavoriteToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color(0xFFFF4D6D) else TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        // More Options Dropdown
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(GlassSurfaceElevated)
            ) {
                if (onDownload != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isDownloaded) "Downloaded (Private)" else if (isDownloading) "Downloading..." else "Download Offline",
                                color = if (isDownloaded) Color(0xFF10B981) else TextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                null,
                                tint = if (isDownloaded) Color(0xFF10B981) else TextSecondary
                            )
                        },
                        onClick = {
                            showMenu = false
                            if (!isDownloaded && !isDownloading) {
                                onDownload()
                            }
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Play Next", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Default.PlaylistPlay, null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        onPlayNext()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, null, tint = TextSecondary) },
                    onClick = {
                        showMenu = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = TextSecondary) },
                    onClick = {
                        showMenu = false
                        onAddToPlaylist()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Make Ringtone", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Default.ContentCut, null, tint = TextSecondary) },
                    onClick = {
                        showMenu = false
                        onTrimRingtone()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit Metadata", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = TextSecondary) },
                    onClick = {
                        showMenu = false
                        onEditMetadata()
                    }
                )
            }
        }
    }
}
}
