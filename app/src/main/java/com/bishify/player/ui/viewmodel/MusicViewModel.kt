package com.bishify.player.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bishify.player.local.*
import com.bishify.player.mediastore.MediaStoreScanner
import com.bishify.player.download.OfflineDownloadManager
import com.bishify.player.lyrics.LyricsRepository
import com.bishify.player.lyrics.SongLyrics
import com.bishify.player.model.*
import com.bishify.player.online.JioSaavnSourceProvider
import com.bishify.player.online.MusicCatalogueRepository
import com.bishify.player.online.MusicSearchRepository
import com.bishify.player.online.OnlineAlbum
import com.bishify.player.online.OnlinePlaylist
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
    val downloadManager = OfflineDownloadManager.getInstance(application)
    val activeDownloads = downloadManager.activeDownloads
    val completedDownloads = downloadManager.completedDownloads
    val downloadedSongIds = downloadManager.downloadedSongIds
    val totalDownloadStorageBytes = downloadManager.totalStorageBytes

    // Downloaded songs converted to Song models for seamless 100% offline playback
    val downloadedSongs: StateFlow<List<Song>> = downloadManager.completedDownloads.map { list ->
        list.map { entity ->
            Song(
                id = entity.songId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                durationMs = entity.durationMs,
                dataPath = entity.localFilePath,
                albumId = 0L,
                artworkUri = entity.artworkUri,
                dateAdded = entity.downloadDate,
                size = entity.fileSizeBytes,
                isOnline = false,
                sourceId = "downloaded",
                streamUrl = entity.localFilePath
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Online Music Repositories
    val searchRepository = MusicSearchRepository()
    val catalogueRepository = MusicCatalogueRepository()

    // Online StateFlows
    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    private val _onlineSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val onlineSearchResults: StateFlow<List<Song>> = _onlineSearchResults.asStateFlow()

    private val _onlineAlbums = MutableStateFlow<List<OnlineAlbum>>(emptyList())
    val onlineAlbums: StateFlow<List<OnlineAlbum>> = _onlineAlbums.asStateFlow()

    private val _onlinePlaylists = MutableStateFlow<List<OnlinePlaylist>>(emptyList())
    val onlinePlaylists: StateFlow<List<OnlinePlaylist>> = _onlinePlaylists.asStateFlow()

    private val _isOnlineSearching = MutableStateFlow(false)
    val isOnlineSearching: StateFlow<Boolean> = _isOnlineSearching.asStateFlow()

    private val _isOnlineLoading = MutableStateFlow(false)
    val isOnlineLoading: StateFlow<Boolean> = _isOnlineLoading.asStateFlow()

    private val _onlineError = MutableStateFlow<String?>(null)
    val onlineError: StateFlow<String?> = _onlineError.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _genreSongsMap = MutableStateFlow<Map<String, List<Song>>>(emptyMap())
    val genreSongsMap: StateFlow<Map<String, List<Song>>> = _genreSongsMap.asStateFlow()

    // Lyrics
    val lyricsRepository = LyricsRepository()
    private val _currentLyrics = MutableStateFlow<SongLyrics?>(null)
    val currentLyrics: StateFlow<SongLyrics?> = _currentLyrics.asStateFlow()
    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

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
        ids.distinct().mapNotNull { songMap[it] }
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
        checkNetworkStatus()
        loadTrendingSongs()

        // Observe MediaStore content observer
        viewModelScope.launch {
            scanner.observeMediaStoreChanges().collect {
                rescanLibrary()
            }
        }

        // Observe custom JioSaavn API configuration from preferences
        viewModelScope.launch {
            preferences.map { it.customJioSaavnApiUrl }.distinctUntilChanged().collect { customUrl ->
                (searchRepository.primaryProvider as? JioSaavnSourceProvider)?.setCustomBaseUrl(customUrl)
                (catalogueRepository.primaryProvider as? JioSaavnSourceProvider)?.setCustomBaseUrl(customUrl)
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

        // Auto-fetch real-time lyrics whenever active playing track changes
        viewModelScope.launch {
            playbackManager.currentSong.collect { song ->
                if (song != null) {
                    loadLyricsForSong(song)
                } else {
                    _currentLyrics.value = null
                }
            }
        }
    }

    fun loadLyricsForSong(song: Song) {
        viewModelScope.launch {
            _isLyricsLoading.value = true
            _currentLyrics.value = null
            val result = lyricsRepository.getLyrics(
                title = song.title,
                artist = song.artist,
                durationMs = song.durationMs
            )
            _currentLyrics.value = result.getOrNull()
            _isLyricsLoading.value = false
        }
    }

    fun checkNetworkStatus() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = if (network != null) cm.getNetworkCapabilities(network) else null
        val available = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isNetworkAvailable.value = available
    }

    fun loadTrendingSongs() {
        viewModelScope.launch {
            try {
                checkNetworkStatus()
                if (!_isNetworkAvailable.value) return@launch

                _isOnlineLoading.value = true
                _onlineError.value = null

                val result = catalogueRepository.getTrendingTracks(limit = 30)
                if (result.isSuccess) {
                    _trendingSongs.value = result.getOrDefault(emptyList())
                } else {
                    _onlineError.value = result.exceptionOrNull()?.localizedMessage
                }
            } catch (e: Exception) {
                _onlineError.value = e.localizedMessage
            } finally {
                _isOnlineLoading.value = false
            }
        }
    }

    fun searchOnline(query: String) {
        if (query.isBlank()) {
            _onlineSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            checkNetworkStatus()
            if (!_isNetworkAvailable.value) {
                _onlineError.value = "No internet connection. Playing offline music."
                return@launch
            }

            _isOnlineSearching.value = true
            _onlineError.value = null

            val result = searchRepository.searchOnline(query.trim(), limit = 30)
            if (result.isSuccess) {
                _onlineSearchResults.value = result.getOrDefault(emptyList())
            } else {
                _onlineError.value = result.exceptionOrNull()?.localizedMessage
            }
            _isOnlineSearching.value = false

            // Also search online albums and playlists concurrently
            searchOnlineAlbums(query)
            searchOnlinePlaylists(query)
        }
    }

    fun searchOnlineAlbums(query: String) {
        if (query.isBlank()) {
            _onlineAlbums.value = emptyList()
            return
        }
        viewModelScope.launch {
            val jio = searchRepository.primaryProvider as? JioSaavnSourceProvider
            if (jio != null) {
                val res = jio.searchAlbums(query.trim())
                _onlineAlbums.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun searchOnlinePlaylists(query: String) {
        if (query.isBlank()) {
            _onlinePlaylists.value = emptyList()
            return
        }
        viewModelScope.launch {
            val jio = searchRepository.primaryProvider as? JioSaavnSourceProvider
            if (jio != null) {
                val res = jio.searchPlaylists(query.trim())
                _onlinePlaylists.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun loadAlbumTracks(albumId: String, onLoaded: (List<Song>) -> Unit) {
        viewModelScope.launch {
            val jio = searchRepository.primaryProvider as? JioSaavnSourceProvider
            if (jio != null) {
                val res = jio.getAlbumTracks(albumId)
                onLoaded(res.getOrDefault(emptyList()))
            }
        }
    }

    fun loadPlaylistTracks(playlistId: String, onLoaded: (List<Song>) -> Unit) {
        viewModelScope.launch {
            val jio = searchRepository.primaryProvider as? JioSaavnSourceProvider
            if (jio != null) {
                val res = jio.getPlaylistTracks(playlistId)
                onLoaded(res.getOrDefault(emptyList()))
            }
        }
    }

    fun insertNext(song: Song) {
        playbackManager.insertNext(song)
    }

    fun addToQueue(song: Song) {
        playbackManager.addToQueue(song)
    }

    fun downloadSong(song: Song) {
        downloadManager.enqueueDownload(song)
    }

    fun downloadTracks(songs: List<Song>) {
        downloadManager.downloadTracks(songs)
    }

    fun pauseDownload(songId: Long) {
        downloadManager.pauseDownload(songId)
    }

    fun resumeDownload(song: Song) {
        downloadManager.resumeDownload(song)
    }

    fun cancelDownload(songId: Long) {
        downloadManager.cancelDownload(songId)
    }

    fun deleteDownloadedSong(songId: Long) {
        downloadManager.deleteDownloadedSong(songId)
    }

    fun clearAllDownloads() {
        downloadManager.clearAllDownloads()
    }

    fun loadGenreSongs(genre: String) {
        viewModelScope.launch {
            checkNetworkStatus()
            if (!_isNetworkAvailable.value) return@launch

            val result = catalogueRepository.getTracksByGenre(genre, limit = 25)
            if (result.isSuccess) {
                val current = _genreSongsMap.value.toMutableMap()
                current[genre] = result.getOrDefault(emptyList())
                _genreSongsMap.value = current
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
        if (song.isOnline) {
            playbackManager.playSingle(song)
            return
        }
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

