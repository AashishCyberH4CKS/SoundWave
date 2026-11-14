package com.bishify.player.online

import com.bishify.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UnifiedSearchResult(
    val onlineTracks: List<Song> = emptyList(),
    val localTracks: List<Song> = emptyList(),
    val isOnlineSuccess: Boolean = true,
    val errorMessage: String? = null
)

/**
 * MusicSearchRepository coordinates searching across online catalogues
 * and local offline device storage with suggestions and query history.
 */
class MusicSearchRepository(
    val primaryProvider: StreamingSourceProvider = JioSaavnSourceProvider(),
    val secondaryProvider: StreamingSourceProvider = ITunesSourceProvider()
) {
    private val recentSearches = mutableListOf<String>()

    val defaultSuggestions = listOf(
        "Trending Hits",
        "Taylor Swift",
        "Eminem",
        "The Weeknd",
        "Daft Punk",
        "Hans Zimmer",
        "Coldplay",
        "Lofi Hip Hop",
        "Acoustic Guitar",
        "Synthwave"
    )

    suspend fun searchOnline(query: String, limit: Int = 30): Result<List<Song>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())

        // Record search in recent searches
        synchronized(recentSearches) {
            recentSearches.remove(query)
            recentSearches.add(0, query)
            if (recentSearches.size > 20) recentSearches.removeAt(recentSearches.lastIndex)
        }

        // Search primary provider (iTunes/Apple Music catalog)
        val result = primaryProvider.searchTracks(query, limit)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            return@withContext result
        }

        // Fallback to open archive provider
        val fallback = secondaryProvider.searchTracks(query, limit)
        if (fallback.isSuccess && fallback.getOrNull()?.isNotEmpty() == true) {
            return@withContext fallback
        }

        result
    }

    suspend fun searchUnified(query: String, localSongs: List<Song>): UnifiedSearchResult = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext UnifiedSearchResult()
        }

        val q = query.trim().lowercase()
        val localMatches = localSongs.filter {
            it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
        }

        val onlineResult = searchOnline(query, limit = 30)

        UnifiedSearchResult(
            onlineTracks = onlineResult.getOrDefault(emptyList()),
            localTracks = localMatches,
            isOnlineSuccess = onlineResult.isSuccess,
            errorMessage = onlineResult.exceptionOrNull()?.localizedMessage
        )
    }

    fun getRecentSearches(): List<String> = synchronized(recentSearches) {
        recentSearches.toList()
    }

    fun clearRecentSearches() = synchronized(recentSearches) {
        recentSearches.clear()
    }
}

