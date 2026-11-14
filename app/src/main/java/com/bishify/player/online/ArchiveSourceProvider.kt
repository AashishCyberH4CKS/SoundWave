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
 * Open audio provider using the Internet Archive public audio collection.
 * Delivers Creative Commons, live concerts, and public domain full-length audio tracks.
 */
class ArchiveSourceProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : StreamingSourceProvider {

    private val tag = "ArchiveSourceProvider"

    override val sourceId: String = "archive"
    override val displayName: String = "Internet Archive (Open Audio)"

    override suspend fun searchTracks(query: String, limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())

            val cleanQuery = query.trim().replace("\"", "")
            val encoded = URLEncoder.encode("mediatype:audio AND ($cleanQuery)", "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=$encoded&fl[]=identifier,title,creator,year&rows=$limit&output=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SoundWave-Android/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Archive search failed with code ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val json = JSONObject(body)
                val responseObj = json.optJSONObject("response") ?: return@withContext Result.success(emptyList())
                val docs = responseObj.optJSONArray("docs") ?: return@withContext Result.success(emptyList())

                val songs = mutableListOf<Song>()
                for (i in 0 until docs.length()) {
                    val doc = docs.getJSONObject(i)
                    val id = doc.optString("identifier")
                    if (id.isNullOrBlank()) continue

                    val title = doc.optString("title", "Untitled Audio")
                    val creator = doc.optString("creator", "Unknown Artist")
                    val year = doc.optString("year", "")
                    val album = if (year.isNotBlank()) "Archive ($year)" else "Internet Archive"
                    val artworkUrl = "https://archive.org/services/img/$id"

                    // Use standard metadata endpoint stream resolution for play
                    val stableId = abs(("archive_${id}").hashCode().toLong())

                    songs.add(
                        Song(
                            id = stableId,
                            title = title,
                            artist = creator,
                            album = album,
                            durationMs = 180000L,
                            dataPath = "https://archive.org/download/$id",
                            albumId = 0L,
                            artworkUri = artworkUrl,
                            dateAdded = System.currentTimeMillis(),
                            isOnline = true,
                            streamUrl = null, // resolved dynamically on stream request
                            sourceId = sourceId,
                            onlineTrackId = id,
                            genre = "Open Audio"
                        )
                    )
                }

                Result.success(songs)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error searching Archive tracks: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getTrendingTracks(limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        // Search popular open collections
        searchTracks(query = "classical piano acoustic ambient", limit = limit)
    }

    override suspend fun getTracksByGenre(genre: String, limit: Int): Result<List<Song>> {
        return searchTracks(query = genre, limit = limit)
    }

    override suspend fun resolveStream(song: Song): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!song.streamUrl.isNullOrBlank()) {
                return@withContext Result.success(song.streamUrl)
            }

            val id = song.onlineTrackId ?: return@withContext Result.failure(Exception("Missing archive ID"))
            val metaUrl = "https://archive.org/metadata/$id/files"

            val request = Request.Builder()
                .url(metaUrl)
                .header("User-Agent", "SoundWave-Android/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to resolve files for $id"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty file list"))
                val json = JSONObject(body)
                val files = json.optJSONArray("result") ?: return@withContext Result.failure(Exception("No files found"))

                var mp3Filename: String? = null
                for (i in 0 until files.length()) {
                    val fileObj = files.getJSONObject(i)
                    val name = fileObj.optString("name")
                    if (name.endsWith(".mp3", ignoreCase = true) && !name.contains("_thumb") && !name.contains("_vbr")) {
                        mp3Filename = name
                        break
                    }
                }

                if (mp3Filename == null) {
                    // Fallback to any mp3
                    for (i in 0 until files.length()) {
                        val name = files.getJSONObject(i).optString("name")
                        if (name.endsWith(".mp3", ignoreCase = true)) {
                            mp3Filename = name
                            break
                        }
                    }
                }

                if (mp3Filename != null) {
                    val streamUrl = "https://archive.org/download/$id/$mp3Filename"
                    Result.success(streamUrl)
                } else {
                    Result.failure(Exception("No playable MP3 stream found in archive $id"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error resolving archive stream for ${song.title}: ${e.message}")
            Result.failure(e)
        }
    }
}

