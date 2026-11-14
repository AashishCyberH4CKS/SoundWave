package com.bishify.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.bishify.player.online.MusicGenreChannel
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.*
import com.bishify.player.ui.viewmodel.MusicViewModel

enum class ExploreFilter(val title: String) {
    ALL("All"),
    GENRES("Online Genres"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    RECENT("Recently Added")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val recentlyAdded by viewModel.recentlyAddedSongs.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val genreMap by viewModel.genreSongsMap.collectAsState()
    val downloadedSongIds by viewModel.downloadedSongIds.collectAsState()

    var selectedFilter by remember { mutableStateOf(ExploreFilter.ALL) }
    var selectedAlbumForDetail by remember { mutableStateOf<Album?>(null) }
    var selectedArtistForDetail by remember { mutableStateOf<Artist?>(null) }
    var selectedGenreForDetail by remember { mutableStateOf<MusicGenreChannel?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DISCOVER",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Explore",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Quick Shuffle Button
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(19.dp))
                        .clickable { if (songs.isNotEmpty()) viewModel.shuffleAll(songs) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mix",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs (Glass Pills)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ExploreFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) GlassBorderHighlight else GlassBorderSubtle,
                                RoundedCornerShape(17.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selected filter
            when (selectedFilter) {
                ExploreFilter.ALL -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        // Online Genres Preview
                        item {
                            Text(
                                text = "Online Music Genres",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(viewModel.catalogueRepository.supportedGenres) { genre ->
                                    GenreCapsuleCard(
                                        genre = genre,
                                        onClick = {
                                            selectedGenreForDetail = genre
                                            viewModel.loadGenreSongs(genre.name)
                                        }
                                    )
                                }
                            }
                        }

                        // Featured Albums
                        item {
                            Text(
                                text = "Featured Albums",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(albums.take(10), key = { idx, album -> "exp_album_${idx}_${album.id}" }) { _, album ->
                                    ExploreAlbumCard(
                                        album = album,
                                        onClick = { selectedAlbumForDetail = album }
                                    )
                                }
                            }
                        }

                        // Top Artists
                        item {
                            Text(
                                text = "Popular Artists",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(artists.take(10), key = { idx, artist -> "exp_art_${idx}_${artist.id}" }) { _, artist ->
                                    ExploreArtistCard(
                                        artist = artist,
                                        onClick = { selectedArtistForDetail = artist }
                                    )
                                }
                            }
                        }

                        // Recently Added Tracks
                        item {
                            Text(
                                text = "Fresh Local Finds",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        itemsIndexed(recentlyAdded.take(8), key = { idx, song -> "exp_rec_${idx}_${song.id}" }) { _, song ->
                            ExploreSongRow(
                                song = song,
                                onClick = { viewModel.playSong(song) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                ExploreFilter.GENRES -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.catalogueRepository.supportedGenres) { genre ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = GlassSurfaceElevated.copy(alpha = 0.65f),
                                onClick = {
                                    selectedGenreForDetail = genre
                                    viewModel.loadGenreSongs(genre.name)
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = genre.iconEmoji,
                                        fontSize = 28.sp
                                    )
                                    Column {
                                        Text(
                                            text = genre.name,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Popular Hits",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ExploreFilter.ALBUMS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(albums, key = { "grid_album_${it.id}" }) { album ->
                            ExploreAlbumCard(
                                album = album,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedAlbumForDetail = album }
                            )
                        }
                    }
                }

                ExploreFilter.ARTISTS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(artists, key = { "grid_artist_${it.id}" }) { artist ->
                            ExploreArtistCard(
                                artist = artist,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedArtistForDetail = artist }
                            )
                        }
                    }
                }

                ExploreFilter.RECENT -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentlyAdded.size, key = { idx -> "all_rec_${idx}_${recentlyAdded[idx].id}" }) { idx ->
                            val song = recentlyAdded[idx]
                            ExploreSongRow(
                                song = song,
                                onClick = { viewModel.playSong(song) }
                            )
                        }
                    }
                }
            }
        }

        // Online Genre Detail Bottom Sheet
        if (selectedGenreForDetail != null) {
            val genre = selectedGenreForDetail!!
            val genreTracks = genreMap[genre.name] ?: emptyList()

            ModalBottomSheet(
                onDismissRequest = { selectedGenreForDetail = null },
                containerColor = AmoledBackground,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = genre.iconEmoji, fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = genre.name,
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = genre.description,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (genreTracks.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.playAll(genreTracks, 0)
                                    selectedGenreForDetail = null
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.25f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (genreTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(0.6f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(genreTracks) { idx, song ->
                                ExploreSongRow(
                                    song = song,
                                    isDownloaded = downloadedSongIds.contains(song.id),
                                    onDownload = { viewModel.downloadSong(song) },
                                    onClick = {
                                        viewModel.playAll(genreTracks, idx)
                                        selectedGenreForDetail = null
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Album Detail Bottom Sheet
        if (selectedAlbumForDetail != null) {
            val album = selectedAlbumForDetail!!
            val albumSongs = songs.filter { it.albumId == album.id }

            ModalBottomSheet(
                onDismissRequest = { selectedAlbumForDetail = null },
                containerColor = AmoledBackground,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF181A20))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
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
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.title,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} • ${albumSongs.size} tracks",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // Play All Button
                        IconButton(
                            onClick = {
                                if (albumSongs.isNotEmpty()) {
                                    viewModel.playAll(albumSongs)
                                    selectedAlbumForDetail = null
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(albumSongs, key = { idx, song -> "alb_song_${idx}_${song.id}" }) { _, song ->
                            ExploreSongRow(
                                song = song,
                                onClick = {
                                    viewModel.playSong(song)
                                    selectedAlbumForDetail = null
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Artist Detail Bottom Sheet
        if (selectedArtistForDetail != null) {
            val artist = selectedArtistForDetail!!
            val artistSongs = songs.filter { it.artist.equals(artist.name, ignoreCase = true) }

            ModalBottomSheet(
                onDismissRequest = { selectedArtistForDetail = null },
                containerColor = AmoledBackground,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22242B))
                                .border(1.dp, GlassBorderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist.name,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${artistSongs.size} tracks available",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                if (artistSongs.isNotEmpty()) {
                                    viewModel.playAll(artistSongs)
                                    selectedArtistForDetail = null
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(artistSongs, key = { idx, song -> "art_song_${idx}_${song.id}" }) { _, song ->
                            ExploreSongRow(
                                song = song,
                                onClick = {
                                    viewModel.playSong(song)
                                    selectedArtistForDetail = null
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun GenreCapsuleCard(
    genre: MusicGenreChannel,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(23.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(genre.iconEmoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(genre.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExploreAlbumCard(
    album: Album,
    modifier: Modifier = Modifier.width(140.dp),
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = GlassSurfaceElevated.copy(alpha = 0.65f),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
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
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.artist} • ${album.songCount} songs",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExploreArtistCard(
    artist: Artist,
    modifier: Modifier = Modifier.width(115.dp),
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
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
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22242B))
                    .border(1.dp, GlassBorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(30.dp)
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
private fun ExploreSongRow(
    song: Song,
    isDownloaded: Boolean = false,
    onDownload: (() -> Unit)? = null,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.isOnline) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(text = "ONLINE", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Offline Available",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = "${song.artist} • ${song.album}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onDownload != null) {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = if (isDownloaded) "Downloaded" else "Download Offline",
                        tint = if (isDownloaded) Color(0xFF10B981) else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
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

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
