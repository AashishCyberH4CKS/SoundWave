package com.bishify.player.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.bishify.player.local.DownloadedSongEntity
import com.bishify.player.local.PreferencesManager
import com.bishify.player.local.SoundWaveDatabase
import com.bishify.player.model.Song
import com.bishify.player.online.StreamResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class ActiveDownload(
    val songId: Long,
    val title: String,
    val artist: String,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedKbps: Long,
    val status: DownloadStatus,
    val errorMessage: String? = null
)

/**
 * SoundWave's 100% Private Offline Music Download Manager.
 * All downloaded audio files are stored inside SoundWave's internal application files
 * (context.filesDir/downloads/), completely invisible to external media scanners, public folders,
 * and third-party music players.
 */
class OfflineDownloadManager private constructor(private val context: Context) {

    private val tag = "OfflineDownloadManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database = SoundWaveDatabase.getInstance(context)
    private val downloadDao = database.downloadDao()
    private val preferencesManager = PreferencesManager(context)
    private val streamResolver = StreamResolver()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Download directory inside private internal app storage
    private val privateDownloadDir: File by lazy {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        // Create .nomedia to prevent any Android media indexer from touching it
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }
        dir
    }

    // Active download coroutine jobs and tracking map
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val _activeDownloads = MutableStateFlow<Map<Long, ActiveDownload>>(emptyMap())
    val activeDownloads: StateFlow<Map<Long, ActiveDownload>> = _activeDownloads.asStateFlow()

    // Completed downloads from Room Database
    val completedDownloads: StateFlow<List<DownloadedSongEntity>> = downloadDao.getCompletedDownloads()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val downloadedSongIds: StateFlow<Set<Long>> = downloadDao.getCompletedSongIds()
        .map { it.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val totalStorageBytes: StateFlow<Long> = downloadDao.getTotalDownloadSize()
        .map { it ?: 0L }
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    init {
        // Synchronize on startup: verify that DB records match physical files
        scope.launch {
            verifyPhysicalFiles()
        }
    }

    /**
     * Checks that every record marked as COMPLETED actually exists in private storage.
     * If a file was removed, clean up the DB record.
     */
    private suspend fun verifyPhysicalFiles() = withContext(Dispatchers.IO) {
        try {
            val records = downloadDao.getCompletedDownloads().first()
            for (record in records) {
                val file = File(record.localFilePath)
                if (!file.exists() || file.length() == 0L) {
                    downloadDao.deleteDownload(record.songId)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "File verification error: ${e.message}")
        }
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Download a single track to SoundWave private storage.
     */
    fun enqueueDownload(song: Song) {
        // Prevent duplicate downloads
        if (downloadedSongIds.value.contains(song.id)) {
            Log.d(tag, "Song ${song.title} is already downloaded")
            return
        }

        if (activeJobs.containsKey(song.id)) {
            Log.d(tag, "Song ${song.title} is already being downloaded")
            return
        }

        val job = scope.launch {
            val prefs = preferencesManager.preferencesFlow.first()

            // Check Wi-Fi only restriction
            if (prefs.downloadWifiOnly && !isWifiConnected()) {
                val state = ActiveDownload(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    progress = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                    speedKbps = 0L,
                    status = DownloadStatus.FAILED,
                    errorMessage = "Wi-Fi required by settings"
                )
                updateActiveState(song.id, state)
                return@launch
            }

            executeDownload(song, prefs.downloadQuality)
        }

        activeJobs[song.id] = job
    }

    private suspend fun executeDownload(song: Song, quality: String) = withContext(Dispatchers.IO) {
        // Resolve stream URL if not present or expired
        var streamUrl = song.streamUrl
        if (streamUrl.isNullOrBlank() || (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://"))) {
            updateActiveState(
                song.id,
                ActiveDownload(song.id, song.title, song.artist, 0f, 0L, 0L, 0L, DownloadStatus.QUEUED)
            )
            val resolved = streamResolver.resolveStreamUrl(song)
            if (resolved.isSuccess) {
                streamUrl = resolved.getOrThrow()
            } else {
                val err = "Could not resolve stream URL"
                updateActiveState(
                    song.id,
                    ActiveDownload(song.id, song.title, song.artist, 0f, 0L, 0L, 0L, DownloadStatus.FAILED, err)
                )
                return@withContext
            }
        }

        // Target file in private storage
        val targetFile = File(privateDownloadDir, "${song.id}.m4a")
        val partFile = File(privateDownloadDir, "${song.id}.part")

        var existingBytes = 0L
        if (partFile.exists()) {
            existingBytes = partFile.length()
        }

        // Insert / Update initial DB record
        val entity = DownloadedSongEntity(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            artworkUri = song.artworkUri,
            localFilePath = targetFile.absolutePath,
            fileSizeBytes = 0L,
            quality = quality,
            status = "DOWNLOADING",
            downloadProgress = 0f,
            downloadUrl = streamUrl
        )
        downloadDao.insertDownload(entity)

        val requestBuilder = Request.Builder()
            .url(streamUrl)
            .header("User-Agent", "SoundWave-Player/1.0")

        if (existingBytes > 0L) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val responseLength = body.contentLength()
                val totalBytes = if (response.code == 206) responseLength + existingBytes else responseLength

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(partFile, response.code == 206)

                val buffer = ByteArray(16384)
                var bytesRead: Int
                var totalRead = existingBytes
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L
                var currentSpeedKbps = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!coroutineContext.isActive) {
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        return@withContext
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    bytesSinceLastCalc += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastSpeedCalcTime
                    if (timeDelta >= 500) {
                        currentSpeedKbps = ((bytesSinceLastCalc * 1000L) / timeDelta) / 1024L
                        bytesSinceLastCalc = 0L
                        lastSpeedCalcTime = now

                        val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes.toFloat() else 0f
                        updateActiveState(
                            song.id,
                            ActiveDownload(
                                songId = song.id,
                                title = song.title,
                                artist = song.artist,
                                progress = progress.coerceIn(0f, 1f),
                                bytesDownloaded = totalRead,
                                totalBytes = totalBytes,
                                speedKbps = currentSpeedKbps,
                                status = DownloadStatus.DOWNLOADING
                            )
                        )
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                // Finalize: Rename .part to .m4a
                if (partFile.exists()) {
                    if (targetFile.exists()) targetFile.delete()
                    partFile.renameTo(targetFile)
                }

                // Update DB to COMPLETED
                val completedEntity = entity.copy(
                    status = "COMPLETED",
                    downloadProgress = 1.0f,
                    fileSizeBytes = targetFile.length(),
                    downloadDate = System.currentTimeMillis()
                )
                downloadDao.insertDownload(completedEntity)

                // Remove from active state
                removeActiveState(song.id)
                activeJobs.remove(song.id)
                Log.d(tag, "Successfully downloaded ${song.title} to private storage: ${targetFile.absolutePath}")
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                Log.d(tag, "Download for ${song.title} cancelled or paused")
                downloadDao.updateStatus(song.id, "PAUSED", 0f)
            } else {
                Log.e(tag, "Download failed for ${song.title}: ${e.message}")
                downloadDao.updateStatus(song.id, "FAILED", 0f)
                updateActiveState(
                    song.id,
                    ActiveDownload(
                        songId = song.id,
                        title = song.title,
                        artist = song.artist,
                        progress = 0f,
                        bytesDownloaded = 0L,
                        totalBytes = 0L,
                        speedKbps = 0L,
                        status = DownloadStatus.FAILED,
                        errorMessage = e.localizedMessage ?: "Network error"
                    )
                )
            }
            activeJobs.remove(song.id)
        }
    }

    fun pauseDownload(songId: Long) {
        val job = activeJobs[songId]
        if (job != null) {
            job.cancel()
            activeJobs.remove(songId)
            scope.launch {
                downloadDao.updateStatus(songId, "PAUSED", 0f)
            }
            updateActiveState(
                songId,
                _activeDownloads.value[songId]?.copy(status = DownloadStatus.PAUSED)
            )
        }
    }

    fun resumeDownload(song: Song) {
        enqueueDownload(song)
    }

    fun cancelDownload(songId: Long) {
        val job = activeJobs[songId]
        job?.cancel()
        activeJobs.remove(songId)
        removeActiveState(songId)

        scope.launch {
            val partFile = File(privateDownloadDir, "$songId.part")
            if (partFile.exists()) partFile.delete()
            val targetFile = File(privateDownloadDir, "$songId.m4a")
            if (targetFile.exists()) targetFile.delete()
            downloadDao.deleteDownload(songId)
        }
    }

    fun deleteDownloadedSong(songId: Long) {
        scope.launch {
            // Cancel if active
            activeJobs[songId]?.cancel()
            activeJobs.remove(songId)
            removeActiveState(songId)

            val file = File(privateDownloadDir, "$songId.m4a")
            if (file.exists()) {
                file.delete()
            }
            val partFile = File(privateDownloadDir, "$songId.part")
            if (partFile.exists()) {
                partFile.delete()
            }
            downloadDao.deleteDownload(songId)
            Log.d(tag, "Deleted private download for songId: $songId")
        }
    }

    fun clearAllDownloads() {
        scope.launch {
            activeJobs.values.forEach { it.cancel() }
            activeJobs.clear()
            _activeDownloads.value = emptyMap()

            // Delete all files in private directory
            privateDownloadDir.listFiles()?.forEach {
                if (it.name != ".nomedia") it.delete()
            }
            downloadDao.clearAll()
            Log.d(tag, "Cleared all private downloaded files")
        }
    }

    fun downloadTracks(songs: List<Song>) {
        songs.forEach { enqueueDownload(it) }
    }

    private fun updateActiveState(songId: Long, state: ActiveDownload?) {
        val current = _activeDownloads.value.toMutableMap()
        if (state != null) {
            current[songId] = state
        } else {
            current.remove(songId)
        }
        _activeDownloads.value = current
    }

    private fun removeActiveState(songId: Long) {
        val current = _activeDownloads.value.toMutableMap()
        current.remove(songId)
        _activeDownloads.value = current
    }

    companion object {
        @Volatile
        private var instance: OfflineDownloadManager? = null

        fun getInstance(context: Context): OfflineDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
