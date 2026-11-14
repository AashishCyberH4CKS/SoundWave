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
import com.bishify.player.model.Song
import com.bishify.player.online.OnlineAlbum
import com.bishify.player.online.OnlinePlaylist
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.*
import com.bishify.player.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

enum class SearchMode(val title: String) {
    ONLINE("Online"),
    LOCAL("Local Library")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val localSongs by viewModel.songs.collectAsState()
    val rawSongs by viewModel.rawSongs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()

    val onlineResults by viewModel.onlineSearchResults.collectAsState()
    val onlineAlbums by viewModel.onlineAlbums.collectAsState()
    val onlinePlaylists by viewModel.onlinePlaylists.collectAsState()
    val downloadedSongIds by viewModel.downloadedSongIds.collectAsState()
    val isOnlineSearching by viewModel.isOnlineSearching.collectAsState()
    val onlineError by viewModel.onlineError.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()

    var activeSearchMode by remember { mutableStateOf(SearchMode.ONLINE) }
    var selectedOnlineAlbum by remember { mutableStateOf<OnlineAlbum?>(null) }
    var selectedOnlinePlaylist by remember { mutableStateOf<OnlinePlaylist?>(null) }
    var detailTracks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoadingDetailTracks by remember { mutableStateOf(false) }

    // Debounced online search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            delay(400) // 400ms debounce
            viewModel.searchOnline(searchQuery)
        }
    }

    // Filter matching local albums and artists
    val matchingAlbums = remember(searchQuery, albums) {
        if (searchQuery.isBlank()) emptyList()
        else albums.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
    }

    val matchingArtists = remember(searchQuery, artists) {
        if (searchQuery.isBlank()) emptyList()
        else artists.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Liquid Glass Search Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, GlassBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Translucent Search Text Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassSurfaceDark.copy(alpha = 0.8f))
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = if (activeSearchMode == SearchMode.ONLINE) "Search millions of songs online..."
                                    else "Search local songs, artists, albums...",
                                    color = TextTertiary,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector: Online Streaming vs Local Storage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchMode.values().forEach { mode ->
                    val isSelected = activeSearchMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) GlassBorderHighlight else GlassBorderSubtle,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                activeSearchMode = mode
                                if (mode == SearchMode.ONLINE && searchQuery.isNotBlank()) {
                                    viewModel.searchOnline(searchQuery)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (mode == SearchMode.ONLINE) Icons.Outlined.Cloud else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mode.title,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Results Content
            if (searchQuery.isBlank()) {
                // Empty search state with quick discovery suggestions
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, GlassBorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeSearchMode == SearchMode.ONLINE) Icons.Outlined.Cloud else Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (activeSearchMode == SearchMode.ONLINE) "Search Songs Online" else "Search Local Storage",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (activeSearchMode == SearchMode.ONLINE) "Discover songs, artists, and albums"
                        else "Find tracks, albums, artists and folders on your device",
                        color = TextTertiary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "POPULAR SEARCHES",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val suggestions = listOf(
                        "Taylor Swift", "Eminem", "The Weeknd", "Coldplay",
                        "Daft Punk", "Hans Zimmer", "Billie Eilish", "Lofi Beats"
                    )

                    // Suggestion Chips Grid
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions.take(4)) { term ->
                            SearchSuggestionChip(
                                label = term,
                                onClick = {
                                    viewModel.setSearchQuery(term)
                                    if (activeSearchMode == SearchMode.ONLINE) {
                                        viewModel.searchOnline(term)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions.drop(4)) { term ->
                            SearchSuggestionChip(
                                label = term,
                                onClick = {
                                    viewModel.setSearchQuery(term)
                                    if (activeSearchMode == SearchMode.ONLINE) {
                                        viewModel.searchOnline(term)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Display Search Results
                when (activeSearchMode) {
                    SearchMode.ONLINE -> {
                        if (isOnlineSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Searching online music...",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else if (!isNetworkAvailable) {
                            // Offline alert
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = GlassSurfaceElevated.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("No Internet Connection", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Switch to Local Library mode to listen offline.", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else if (onlineResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No online tracks found for \"$searchQuery\"",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Online Albums Carousel
                                if (onlineAlbums.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Albums (${onlineAlbums.size})",
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            itemsIndexed(onlineAlbums, key = { idx, album -> "online_alb_${idx}_${album.id}" }) { _, album ->
                                                GlassCard(
                                                    modifier = Modifier.width(120.dp),
                                                    shape = RoundedCornerShape(14.dp),
                                                    backgroundColor = GlassSurfaceElevated.copy(alpha = 0.6f),
                                                    onClick = {
                                                        selectedOnlineAlbum = album
                                                        isLoadingDetailTracks = true
                                                        viewModel.loadAlbumTracks(album.id) { tracks ->
                                                            detailTracks = tracks
                                                            isLoadingDetailTracks = false
                                                        }
                                                    }
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(104.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(Color(0xFF22242B))
                                                        ) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(album.artworkUri)
                                                                    .crossfade(true)
                                                                    .build(),
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(album.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text(album.artist, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                }

                                // Online Playlists Carousel
                                if (onlinePlaylists.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Playlists (${onlinePlaylists.size})",
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            itemsIndexed(onlinePlaylists, key = { idx, playlist -> "online_ply_${idx}_${playlist.id}" }) { _, playlist ->
                                                GlassCard(
                                                    modifier = Modifier.width(120.dp),
                                                    shape = RoundedCornerShape(14.dp),
                                                    backgroundColor = GlassSurfaceElevated.copy(alpha = 0.6f),
                                                    onClick = {
                                                        selectedOnlinePlaylist = playlist
                                                        isLoadingDetailTracks = true
                                                        viewModel.loadPlaylistTracks(playlist.id) { tracks ->
                                                            detailTracks = tracks
                                                            isLoadingDetailTracks = false
                                                        }
                                                    }
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(104.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(Color(0xFF22242B))
                                                        ) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(playlist.artworkUri)
                                                                    .crossfade(true)
                                                                    .build(),
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(playlist.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text(playlist.subtitle, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Songs (${onlineResults.size})",
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                itemsIndexed(onlineResults, key = { index, s -> "online_${index}_${s.id}" }) { index, song ->
                                    val isDownloaded = downloadedSongIds.contains(song.id)
                                    OnlineSongSearchRow(
                                        song = song,
                                        isDownloaded = isDownloaded,
                                        onClick = {
                                            viewModel.playAll(onlineResults, index)
                                        },
                                        onDownloadClick = {
                                            viewModel.downloadSong(song)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    SearchMode.LOCAL -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Matching Artists
                            if (matchingArtists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Artists (${matchingArtists.size})",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        itemsIndexed(matchingArtists, key = { idx, artist -> "srch_art_${idx}_${artist.id}" }) { _, artist ->
                                            GlassCard(
                                                modifier = Modifier.width(110.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                backgroundColor = GlassSurfaceElevated.copy(alpha = 0.6f),
                                                onClick = {
                                                    val artistSongs = rawSongs.filter { it.artist.equals(artist.name, ignoreCase = true) }
                                                    if (artistSongs.isNotEmpty()) {
                                                        viewModel.playAll(artistSongs)
                                                        onBackClick()
                                                    }
                                                }
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF22242B)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(artist.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Matching Albums
                            if (matchingAlbums.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Albums (${matchingAlbums.size})",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        itemsIndexed(matchingAlbums, key = { idx, album -> "srch_alb_${idx}_${album.id}" }) { _, album ->
                                            GlassCard(
                                                modifier = Modifier.width(120.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                backgroundColor = GlassSurfaceElevated.copy(alpha = 0.6f),
                                                onClick = {
                                                    val albumSongs = rawSongs.filter { it.albumId == album.id }
                                                    if (albumSongs.isNotEmpty()) {
                                                        viewModel.playAll(albumSongs)
                                                        onBackClick()
                                                    }
                                                }
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Box(
                                                        modifier = Modifier.size(104.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF181A20)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (!album.artworkUri.isNullOrBlank()) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current).data(album.artworkUri).crossfade(true).build(),
                                                                contentDescription = album.title,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Icon(Icons.Default.Album, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(album.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(album.artist, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Matching Local Tracks
                            item {
                                Text(
                                    text = "Local Tracks (${localSongs.size})",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            if (localSongs.isEmpty() && matchingAlbums.isEmpty() && matchingArtists.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No local songs found for \"$searchQuery\"", color = TextSecondary, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                itemsIndexed(localSongs, key = { idx, song -> "srch_s_${idx}_${song.id}" }) { _, song ->
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        backgroundColor = GlassSurfaceDark.copy(alpha = 0.55f),
                                        onClick = {
                                            viewModel.playSong(song)
                                            onBackClick()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E2026)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!song.artworkUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current).data(song.artworkUri).crossfade(true).build(),
                                                        contentDescription = song.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("${song.artist} • ${song.album}", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }

                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal Sheet for Online Album Tracks
        if (selectedOnlineAlbum != null) {
            val album = selectedOnlineAlbum!!
            ModalBottomSheet(
                onDismissRequest = { selectedOnlineAlbum = null },
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(album.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(album.artist, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        if (detailTracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { viewModel.downloadTracks(detailTracks) },
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Download All", tint = Color.White, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.playAll(detailTracks, 0)
                                        selectedOnlineAlbum = null
                                    },
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF38BDF8).copy(alpha = 0.25f))
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoadingDetailTracks) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(detailTracks) { index, song ->
                                OnlineSongSearchRow(
                                    song = song,
                                    isDownloaded = downloadedSongIds.contains(song.id),
                                    onClick = {
                                        viewModel.playAll(detailTracks, index)
                                        selectedOnlineAlbum = null
                                    },
                                    onDownloadClick = { viewModel.downloadSong(song) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal Sheet for Online Playlist Tracks
        if (selectedOnlinePlaylist != null) {
            val playlist = selectedOnlinePlaylist!!
            ModalBottomSheet(
                onDismissRequest = { selectedOnlinePlaylist = null },
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(playlist.subtitle, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        if (detailTracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { viewModel.downloadTracks(detailTracks) },
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Download All", tint = Color.White, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.playAll(detailTracks, 0)
                                        selectedOnlinePlaylist = null
                                    },
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF38BDF8).copy(alpha = 0.25f))
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoadingDetailTracks) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(detailTracks) { index, song ->
                                OnlineSongSearchRow(
                                    song = song,
                                    isDownloaded = downloadedSongIds.contains(song.id),
                                    onClick = {
                                        viewModel.playAll(detailTracks, index)
                                        selectedOnlinePlaylist = null
                                    },
                                    onDownloadClick = { viewModel.downloadSong(song) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineSongSearchRow(
    song: Song,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = GlassSurfaceDark.copy(alpha = 0.65f),
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2026))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp)),
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
                        modifier = Modifier.size(22.dp)
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

            // Download Button
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = if (isDownloaded) "Downloaded" else "Download Offline",
                    tint = if (isDownloaded) Color(0xFF10B981) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestionChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
