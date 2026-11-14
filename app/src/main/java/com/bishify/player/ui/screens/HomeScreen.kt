package com.bishify.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bishify.player.model.Album
import com.bishify.player.model.Artist
import com.bishify.player.model.Song
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.*
import com.bishify.player.ui.viewmodel.MusicViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundWaveColors.current
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayedSongs.collectAsState()
    val recentlyAdded by viewModel.recentlyAddedSongs.collectAsState()
    val favorites by viewModel.favoriteSongs.collectAsState()
    val totalTimeMs by viewModel.totalListeningTimeMs.collectAsState()
    val downloadedSongIds by viewModel.downloadedSongIds.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val isOnlineLoading by viewModel.isOnlineLoading.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()

    val greeting = remember { getGreetingMessage() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top Header Bar
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting.uppercase(),
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SoundWave",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Action shortcuts (Equalizer, Settings)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Equalizer Glass Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceDark.copy(alpha = 0.8f))
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                            .clickable { onNavigateToEqualizer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Equalizer",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings Glass Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceDark.copy(alpha = 0.8f))
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Trending Online Top Hits (when connected)
        if (isNetworkAvailable) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔥 Trending Hits",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                    }

                    if (trendingSongs.isNotEmpty()) {
                        Text(
                            text = "Play All",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                viewModel.playAll(trendingSongs, 0)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isOnlineLoading && trendingSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else if (trendingSongs.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        itemsIndexed(trendingSongs, key = { index, s -> "trend_${index}_${s.id}" }) { index, song ->
                            val isDownloaded = downloadedSongIds.contains(song.id)
                            val isDownloading = activeDownloads[song.id]?.status == com.bishify.player.download.DownloadStatus.DOWNLOADING
                            OnlineTrendingCard(
                                song = song,
                                isDownloaded = isDownloaded,
                                isDownloading = isDownloading,
                                onDownloadClick = {
                                    if (!isDownloaded && !isDownloading) {
                                        viewModel.downloadSong(song)
                                    }
                                },
                                onClick = {
                                    viewModel.playAll(trendingSongs, index)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(26.dp))
            }
        }

        // Library Stats Overview Glass Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = GlassSurfaceElevated.copy(alpha = 0.7f),
                borderWidth = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatMetric(label = "TRACKS", value = "${songs.size}")
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                    StatMetric(label = "DOWNLOADS", value = "${downloadedSongIds.size}")
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                    StatMetric(label = "LISTENED", value = formatListeningTime(totalTimeMs ?: 0L))
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Quick Action Glass Pills (Shuffle Local, Play Favorites)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shuffle All Glass Button
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    onClick = {
                        if (songs.isNotEmpty()) viewModel.shuffleAll(songs)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shuffle Local",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Favorites Mix Glass Button
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    onClick = {
                        if (favorites.isNotEmpty()) {
                            viewModel.playAll(favorites)
                        } else {
                            onNavigateToLibrary()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF4D6D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Favorites (${favorites.size})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Recently Played (Offline/Local)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Played")
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    itemsIndexed(recentlyPlayed.take(12), key = { index, song -> "recent_${index}_${song.id}" }) { _, song ->
                        GlassSongCard(
                            song = song,
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Section: Albums & Artists Horizontal Carousel
        if (albums.isNotEmpty() || artists.isNotEmpty()) {
            item {
                SectionHeader(title = "Local Albums & Artists")
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    itemsIndexed(albums.take(8), key = { index, album -> "album_${index}_${album.id}" }) { _, album ->
                        GlassAlbumCard(
                            album = album,
                            onClick = {
                                val albumSongs = songs.filter { it.albumId == album.id }
                                if (albumSongs.isNotEmpty()) {
                                    viewModel.playAll(albumSongs)
                                }
                            }
                        )
                    }
                    itemsIndexed(artists.take(8), key = { index, artist -> "artist_${index}_${artist.id}" }) { _, artist ->
                        GlassArtistCard(
                            artist = artist,
                            onClick = {
                                val artistSongs = songs.filter { it.artist.equals(artist.name, ignoreCase = true) }
                                if (artistSongs.isNotEmpty()) {
                                    viewModel.playAll(artistSongs)
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Section: Quick Tracks / All Songs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Local Tracks")
                Text(
                    text = "View Library",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onNavigateToLibrary() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (songs.isNotEmpty()) {
            val previewTracks = songs.take(8)
            itemsIndexed(previewTracks, key = { index, song -> "track_${index}_${song.id}" }) { _, song ->
                GlassSongRow(
                    song = song,
                    onClick = { viewModel.playSong(song) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            item {
                EmptyLibraryPrompt(onRescan = { viewModel.rescanLibrary() })
            }
        }

        // Section: Playlists
        if (playlists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(title = "Your Playlists")
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(playlists, key = { index, playlist -> "pl_${index}_${playlist.id}" }) { _, playlist ->
                        GlassCard(
                            modifier = Modifier
                                .width(150.dp)
                                .height(90.dp),
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = GlassSurfaceElevated.copy(alpha = 0.7f),
                            onClick = onNavigateToPlaylists
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = playlist.name,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Custom Playlist",
                                        color = TextSecondary,
                                        fontSize = 11.sp
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

@Composable
private fun OnlineTrendingCard(
    song: Song,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    onDownloadClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = GlassSurfaceElevated.copy(alpha = 0.75f),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF181A20))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!song.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song.artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // In-App Download Glass Capsule / Badge
                if (onDownloadClick != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(1.dp, if (isDownloaded) Color(0xFF10B981) else GlassBorderSubtle, CircleShape)
                            .clickable { onDownloadClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                contentDescription = if (isDownloaded) "Downloaded (Private)" else "Download",
                                tint = if (isDownloaded) Color(0xFF10B981) else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    )
}

@Composable
private fun StatMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun GlassSongCard(
    song: Song,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.width(136.dp),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = GlassSurfaceElevated.copy(alpha = 0.65f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF181A20))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!song.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song.artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GlassAlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.width(136.dp),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = GlassSurfaceElevated.copy(alpha = 0.65f),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF181A20))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!album.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = album.title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.songCount} songs",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun GlassArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = GlassSurfaceElevated.copy(alpha = 0.65f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22242B))
                    .border(1.dp, GlassBorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = artist.name,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artist.songCount} tracks",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun GlassSongRow(
    song: Song,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = GlassSurfaceDark.copy(alpha = 0.55f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatDuration(song.durationMs),
                color = TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyLibraryPrompt(onRescan: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = GlassSurfaceElevated.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No songs found on your device",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ensure music files (.mp3, .flac, .m4a) are in your storage and permissions are granted.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRescan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Storage")
            }
        }
    }
}

private fun getGreetingMessage(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Night Grooves"
    }
}

private fun formatListeningTime(timeMs: Long): String {
    val minutes = timeMs / 1000 / 60
    val hours = minutes / 60
    return if (hours > 0) "${hours}h ${minutes % 60}m" else "${minutes}m"
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
