package com.bishify.player.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class SongLyrics(
    val isSynced: Boolean,
    val lines: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null
)

class LyricsRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "LyricsRepository"
    private val memoryCache = mutableMapOf<String, SongLyrics>()
    private val lrcPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    suspend fun getLyrics(
        title: String,
        artist: String,
        durationMs: Long = 0L,
        lyricsId: String? = null
    ): Result<SongLyrics> = withContext(Dispatchers.IO) {
        val cacheKey = "${cleanQuery(title)}_${cleanQuery(artist)}"
        memoryCache[cacheKey]?.let {
            return@withContext Result.success(it)
        }

        // 1. Try LRCLIB exact match
        val lrclibResult = fetchFromLrclib(title, artist, durationMs)
        if (lrclibResult.isSuccess && lrclibResult.getOrNull()?.lines?.isNotEmpty() == true) {
            val lyrics = lrclibResult.getOrThrow()
            memoryCache[cacheKey] = lyrics
            return@withContext Result.success(lyrics)
        }

        // 2. Try LRCLIB search fallback
        val lrclibSearchResult = searchLrclib(title, artist)
        if (lrclibSearchResult.isSuccess && lrclibSearchResult.getOrNull()?.lines?.isNotEmpty() == true) {
            val lyrics = lrclibSearchResult.getOrThrow()
            memoryCache[cacheKey] = lyrics
            return@withContext Result.success(lyrics)
        }

        // 3. Fallback to JioSaavn plain lyrics if lyricsId provided
        if (!lyricsId.isNullOrBlank()) {
            val saavnResult = fetchFromJioSaavn(lyricsId)
            if (saavnResult.isSuccess) {
                val lyrics = saavnResult.getOrThrow()
                memoryCache[cacheKey] = lyrics
                return@withContext Result.success(lyrics)
            }
        }

        Result.failure(Exception("No synchronized lyrics found for $title by $artist"))
    }

    private fun fetchFromLrclib(title: String, artist: String, durationMs: Long): Result<SongLyrics> {
        return try {
            val cleanTitle = cleanQuery(title)
            val cleanArtist = cleanQuery(artist)
            val durationSec = if (durationMs > 0) durationMs / 1000L else 0L

            var url = "https://lrclib.net/api/get?track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}"
            if (durationSec > 0) {
                url += "&duration=$durationSec"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SoundWave-Android/1.0 (https://github.com/AashishCyberH4CKS/SoundWave)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("LRCLIB returned ${response.code}"))
                }

                val body = response.body?.string() ?: return Result.failure(Exception("Empty body"))
                val json = JSONObject(body)
                val synced = json.optString("syncedLyrics", "")
                val plain = json.optString("plainLyrics", "")

                if (synced.isNotBlank()) {
                    val lines = parseLrc(synced)
                    Result.success(SongLyrics(isSynced = true, lines = lines, plainLyrics = plain))
                } else if (plain.isNotBlank()) {
                    Result.success(SongLyrics(isSynced = false, plainLyrics = plain))
                } else {
                    Result.failure(Exception("Lyrics not available"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun searchLrclib(title: String, artist: String): Result<SongLyrics> {
        return try {
            val query = "${cleanQuery(title)} ${cleanQuery(artist)}".trim()
            val url = "https://lrclib.net/api/search?q=${URLEncoder.encode(query, "UTF-8")}"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SoundWave-Android/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.failure(Exception("LRCLIB search error"))
                val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
                val array = JSONArray(body)

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val synced = item.optString("syncedLyrics", "")
                    val plain = item.optString("plainLyrics", "")
                    if (synced.isNotBlank()) {
                        val lines = parseLrc(synced)
                        return Result.success(SongLyrics(isSynced = true, lines = lines, plainLyrics = plain))
                    }
                }
                Result.failure(Exception("No synced lyrics found in search results"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchFromJioSaavn(lyricsId: String): Result<SongLyrics> {
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=lyrics.getLyrics&_format=json&_marker=0&api_version=4&ctx=web6dot0&lyrics_id=$lyricsId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.failure(Exception("JioSaavn lyrics error"))
                val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
                val json = JSONObject(body)
                val rawLyrics = json.optString("lyrics", "")
                if (rawLyrics.isNotBlank()) {
                    val formatted = rawLyrics.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
                    Result.success(SongLyrics(isSynced = false, plainLyrics = formatted))
                } else {
                    Result.failure(Exception("No lyrics available"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (rawLine in lrcContent.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isBlank()) continue

            val matcher = lrcPattern.matcher(trimmed)
            if (matcher.find()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = matcher.group(3) ?: "0"
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val timeMs = (min * 60 + sec) * 1000 + ms
                val text = matcher.group(4)?.trim() ?: ""
                lines.add(LyricLine(timeMs = timeMs, text = text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun cleanQuery(str: String): String {
        return str
            .replace(Regex("(?i)\\b(official|music|video|audio|lyric|lyrics|remix|feat|ft|explicit|prod|extended|version)\\b"), "")
            .replace(Regex("[\\(\\)\\[\\]\\-_,.]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

