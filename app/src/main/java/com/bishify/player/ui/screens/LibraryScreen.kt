package com.bishify.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import com.bishify.player.local.PlaylistEntity
import com.bishify.player.model.*
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.components.SongItem
import com.bishify.player.ui.theme.*
import com.bishify.player.ui.viewmodel.MusicViewModel

enum class LibraryTab(val title: String) {
    TRACKS("Tracks"),
    DOWNLOADS("Downloads"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onNavigateToRingtone: (Song) -> Unit,
    onNavigateToEditMetadata: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentSong by viewModel.playbackManager.currentSong.collectAsState()
    val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val downloadedSongIds by viewModel.downloadedSongIds.collectAsState()

    var activeTab by remember { mutableStateOf(LibraryTab.TRACKS) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Dialogs / Sheets state
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var renamePlaylistName by remember { mutableStateOf("") }
    var selectedPlaylistForDetail by remember { mutableStateOf<PlaylistEntity?>(null) }

    var selectedAlbumForDetail by remember { mutableStateOf<Album?>(null) }
    var selectedArtistForDetail by remember { mutableStateOf<Artist?>(null) }
    var selectedFolderForDetail by remember { mutableStateOf<Folder?>(null) }

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

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COLLECTION",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Library",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Action icon (Sort or Add Playlist)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeTab == LibraryTab.TRACKS) {
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, GlassBorderSubtle, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(GlassSurfaceDark)
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                                    color = if (sortOption == option) Color.White else TextSecondary
                                                )
                                                if (sortOption == option) {
                                                    Icon(
                                                        imageVector = if (sortDirection == SortDirection.ASCENDING)
                                                            Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
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
                    } else if (activeTab == LibraryTab.PLAYLISTS) {
                        IconButton(
                            onClick = {
                                newPlaylistName = ""
                                showCreatePlaylistDialog = true
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, GlassBorderSubtle, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Playlist",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Liquid Glass Tab Selector Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LibraryTab.values()) { tab ->
                    val isSelected = activeTab == tab
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
                            .clickable { activeTab = tab }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Tab Content
            when (activeTab) {
                LibraryTab.TRACKS -> {
                    // Header row with count & Shuffle button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${songs.size} tracks",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (songs.isNotEmpty()) {
                            Text(
                                text = "Shuffle All",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { viewModel.shuffleAll(songs) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(songs, key = { idx, song -> "lib_s_${idx}_${song.id}" }) { _, song ->
                            SongItem(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                isDownloaded = downloadedSongIds.contains(song.id),
                                onDownload = if (song.isOnline) { { viewModel.downloadSong(song) } } else null,
                                onClick = { viewModel.playSong(song) },
                                onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                onAddToPlaylist = { songToAddToPlaylist = song },
                                onRingtoneClick = { onNavigateToRingtone(song) },
                                onEditMetadataClick = { onNavigateToEditMetadata(song) }
                            )
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(albums, key = { idx, album -> "lib_alb_${idx}_${album.id}" }) { _, album ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = GlassSurfaceElevated.copy(alpha = 0.65f),
                                onClick = { selectedAlbumForDetail = album }
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
                    }
                }

                LibraryTab.ARTISTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(artists, key = { idx, artist -> "lib_art_${idx}_${artist.id}" }) { _, artist ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = GlassSurfaceDark.copy(alpha = 0.6f),
                                onClick = { selectedArtistForDetail = artist }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22242B))
                                            .border(1.dp, GlassBorderSubtle, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${artist.songCount} tracks",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.FOLDERS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(folders, key = { idx, folder -> "lib_fol_${idx}_${folder.path}" }) { _, folder ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = GlassSurfaceDark.copy(alpha = 0.6f),
                                onClick = { selectedFolderForDetail = folder }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(28.dp)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${folder.songCount} tracks • ${folder.path}",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.excludeFolder(folder.path) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = "Hide folder",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.DOWNLOADS -> {
                    DownloadsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                LibraryTab.PLAYLISTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playlists, key = { idx, playlist -> "lib_pl_${idx}_${playlist.id}" }) { _, playlist ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = GlassSurfaceDark.copy(alpha = 0.6f),
                                onClick = { selectedPlaylistForDetail = playlist }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF22242B))
                                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Playlist",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            playlistToRename = playlist
                                            renamePlaylistName = playlist.name
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Rename",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deletePlaylist(playlist.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFFF4D6D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add to Playlist Dialog
        if (songToAddToPlaylist != null) {
            val song = songToAddToPlaylist!!
            AlertDialog(
                onDismissRequest = { songToAddToPlaylist = null },
                containerColor = GlassSurfaceElevated,
                title = { Text("Add to Playlist", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = song.title,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (playlists.isEmpty()) {
                            Text("No playlists yet. Create one first!", color = TextTertiary, fontSize = 13.sp)
                        } else {
                            playlists.forEach { pl ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.addSongToPlaylist(pl.id, song.id)
                                            songToAddToPlaylist = null
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp)
                                ) {
                                    Text(pl.name, color = TextPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { songToAddToPlaylist = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                containerColor = GlassSurfaceElevated,
                title = { Text("New Playlist", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist name", color = TextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = GlassBorderSubtle
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            showCreatePlaylistDialog = false
                        }
                    }) {
                        Text("Create", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Rename Playlist Dialog
        if (playlistToRename != null) {
            AlertDialog(
                onDismissRequest = { playlistToRename = null },
                containerColor = GlassSurfaceElevated,
                title = { Text("Rename Playlist", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = renamePlaylistName,
                        onValueChange = { renamePlaylistName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = GlassBorderSubtle
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (renamePlaylistName.isNotBlank()) {
                            viewModel.renamePlaylist(playlistToRename!!.id, renamePlaylistName)
                            playlistToRename = null
                        }
                    }) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { playlistToRename = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Playlist Detail Bottom Sheet
        if (selectedPlaylistForDetail != null) {
            val pl = selectedPlaylistForDetail!!
            val plSongs by viewModel.getSongsForPlaylist(pl.id).collectAsState(initial = emptyList())

            ModalBottomSheet(
                onDismissRequest = { selectedPlaylistForDetail = null },
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
                        Column {
                            Text(pl.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${plSongs.size} tracks", color = TextSecondary, fontSize = 12.sp)
                        }

                        if (plSongs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.playAll(plSongs)
                                    selectedPlaylistForDetail = null
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (plSongs.isEmpty()) {
                        Text("This playlist has no songs yet.", color = TextTertiary, fontSize = 13.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(0.6f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(plSongs, key = { idx, song -> "pl_s_${idx}_${song.id}" }) { _, song ->
                                SongItem(
                                    song = song,
                                    isPlaying = isPlaying && currentSong?.id == song.id,
                                    onClick = {
                                        viewModel.playSong(song)
                                        selectedPlaylistForDetail = null
                                    },
                                    onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                    onAddToPlaylist = {},
                                    onRingtoneClick = { onNavigateToRingtone(song) },
                                    onEditMetadataClick = { onNavigateToEditMetadata(song) }
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
                            Text(album.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${album.artist} • ${albumSongs.size} tracks", color = TextSecondary, fontSize = 12.sp)
                        }

                        if (albumSongs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.playAll(albumSongs)
                                    selectedAlbumForDetail = null
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(albumSongs, key = { idx, song -> "lib_alb_s_${idx}_${song.id}" }) { _, song ->
                            SongItem(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onClick = {
                                    viewModel.playSong(song)
                                    selectedAlbumForDetail = null
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                onAddToPlaylist = { songToAddToPlaylist = song },
                                onRingtoneClick = { onNavigateToRingtone(song) },
                                onEditMetadataClick = { onNavigateToEditMetadata(song) }
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
                            Text(artist.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${artistSongs.size} tracks", color = TextSecondary, fontSize = 12.sp)
                        }

                        if (artistSongs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.playAll(artistSongs)
                                    selectedArtistForDetail = null
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(artistSongs, key = { idx, song -> "lib_art_s_${idx}_${song.id}" }) { _, song ->
                            SongItem(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onClick = {
                                    viewModel.playSong(song)
                                    selectedArtistForDetail = null
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                onAddToPlaylist = { songToAddToPlaylist = song },
                                onRingtoneClick = { onNavigateToRingtone(song) },
                                onEditMetadataClick = { onNavigateToEditMetadata(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Folder Detail Bottom Sheet
        if (selectedFolderForDetail != null) {
            val folder = selectedFolderForDetail!!
            val folderSongs = songs.filter { it.dataPath.startsWith(folder.path) }

            ModalBottomSheet(
                onDismissRequest = { selectedFolderForDetail = null },
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
                            Text(folder.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${folderSongs.size} tracks in folder", color = TextSecondary, fontSize = 12.sp)
                        }

                        if (folderSongs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.playAll(folderSongs)
                                    selectedFolderForDetail = null
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(folderSongs, key = { idx, song -> "lib_fol_s_${idx}_${song.id}" }) { _, song ->
                            SongItem(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onClick = {
                                    viewModel.playSong(song)
                                    selectedFolderForDetail = null
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                onAddToPlaylist = { songToAddToPlaylist = song },
                                onRingtoneClick = { onNavigateToRingtone(song) },
                                onEditMetadataClick = { onNavigateToEditMetadata(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
