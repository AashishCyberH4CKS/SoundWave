package com.bishify.player.online

import android.util.Base64
import android.util.Log
import com.bishify.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

data class OnlineAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val year: String,
    val artworkUri: String,
    val songCount: Int = 0
)

data class OnlinePlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUri: String,
    val songCount: Int = 0
)

/**
 * Production-ready JioSaavn online catalogue and full-length audio streaming provider.
 * Supports both:
 * 1. Native direct client-side engine with instantaneous DES-ECB decryption (zero servers required).
 * 2. Custom self-hosted sumitkolhe/jiosaavn-api instances (https://github.com/sumitkolhe/jiosaavn-api).
 */
class JioSaavnSourceProvider(
    private var customBaseUrl: String? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : StreamingSourceProvider {

    private val tag = "JioSaavnSourceProvider"
    private val desKey = "38346591"
    private val directUpstreamUrl = "https://www.jiosaavn.com/api.php"

    override val sourceId: String = "jiosaavn"
    override val displayName: String = "JioSaavn"

    fun setCustomBaseUrl(url: String?) {
        customBaseUrl = if (url.isNullOrBlank()) null else url.trim().removeSuffix("/")
    }

    /**
     * Decrypts JioSaavn's encrypted_media_url using DES/ECB/PKCS5Padding with key "38346591".
     * Replaces standard 96kbps with 320kbps for audiophile streaming quality.
     */
    fun decryptMediaUrl(encryptedUrl: String, bitrate: String = "320"): String {
        if (encryptedUrl.isBlank()) return ""
        return try {
            val keyBytes = desKey.toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val rawUrl = String(decryptedBytes, Charsets.UTF_8)

            // Upgrade stream to 320kbps or selected bitrate
            rawUrl.replace("_96.", "_${bitrate}.")
                .replace("_96_", "_${bitrate}_")
        } catch (e: Exception) {
            Log.e(tag, "Failed to decrypt media url: ${e.message}")
            ""
        }
    }

    /**
     * Cleans up HTML entity escapes returned by JioSaavn (e.g. &quot;, &amp;, &#039;).
     */
    private fun unescapeHtml(text: String): String {
        if (text.isBlank()) return ""
        return text
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .trim()
    }

    /**
     * Converts raw artwork URL into a crystal clear 500x500 high-resolution artwork link.
     */
    private fun getHighResArtwork(rawUrl: String): String {
        if (rawUrl.isBlank()) return ""
        return rawUrl
            .replace("150x150", "500x500")
            .replace("50x50", "500x500")
            .replace("http://", "https://")
    }

    override suspend fun searchTracks(query: String, limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())

        // 1. If user configured a custom sumitkolhe/jiosaavn-api instance URL
        val custom = customBaseUrl
        if (!custom.isNullOrBlank()) {
            val customResult = searchViaCustomApi(custom, query, limit)
            if (customResult.isSuccess && customResult.getOrNull()?.isNotEmpty() == true) {
                return@withContext customResult
            }
        }

        // 2. Direct upstream JioSaavn search
        searchViaDirectUpstream(query, limit)
    }

    private fun searchViaDirectUpstream(query: String, limit: Int): Result<List<Song>> {
        return try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val targetUrl = "$directUpstreamUrl?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&q=$encodedQuery&p=1&n=$limit"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("JioSaavn server responded with HTTP ${response.code}"))
                }

                val body = response.body?.string() ?: return Result.success(emptyList())
                val json = JSONObject(body)
                val resultsArray = json.optJSONArray("results") ?: return Result.success(emptyList())

                val songs = parseDirectSongArray(resultsArray)
                Result.success(songs)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error searching direct JioSaavn for '$query': ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun searchViaCustomApi(baseUrl: String, query: String, limit: Int): Result<List<Song>> {
        return try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val targetUrl = "$baseUrl/api/search/songs?query=$encodedQuery&page=0&limit=$limit"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "SoundWave-Android/1.0")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Custom API responded with HTTP ${response.code}"))
                }

                val body = response.body?.string() ?: return Result.success(emptyList())
                val json = JSONObject(body)
                val dataObj = json.optJSONObject("data")
                val resultsArray = dataObj?.optJSONArray("results") ?: json.optJSONArray("data") ?: return Result.success(emptyList())

                val songs = mutableListOf<Song>()
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    val idStr = item.optString("id", "")
                    if (idStr.isBlank()) continue

                    val name = unescapeHtml(item.optString("name", item.optString("title", "Unknown Track")))
                    val primaryArtists = unescapeHtml(
                        item.optString("primaryArtists", item.optJSONObject("artists")?.optString("primary") ?: "Unknown Artist")
                    )
                    val albumName = unescapeHtml(
                        item.optJSONObject("album")?.optString("name") ?: item.optString("album", "Single")
                    )
                    val durationSec = item.optLong("duration", 0L)
                    val durationMs = if (durationSec > 0) durationSec * 1000L else 180000L

                    // Resolve artwork
                    val imageArray = item.optJSONArray("image")
                    var artworkUrl = ""
                    if (imageArray != null && imageArray.length() > 0) {
                        artworkUrl = imageArray.getJSONObject(imageArray.length() - 1).optString("url", "")
                    }
                    if (artworkUrl.isBlank()) {
                        artworkUrl = getHighResArtwork(item.optString("image", ""))
                    }

                    // Resolve download/stream URL
                    val downloadArray = item.optJSONArray("downloadUrl")
                    var streamUrl = ""
                    if (downloadArray != null && downloadArray.length() > 0) {
                        // Pick 320kbps or highest quality
                        streamUrl = downloadArray.getJSONObject(downloadArray.length() - 1).optString("url", "")
                    }

                    val stableId = abs(("jiosaavn_$idStr").hashCode().toLong())
                    songs.add(
                        Song(
                            id = stableId,
                            title = name,
                            artist = primaryArtists,
                            album = albumName,
                            durationMs = durationMs,
                            dataPath = streamUrl,
                            albumId = 0L,
                            artworkUri = artworkUrl,
                            dateAdded = System.currentTimeMillis(),
                            isOnline = true,
                            streamUrl = streamUrl,
                            sourceId = sourceId,
                            onlineTrackId = idStr,
                            genre = item.optString("language", "Music")
                        )
                    )
                }

                Result.success(songs)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error searching custom API: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseDirectSongArray(resultsArray: JSONArray): List<Song> {
        val songs = mutableListOf<Song>()
        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.optJSONObject(i) ?: continue
            val idStr = item.optString("id", "")
            if (idStr.isBlank()) continue

            val title = unescapeHtml(item.optString("title", item.optString("song", "Unknown Track")))
            val moreInfo = item.optJSONObject("more_info")

            // Artist resolution
            var artist = ""
            val artistMap = moreInfo?.optJSONObject("artistMap")
            val primaryArtists = artistMap?.optJSONArray("primary_artists")
            if (primaryArtists != null && primaryArtists.length() > 0) {
                val names = mutableListOf<String>()
                for (a in 0 until primaryArtists.length()) {
                    val aObj = primaryArtists.optJSONObject(a)
                    val aName = aObj?.optString("name")
                    if (!aName.isNullOrBlank()) names.add(aName)
                }
                artist = names.joinToString(", ")
            }
            if (artist.isBlank()) {
                artist = moreInfo?.optString("singers", "") ?: ""
            }
            if (artist.isBlank()) {
                artist = item.optString("subtitle", "Unknown Artist")
            }
            artist = unescapeHtml(artist)

            // Album
            val album = unescapeHtml(moreInfo?.optString("album", "Single") ?: "Single")

            // Duration
            val durSec = moreInfo?.optLong("duration", 0L) ?: 0L
            val durationMs = if (durSec > 0) durSec * 1000L else 180000L

            // Artwork
            val rawArtwork = item.optString("image", "")
            val hdArtwork = getHighResArtwork(rawArtwork)

            // Stream URL decryption
            val encryptedUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
            val streamUrl = decryptMediaUrl(encryptedUrl, "320")

            val stableId = abs(("jiosaavn_$idStr").hashCode().toLong())

            songs.add(
                Song(
                    id = stableId,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                    dataPath = streamUrl,
                    albumId = 0L,
                    artworkUri = hdArtwork,
                    dateAdded = System.currentTimeMillis(),
                    isOnline = true,
                    streamUrl = streamUrl,
                    sourceId = sourceId,
                    onlineTrackId = idStr,
                    genre = item.optString("language", "Music")
                )
            )
        }
        return songs
    }

    override suspend fun getTrendingTracks(limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = "$directUpstreamUrl?__call=content.getTrending&_format=json&_marker=0&api_version=4&ctx=web6dot0"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Trending call failed with HTTP ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val array = JSONArray(body)
                val songs = parseDirectSongArray(array)
                val filtered = songs.filter { it.streamUrl?.isNotBlank() == true }.take(limit)

                if (filtered.isNotEmpty()) {
                    Result.success(filtered)
                } else {
                    // Fallback to top hits search if trending response had non-song objects
                    searchTracks("Top Trending Hits", limit)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading trending tracks: ${e.message}", e)
            searchTracks("Global Top Hits", limit)
        }
    }

    override suspend fun getTracksByGenre(genre: String, limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        searchTracks(genre, limit)
    }

    override suspend fun resolveStream(song: Song): Result<String> = withContext(Dispatchers.IO) {
        // If streamUrl is already available and non-empty
        if (!song.streamUrl.isNullOrBlank() && (song.streamUrl.startsWith("http://") || song.streamUrl.startsWith("https://"))) {
            return@withContext Result.success(song.streamUrl)
        }

        // Re-resolve via song.getDetails if track id is available
        val trackId = song.onlineTrackId
        if (trackId.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Cannot resolve stream: missing onlineTrackId"))
        }

        try {
            val targetUrl = "$directUpstreamUrl?__call=song.getDetails&_format=json&_marker=0&api_version=4&ctx=web6dot0&pids=$trackId"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Stream resolution failed: HTTP ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val json = JSONObject(body)
                val songsArray = json.optJSONArray("songs")
                val songObj = if (songsArray != null && songsArray.length() > 0) {
                    songsArray.getJSONObject(0)
                } else {
                    json.optJSONObject(trackId)
                }

                val moreInfo = songObj?.optJSONObject("more_info")
                val encUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                val streamUrl = decryptMediaUrl(encUrl, "320")

                if (streamUrl.isNotBlank()) {
                    Result.success(streamUrl)
                } else {
                    Result.failure(Exception("Failed to decrypt audio stream for $trackId"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error resolving stream for track $trackId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchAlbums(query: String, limit: Int = 15): Result<List<OnlineAlbum>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val targetUrl = "$directUpstreamUrl?__call=search.getAlbumResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&q=$encodedQuery&p=1&n=$limit"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext Result.success(emptyList())

                val albums = mutableListOf<OnlineAlbum>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    if (id.isBlank()) continue
                    val title = unescapeHtml(item.optString("title", "Unknown Album"))
                    val artist = unescapeHtml(item.optString("subtitle", item.optString("artist", "Various Artists")))
                    val year = item.optString("year", "")
                    val image = getHighResArtwork(item.optString("image", ""))
                    val songCount = item.optJSONObject("more_info")?.optInt("song_count", 0) ?: 0

                    albums.add(OnlineAlbum(id = id, title = title, artist = artist, year = year, artworkUri = image, songCount = songCount))
                }
                Result.success(albums)
            }
        } catch (e: Exception) {
            Log.e(tag, "searchAlbums error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchPlaylists(query: String, limit: Int = 15): Result<List<OnlinePlaylist>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val targetUrl = "$directUpstreamUrl?__call=search.getPlaylistResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&q=$encodedQuery&p=1&n=$limit"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext Result.success(emptyList())

                val playlists = mutableListOf<OnlinePlaylist>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    if (id.isBlank()) continue
                    val title = unescapeHtml(item.optString("title", "Unknown Playlist"))
                    val subtitle = unescapeHtml(item.optString("subtitle", item.optString("description", "")))
                    val image = getHighResArtwork(item.optString("image", ""))
                    val count = item.optJSONObject("more_info")?.optInt("song_count", 0) ?: 0

                    playlists.add(OnlinePlaylist(id = id, title = title, subtitle = subtitle, artworkUri = image, songCount = count))
                }
                Result.success(playlists)
            }
        } catch (e: Exception) {
            Log.e(tag, "searchPlaylists error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAlbumTracks(albumId: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = "$directUpstreamUrl?__call=content.getAlbumDetails&_format=json&_marker=0&api_version=4&ctx=web6dot0&albumid=$albumId"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val json = JSONObject(body)
                val list = json.optJSONArray("list") ?: return@withContext Result.success(emptyList())

                val songs = parseDirectSongArray(list)
                Result.success(songs)
            }
        } catch (e: Exception) {
            Log.e(tag, "getAlbumTracks error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPlaylistTracks(playlistId: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = "$directUpstreamUrl?__call=playlist.getDetails&_format=json&_marker=0&api_version=4&ctx=web6dot0&listid=$playlistId"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val json = JSONObject(body)
                val list = json.optJSONArray("list") ?: return@withContext Result.success(emptyList())

                val songs = parseDirectSongArray(list)
                Result.success(songs)
            }
        } catch (e: Exception) {
            Log.e(tag, "getPlaylistTracks error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRecommendedTracks(currentSong: Song, limit: Int = 10): Result<List<Song>> = withContext(Dispatchers.IO) {
        val query = currentSong.artist.ifBlank { currentSong.genre ?: "Top Hits" }
        val searchRes = searchTracks(query, limit = limit + 5)
        if (searchRes.isSuccess) {
            // Exclude current track and return top recommendations
            val filtered = searchRes.getOrDefault(emptyList())
                .filter { it.title.lowercase() != currentSong.title.lowercase() }
                .take(limit)
            Result.success(filtered)
        } else {
            searchRes
        }
    }
}

