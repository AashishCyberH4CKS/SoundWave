package com.bishify.player.online

import com.bishify.player.model.Song

/**
 * Extensible provider interface for online music sources.
 * Allows adding additional licensed or open catalogues without rewriting the player.
 */
interface StreamingSourceProvider {
    val sourceId: String
    val displayName: String

    suspend fun searchTracks(query: String, limit: Int = 30): Result<List<Song>>
    suspend fun getTrendingTracks(limit: Int = 30): Result<List<Song>>
    suspend fun getTracksByGenre(genre: String, limit: Int = 30): Result<List<Song>>
    suspend fun resolveStream(song: Song): Result<String>
}

