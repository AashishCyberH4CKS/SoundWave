package com.bishify.player.online

import android.util.Log
import com.bishify.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Licensed iTunes / Apple Music online catalogue provider.
 * Delivers real global mainstream music metadata, high-resolution album artwork,
 * top global trending charts, and official audio streaming previews without API keys.
 */
class ITunesSourceProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : StreamingSourceProvider {

    private val tag = "ITunesSourceProvider"

    override val sourceId: String = "itunes"
    override val displayName: String = "Apple Music / iTunes"

    override suspend fun searchTracks(query: String, limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())

            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SoundWave-Android/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext Result.success(emptyList())

                val songs = mutableListOf<Song>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val previewUrl = item.optString("previewUrl")
                    if (previewUrl.isNullOrBlank()) continue // Ensure track has a playable stream

                    val trackId = item.optLong("trackId", (i + 1).toLong())
                    val title = item.optString("trackName", "Unknown Track")
                    val artist = item.optString("artistName", "Unknown Artist")
                    val album = item.optString("collectionName", "Single")
                    val durationMs = item.optLong("trackTimeMillis", 30000L)
                    val rawArtwork = item.optString("artworkUrl100")
                    val hdArtwork = rawArtwork.replace("100x100bb", "600x600bb")
                    val genre = item.optString("primaryGenreName", "Music")

                    // Generate a stable positive Long id for Room and playback
                    val stableId = abs(("itunes_${trackId}").hashCode().toLong())

                    songs.add(
                        Song(
                            id = stableId,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            dataPath = previewUrl,
                            albumId = item.optLong("collectionId", 0L),
                            artworkUri = hdArtwork,
                            dateAdded = System.currentTimeMillis(),
                            isOnline = true,
                            streamUrl = previewUrl,
                            sourceId = sourceId,
                            onlineTrackId = trackId.toString(),
                            genre = genre
                        )
                    )
                }

                Result.success(songs)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error searching tracks for query '$query': ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getTrendingTracks(limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://itunes.apple.com/us/rss/topsongs/limit=$limit/json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SoundWave-Android/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val json = JSONObject(body)
                val feed = json.optJSONObject("feed") ?: return@withContext Result.success(emptyList())

                val entries = feed.optJSONArray("entry")
                val songs = mutableListOf<Song>()

                if (entries != null) {
                    for (i in 0 until entries.length()) {
                        val entry = entries.getJSONObject(i)
                        val title = entry.optJSONObject("im:name")?.optString("label") ?: "Top Track"
                        val artist = entry.optJSONObject("im:artist")?.optString("label") ?: "Unknown Artist"
                        val collectionObj = entry.optJSONObject("im:collection")
                        val album = collectionObj?.optJSONObject("im:name")?.optString("label") ?: "Trending"

                        // Extract high resolution cover image
                        val images = entry.optJSONArray("im:image")
                        val rawImage = if (images != null && images.length() > 0) {
                            images.getJSONObject(images.length() - 1).optString("label")
                        } else null
                        val hdImage = rawImage?.replace("170x170bb", "600x600bb")

                        // Extract audio preview enclosure stream
                        var streamUrl: String? = null
                        val links = entry.optJSONArray("link")
                        if (links != null) {
                            for (j in 0 until links.length()) {
                                val linkObj = links.getJSONObject(j)
                                val attrs = linkObj.optJSONObject("attributes")
                                if (attrs != null && (attrs.optString("im:assetType") == "preview" || attrs.optString("rel") == "enclosure")) {
                                    streamUrl = attrs.optString("href")
                                    break
                                }
                            }
                        }

                        if (streamUrl.isNullOrBlank()) continue

                        val idObj = entry.optJSONObject("id")
                        val onlineId = idObj?.optJSONObject("attributes")?.optString("im:id") ?: "$i"
                        val stableId = abs(("itunes_chart_${onlineId}").hashCode().toLong())
                        val genre = entry.optJSONObject("category")?.optJSONObject("attributes")?.optString("label") ?: "Top Chart"

                        songs.add(
                            Song(
                                id = stableId,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = 30000L,
                                dataPath = streamUrl,
                                albumId = 0L,
                                artworkUri = hdImage,
                                dateAdded = System.currentTimeMillis(),
                                isOnline = true,
                                streamUrl = streamUrl,
                                sourceId = sourceId,
                                onlineTrackId = onlineId,
                                genre = genre
                            )
                        )
                    }
                }

                Result.success(songs)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading trending tracks: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getTracksByGenre(genre: String, limit: Int): Result<List<Song>> {
        // Query by genre keyword using iTunes search
        return searchTracks(query = genre, limit = limit)
    }

    override suspend fun resolveStream(song: Song): Result<String> {
        val url = song.streamUrl ?: song.dataPath
        return if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
            Result.success(url)
        } else {
            Result.failure(Exception("Stream URL is missing or invalid for track ${song.title}"))
        }
    }
}

