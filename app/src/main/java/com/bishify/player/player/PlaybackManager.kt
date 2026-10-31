package com.bishify.player.player

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.bishify.player.model.RepeatState
import com.bishify.player.model.Song
import com.bishify.player.service.SoundWavePlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class PlaybackManager private constructor(private val context: Context) {

    private val tag = "PlaybackManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null
        private set

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatState = MutableStateFlow(RepeatState.OFF)
    val repeatState: StateFlow<RepeatState> = _repeatState.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _sleepTimerRemainingMinutes = MutableStateFlow(0)
    val sleepTimerRemainingMinutes: StateFlow<Int> = _sleepTimerRemainingMinutes.asStateFlow()

    private var progressJob: Job? = null
    private var sleepCountdownJob: Job? = null

    init {
        connectService()
    }

    private fun connectService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, SoundWavePlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
                _isConnected.value = true
                startPositionTracker()
            } catch (e: Exception) {
                Log.e(tag, "Failed to connect to MediaController: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupControllerListener() {
        val controller = mediaController ?: return

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _duration.value = controller.duration.coerceAtLeast(0L)
                if (playbackState == Player.STATE_ENDED) {
                    if (controller.hasNextMediaItem()) {
                        controller.seekToNextMediaItem()
                        controller.prepare()
                        controller.play()
                    } else if (controller.mediaItemCount > 0) {
                        controller.seekToDefaultPosition(0)
                        controller.prepare()
                        controller.play()
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentIdx = controller.currentMediaItemIndex
                _currentIndex.value = currentIdx
                val currentQ = _queue.value
                val foundSong = if (currentIdx in currentQ.indices) {
                    currentQ[currentIdx]
                } else {
                    val songId = mediaItem?.mediaId?.toLongOrNull()
                    currentQ.firstOrNull { it.id == songId }
                }
                if (foundSong != null) {
                    _currentSong.value = foundSong
                }
                _duration.value = controller.duration.coerceAtLeast(0L)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatState.value = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatState.ONE
                    Player.REPEAT_MODE_ALL -> RepeatState.ALL
                    else -> RepeatState.OFF
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffle.value = shuffleModeEnabled
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _playbackSpeed.value = playbackParameters.speed
            }
        })

        _isPlaying.value = controller.isPlaying
        _repeatState.value = when (controller.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatState.ONE
            Player.REPEAT_MODE_ALL -> RepeatState.ALL
            else -> RepeatState.OFF
        }
        _isShuffle.value = controller.shuffleModeEnabled
        _playbackSpeed.value = controller.playbackParameters.speed
    }

    private fun startPositionTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
                        _duration.value = controller.duration.coerceAtLeast(0L)
                    }
                }
                delay(300)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val controller = mediaController ?: return

        _queue.value = songs
        _currentIndex.value = startIndex.coerceIn(0, songs.size - 1)
        _currentSong.value = songs[_currentIndex.value]

        val mediaItems = songs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.artworkUri?.let { Uri.parse(it) })
                .build()

            val contentUri = if (song.id > 0) {
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            } else {
                Uri.fromFile(File(song.dataPath))
            }

            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(contentUri)
                .setMediaMetadata(metadata)
                .build()
        }

        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playSingle(song: Song) {
        val currentQ = _queue.value.toMutableList()
        val index = currentQ.indexOfFirst { it.id == song.id }
        if (index != -1) {
            mediaController?.seekTo(index, 0L)
            mediaController?.prepare()
            mediaController?.play()
        } else {
            currentQ.add(song)
            playQueue(currentQ, currentQ.lastIndex)
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun skipNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (_queue.value.isNotEmpty()) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaController?.let { controller ->
            controller.playbackParameters = PlaybackParameters(speed)
            val bundle = Bundle().apply {
                putFloat("speed", speed)
            }
            controller.sendCustomCommand(
                SessionCommand(SoundWavePlaybackService.COMMAND_SET_SPEED, Bundle.EMPTY),
                bundle
            )
        }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun addToQueueNext(song: Song) {
        val currentQ = _queue.value.toMutableList()
        val currentIdx = _currentIndex.value
        val insertIndex = (currentIdx + 1).coerceAtMost(currentQ.size)
        currentQ.add(insertIndex, song)
        _queue.value = currentQ

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.artworkUri?.let { Uri.parse(it) })
            .build()

        val contentUri = if (song.id > 0) {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        } else {
            Uri.fromFile(File(song.dataPath))
        }

        val item = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()

        mediaController?.addMediaItem(insertIndex, item)
    }

    fun addToQueueEnd(song: Song) {
        val currentQ = _queue.value.toMutableList()
        currentQ.add(song)
        _queue.value = currentQ

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.artworkUri?.let { Uri.parse(it) })
            .build()

        val contentUri = if (song.id > 0) {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        } else {
            Uri.fromFile(File(song.dataPath))
        }

        val item = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()

        mediaController?.addMediaItem(item)
    }

    fun removeFromQueue(index: Int) {
        val currentQ = _queue.value.toMutableList()
        if (index in currentQ.indices) {
            currentQ.removeAt(index)
            _queue.value = currentQ
            mediaController?.removeMediaItem(index)
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentQ = _queue.value.toMutableList()
        if (fromIndex in currentQ.indices && toIndex in currentQ.indices) {
            val item = currentQ.removeAt(fromIndex)
            currentQ.add(toIndex, item)
            _queue.value = currentQ
            mediaController?.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentSong.value = null
        mediaController?.clearMediaItems()
        mediaController?.stop()
    }

    fun startSleepTimer(minutes: Int) {
        _sleepTimerRemainingMinutes.value = minutes
        val bundle = Bundle().apply {
            putInt("minutes", minutes)
        }
        mediaController?.sendCustomCommand(
            SessionCommand(SoundWavePlaybackService.COMMAND_START_SLEEP_TIMER, Bundle.EMPTY),
            bundle
        )

        sleepCountdownJob?.cancel()
        if (minutes > 0) {
            sleepCountdownJob = scope.launch {
                for (m in minutes downTo 1) {
                    _sleepTimerRemainingMinutes.value = m
                    delay(60 * 1000L)
                }
                _sleepTimerRemainingMinutes.value = 0
            }
        }
    }

    fun cancelSleepTimer() {
        _sleepTimerRemainingMinutes.value = 0
        sleepCountdownJob?.cancel()
        mediaController?.sendCustomCommand(
            SessionCommand(SoundWavePlaybackService.COMMAND_CANCEL_SLEEP_TIMER, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    fun sendEqBand(band: Short, level: Short) {
        val bundle = Bundle().apply {
            putShort("band", band)
            putShort("level", level)
        }
        mediaController?.sendCustomCommand(
            SessionCommand(SoundWavePlaybackService.COMMAND_SET_EQ_BAND, Bundle.EMPTY),
            bundle
        )
    }

    fun sendBassBoost(strength: Short) {
        val bundle = Bundle().apply {
            putShort("strength", strength)
        }
        mediaController?.sendCustomCommand(
            SessionCommand(SoundWavePlaybackService.COMMAND_SET_BASS_BOOST, Bundle.EMPTY),
            bundle
        )
    }

    fun sendVirtualizer(strength: Short) {
        val bundle = Bundle().apply {
            putShort("strength", strength)
        }
        mediaController?.sendCustomCommand(
            SessionCommand(SoundWavePlaybackService.COMMAND_SET_VIRTUALIZER, Bundle.EMPTY),
            bundle
        )
    }

    fun sendEqEnabled(enabled: Boolean) {
        val bundle = Bundle().apply {
            putBoolean("enabled", enabled)
        }
        mediaController?.sendCustomCommand(
            SessionCommand(SoundWavePlaybackService.COMMAND_SET_EQ_ENABLED, Bundle.EMPTY),
            bundle
        )
    }

    fun sendEqPreset(presetIndex: Short) {
        val bundle = Bundle().apply {
            putShort("presetIndex", presetIndex)
        }
        mediaController?.sendCustomCommand(
            SessionCommand(SoundWavePlaybackService.COMMAND_SET_EQ_PRESET, Bundle.EMPTY),
            bundle
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PlaybackManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

