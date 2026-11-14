package com.bishify.player.online

import android.util.Log
import com.bishify.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * StreamResolver handles URL resolution, validation, and provider dispatch
 * for online audio streams before delivering them to ExoPlayer.
 */
class StreamResolver(
    val providers: Map<String, StreamingSourceProvider> = mapOf(
        "jiosaavn" to JioSaavnSourceProvider(),
        "itunes" to ITunesSourceProvider(),
        "archive" to ArchiveSourceProvider()
    )
) {
    private val tag = "StreamResolver"

    suspend fun resolveStreamUrl(song: Song): Result<String> = withContext(Dispatchers.IO) {
        if (!song.isOnline) {
            // Local track uses its data path or file/content URI
            return@withContext Result.success(song.dataPath)
        }

        // If direct stream URL already exists and is valid
        if (!song.streamUrl.isNullOrBlank() && (song.streamUrl.startsWith("http://") || song.streamUrl.startsWith("https://"))) {
            return@withContext Result.success(song.streamUrl)
        }

        val provider = providers[song.sourceId] ?: providers["jiosaavn"] ?: providers["itunes"]
        if (provider != null) {
            provider.resolveStream(song)
        } else {
            Log.e(tag, "No provider registered for sourceId: ${song.sourceId}")
            Result.failure(Exception("Unknown streaming provider: ${song.sourceId}"))
        }
    }
}

