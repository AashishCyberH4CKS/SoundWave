package com.bishify.player.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bishify.player.local.*
import com.bishify.player.mediastore.MediaStoreScanner
import com.bishify.player.model.*
import com.bishify.player.player.PlaybackManager
import com.bishify.player.ui.theme.ExtractedTheme
import com.bishify.player.ui.theme.PaletteManager
import com.bishify.player.util.RingtoneTrimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SoundWaveDatabase.getInstance(application)
    private val scanner = MediaStoreScanner(application)
    val preferencesManager = PreferencesManager(application)
    val playbackManager = PlaybackManager.getInstance(application)

    val preferences = preferencesManager.preferencesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SoundWavePreferences()
    )

    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())
    val rawSongs: StateFlow<List<Song>> = _rawSongs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    val favoriteIds = database.favoriteSongDao().getFavoriteSongIds().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val playlists = database.playlistDao().getAllPlaylists().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val excludedFolderPaths = database.excludedFolderDao().getExcludedFolderPaths().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recentlyPlayedIds = database.historyDao().getRecentlyPlayedSongIds(50).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val mostPlayedStats = database.historyDao().getMostPlayedSongIds(50).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val totalListeningTimeMs = database.historyDao().getTotalListeningTimeMs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0L
    )

    val totalPlays = database.historyDao().getTotalPlays().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    private val _extractedTheme = MutableStateFlow<ExtractedTheme?>(null)
    val extractedTheme: StateFlow<ExtractedTheme?> = _extractedTheme.asStateFlow()

    val songs: StateFlow<List<Song>> = combine(
        _rawSongs,
        _searchQuery,
        _sortOption,
        _sortDirection,
        favoriteIds
    ) { raw, query, sortOpt, sortDir, favs ->
        var list = raw.map { song ->
            song.copy(isFavorite = favs.contains(song.id))
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        File(it.dataPath).name.lowercase().contains(q)
            }
        }

        list = when (sortOpt) {
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> list.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> list.sortedBy { it.album.lowercase() }
            SortOption.DATE_ADDED -> list.sortedBy { it.dateAdded }
            SortOption.DURATION -> list.sortedBy { it.durationMs }
        }

        if (sortDir == SortDirection.DESCENDING) {
            list = list.reversed()
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = _rawSongs.map {
        scanner.getAlbums(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = _rawSongs.map {
        scanner.getArtists(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = _rawSongs.map {
        scanner.getFolders(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = combine(songs, favoriteIds) { allSongs, favs ->
        allSongs.filter { favs.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = combine(songs, recentlyPlayedIds) { allSongs, ids ->
        val songMap = allSongs.associateBy { it.id }
        ids.mapNotNull { songMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedSongs: StateFlow<List<Song>> = _rawSongs.map { list ->
        list.sortedByDescending { it.dateAdded }.take(20)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = combine(songs, mostPlayedStats) { allSongs, stats ->
        val songMap = allSongs.associateBy { it.id }
        stats.mapNotNull { songMap[it.songId] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        rescanLibrary()

        // Observe MediaStore content observer
        viewModelScope.launch {
            scanner.observeMediaStoreChanges().collect {
                rescanLibrary()
            }
        }

        // Dynamically extract artwork color palette when current song changes
        viewModelScope.launch {
            combine(
                playbackManager.currentSong,
                preferences
            ) { song, prefs ->
                Pair(song, prefs)
            }.collect { (song, prefs) ->
                if (prefs.dynamicArtworkTheme && song != null) {
                    val theme = PaletteManager.extractFromUri(getApplication(), song.artworkUri)
                    _extractedTheme.value = theme
                } else {
                    _extractedTheme.value = null
                }
            }
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            val excluded = database.excludedFolderDao().getExcludedFolderPathsSync().toSet()
            val list = scanner.scanSongs(excluded)
            _rawSongs.value = list
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        if (_sortOption.value == option) {
            _sortDirection.value = if (_sortDirection.value == SortDirection.ASCENDING) {
                SortDirection.DESCENDING
            } else {
                SortDirection.ASCENDING
            }
        } else {
            _sortOption.value = option
            _sortDirection.value = SortDirection.ASCENDING
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val isFav = database.favoriteSongDao().isFavoriteSync(songId)
            if (isFav) {
                database.favoriteSongDao().removeFavorite(songId)
            } else {
                database.favoriteSongDao().addFavorite(FavoriteSongEntity(songId = songId))
            }
        }
    }

    // Playlists
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            database.playlistDao().insertPlaylist(
                PlaylistEntity(name = name.trim())
            )
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val playlist = database.playlistDao().getPlaylistById(playlistId)
            if (playlist != null) {
                database.playlistDao().updatePlaylist(
                    playlist.copy(name = newName.trim(), modifiedAt = System.currentTimeMillis())
                )
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            database.playlistDao().deletePlaylistById(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            database.playlistDao().insertPlaylistSong(
                PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
            )
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            database.playlistDao().removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return database.playlistDao().getSongIdsForPlaylist(playlistId).map { ids ->
            val all = _rawSongs.value.associateBy { it.id }
            ids.mapNotNull { all[it] }
        }
    }

    fun selectAlbum(album: Album?) {
        _selectedAlbum.value = album
    }

    fun selectArtist(artist: Artist?) {
        _selectedArtist.value = artist
    }

    fun selectFolder(folder: Folder?) {
        _selectedFolder.value = folder
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }

    fun excludeFolder(folderPath: String) {
        viewModelScope.launch {
            database.excludedFolderDao().excludeFolder(ExcludedFolderEntity(folderPath))
            rescanLibrary()
        }
    }

    fun includeFolder(folderPath: String) {
        viewModelScope.launch {
            database.excludedFolderDao().includeFolder(folderPath)
            rescanLibrary()
        }
    }

    // Playback Shortcuts
    fun playSong(song: Song) {
        val currentList = songs.value.ifEmpty { _rawSongs.value }
        val index = currentList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            playbackManager.playQueue(currentList, index)
        } else {
            val combinedList = (currentList + song).distinctBy { it.id }
            val newIndex = combinedList.indexOfFirst { it.id == song.id }
            playbackManager.playQueue(combinedList, newIndex.coerceAtLeast(0))
        }
    }

    fun playAll(songList: List<Song>, startIndex: Int = 0) {
        playbackManager.playQueue(songList, startIndex)
    }

    fun shuffleAll(songList: List<Song>) {
        if (songList.isEmpty()) return
        val shuffled = songList.shuffled()
        playbackManager.playQueue(shuffled, 0)
    }

    // Ringtone Cutter
    suspend fun cutRingtone(
        sourcePath: String,
        outputName: String,
        startMs: Long,
        endMs: Long
    ): Result<android.net.Uri> {
        return RingtoneTrimmer.trimAudio(getApplication(), sourcePath, outputName, startMs, endMs)
    }
}

