package com.bishify.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.bishify.player.model.*
import com.bishify.player.ui.components.SongItem
import com.bishify.player.ui.theme.LocalSoundWaveColors
import com.bishify.player.ui.viewmodel.MusicViewModel

enum class SongsTab(val title: String) {
    SONGS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MusicViewModel,
    onNavigateToRingtone: (Song) -> Unit,
    onNavigateToEditMetadata: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundWaveColors.current
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val currentSong by viewModel.playbackManager.currentSong.collectAsState()
    val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()

    var activeTab by remember { mutableStateOf(SongsTab.SONGS) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Drilldown selection dialogs / sheets
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    // Add to playlist dialog state
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    val playlists by viewModel.playlists.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Search & Sort Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search songs, artists, albums...", color = colors.textTertiary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.textSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Menu Button
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated)
                ) {
                    Icon(
                        imageVector = if (sortDirection == SortDirection.ASCENDING) Icons.Default.Sort else Icons.Default.South,
                        contentDescription = "Sort Options",
                        tint = colors.primary
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(colors.surfaceElevated)
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                    color = if (sortOption == option) colors.primary else colors.textPrimary,
                                    fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                if (sortOption == option) {
                                    Icon(
                                        imageVector = if (sortDirection == SortDirection.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                viewModel.setSortOption(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                    color = colors.primary
                )
            },
            divider = { HorizontalDivider(color = colors.border.copy(alpha = 0.3f)) }
        ) {
            SongsTab.values().forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = {
                        Text(
                            text = tab.title,
                            color = if (activeTab == tab) colors.primary else colors.textSecondary,
                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        when (activeTab) {
            SongsTab.SONGS -> {
                // Header with Play All / Shuffle buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${songs.size} tracks",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.playAll(songs) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play All", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.shuffleAll(songs) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shuffle", fontSize = 12.sp)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(songs, key = { it.id }) { song ->
                        SongItem(
                            song = song,
                            isPlaying = isPlaying,
                            isCurrent = currentSong?.id == song.id,
                            isFavorite = song.isFavorite,
                            onClick = { viewModel.playAll(songs, songs.indexOf(song).coerceAtLeast(0)) },
                            onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                            onPlayNext = { viewModel.playbackManager.addToQueueNext(song) },
                            onAddToQueue = { viewModel.playbackManager.addToQueueEnd(song) },
                            onAddToPlaylist = { songToAddToPlaylist = song },
                            onTrimRingtone = { onNavigateToRingtone(song) },
                            onEditMetadata = { onNavigateToEditMetadata(song) }
                        )
                    }
                }
            }

            SongsTab.ALBUMS -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(albums) { album ->
                        AlbumGridItem(
                            album = album,
                            onClick = { viewModel.selectAlbum(album) }
                        )
                    }
                }
            }

            SongsTab.ARTISTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                ) {
                    items(artists) { artist ->
                        ArtistRowItem(
                            artist = artist,
                            onClick = { viewModel.selectArtist(artist) }
                        )
                    }
                }
            }

            SongsTab.FOLDERS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                ) {
                    items(folders) { folder ->
                        FolderRowItem(
                            folder = folder,
                            onClick = { viewModel.selectFolder(folder) },
                            onExclude = { viewModel.excludeFolder(folder.path) }
                        )
                    }
                }
            }
        }
    }

    // Album Drilldown Dialog
    selectedAlbum?.let { album ->
        val albumSongs = songs.filter { it.albumId == album.id }
        DrilldownModal(
            title = album.title,
            subtitle = "${album.artist} • ${albumSongs.size} tracks",
            artworkUri = album.artworkUri,
            songs = albumSongs,
            onDismiss = { viewModel.selectAlbum(null) },
            onPlaySong = { selected -> viewModel.playAll(albumSongs, albumSongs.indexOf(selected).coerceAtLeast(0)) },
            onPlayAll = { viewModel.playAll(albumSongs) }
        )
    }

    // Artist Drilldown Dialog
    selectedArtist?.let { artist ->
        val artistSongs = songs.filter { it.artist.equals(artist.name, ignoreCase = true) }
        DrilldownModal(
            title = artist.name,
            subtitle = "${artistSongs.size} tracks",
            artworkUri = artistSongs.firstOrNull()?.artworkUri,
            songs = artistSongs,
            onDismiss = { viewModel.selectArtist(null) },
            onPlaySong = { selected -> viewModel.playAll(artistSongs, artistSongs.indexOf(selected).coerceAtLeast(0)) },
            onPlayAll = { viewModel.playAll(artistSongs) }
        )
    }

    // Folder Drilldown Dialog
    selectedFolder?.let { folder ->
        val folderSongs = songs.filter { it.dataPath.startsWith(folder.path) }
        DrilldownModal(
            title = folder.name,
            subtitle = "${folderSongs.size} tracks • ${folder.path}",
            artworkUri = folderSongs.firstOrNull()?.artworkUri,
            songs = folderSongs,
            onDismiss = { viewModel.selectFolder(null) },
            onPlaySong = { selected -> viewModel.playAll(folderSongs, folderSongs.indexOf(selected).coerceAtLeast(0)) },
            onPlayAll = { viewModel.playAll(folderSongs) }
        )
    }

    // Add to Playlist Dialog
    songToAddToPlaylist?.let { song ->
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            title = { Text("Add to Playlist", color = colors.textPrimary) },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text("No playlists yet. Create one in the Playlists tab.", color = colors.textSecondary)
                    } else {
                        playlists.forEach { pl ->
                            TextButton(
                                onClick = {
                                    viewModel.addSongToPlaylist(pl.id, song.id)
                                    songToAddToPlaylist = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.QueueMusic, null, tint = colors.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(pl.name, color = colors.textPrimary, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { songToAddToPlaylist = null }) {
                    Text("Cancel", color = colors.primary)
                }
            },
            containerColor = colors.surfaceElevated
        )
    }
}

@Composable
private fun AlbumGridItem(
    album: Album,
    onClick: () -> Unit
) {
    val colors = LocalSoundWaveColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (!album.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.title,
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.artist} • ${album.songCount} tracks",
            color = colors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistRowItem(
    artist: Artist,
    onClick: () -> Unit
) {
    val colors = LocalSoundWaveColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${artist.songCount} songs • ${artist.albumCount} albums",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary
        )
    }
}

@Composable
private fun FolderRowItem(
    folder: Folder,
    onClick: () -> Unit,
    onExclude: () -> Unit
) {
    val colors = LocalSoundWaveColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFFFB703),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${folder.songCount} songs • ${folder.path}",
                color = colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onExclude) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Exclude Folder",
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrilldownModal(
    title: String,
    subtitle: String,
    artworkUri: String?,
    songs: List<Song>,
    onDismiss: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayAll: () -> Unit
) {
    val colors = LocalSoundWaveColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (!artworkUri.isNullOrBlank()) {
                        AsyncImage(
                            model = artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = colors.primary)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onPlayAll,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Play All")
                }
            }

            HorizontalDivider(
                color = colors.border.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(songs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaySong(song) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = song.title,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.formattedDuration,
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
