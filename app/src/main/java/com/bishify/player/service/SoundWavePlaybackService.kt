package com.bishify.player.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.bishify.player.R
import com.bishify.player.local.SoundWaveDatabase
import com.bishify.player.local.ListeningHistoryEntity
import com.bishify.player.local.PreferencesManager
import com.bishify.player.local.SavedQueueState
import com.bishify.player.ui.MainActivity
import com.bishify.player.widget.SoundWaveAppWidgetProvider
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SoundWavePlaybackService : MediaSessionService() {

    private val tag = "SoundWavePlaybackService"

    companion object {
        const val CHANNEL_ID = "soundwave_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.soundwave.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.soundwave.ACTION_NEXT"
        const val ACTION_PREV = "com.soundwave.ACTION_PREV"

        // Commands for custom actions
        const val COMMAND_SET_SPEED = "com.soundwave.COMMAND_SET_SPEED"
        const val COMMAND_START_SLEEP_TIMER = "com.soundwave.COMMAND_START_SLEEP_TIMER"
        const val COMMAND_CANCEL_SLEEP_TIMER = "com.soundwave.COMMAND_CANCEL_SLEEP_TIMER"
        const val COMMAND_GET_EFFECTS = "com.soundwave.COMMAND_GET_EFFECTS"
        const val COMMAND_SET_EQ_BAND = "com.soundwave.COMMAND_SET_EQ_BAND"
        const val COMMAND_SET_BASS_BOOST = "com.soundwave.COMMAND_SET_BASS_BOOST"
        const val COMMAND_SET_VIRTUALIZER = "com.soundwave.COMMAND_SET_VIRTUALIZER"
        const val COMMAND_SET_EQ_PRESET = "com.soundwave.COMMAND_SET_EQ_PRESET"
        const val COMMAND_SET_EQ_ENABLED = "com.soundwave.COMMAND_SET_EQ_ENABLED"
    }

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    lateinit var audioEffectsManager: AudioEffectsManager
        private set

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var sleepTimerJob: Job? = null
    private var fadeInJob: Job? = null
    private var playStartTimestamp: Long = 0L
    private var lastTrackId: Long = -1L
    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundWave:PlaybackWakeLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24-hr safe max
            }
        } catch (e: Exception) {
            Log.e(tag, "Error acquiring wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing wake lock: ${e.message}")
        }
    }

    private val preferencesManager by lazy { PreferencesManager(applicationContext) }
    private val database by lazy { SoundWaveDatabase.getInstance(applicationContext) }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                serviceScope.launch {
                    val prefs = preferencesManager.preferencesFlow.first()
                    if (prefs.pauseOnHeadsetDisconnect && player.isPlaying) {
                        player.pause()
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // true handles audio focus automatically
            .setHandleAudioBecomingNoisy(false) // handled custom
            .setWakeMode(C.WAKE_MODE_LOCAL) // keeps CPU awake while playing on lock screen
            .build()

        audioEffectsManager = AudioEffectsManager(this)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    audioEffectsManager.attachToSession(player.audioSessionId)
                } else if (playbackState == Player.STATE_ENDED) {
                    // Current track/playlist reached the end!
                    // Automatically play next track or loop back to start so music keeps playing automatically!
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                        player.prepare()
                        player.play()
                    } else if (player.mediaItemCount > 0) {
                        player.seekToDefaultPosition(0)
                        player.prepare()
                        player.play()
                    }
                }
                updateWidget()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidget()
                if (isPlaying) {
                    acquireWakeLock()
                    playStartTimestamp = System.currentTimeMillis()
                    applyFadeInIfEnabled()
                } else {
                    releaseWakeLock()
                    recordListeningHistory()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                recordListeningHistory()
                lastTrackId = mediaItem?.mediaId?.toLongOrNull() ?: -1L
                playStartTimestamp = System.currentTimeMillis()
                updateWidget()
                saveQueueState()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(tag, "Playback error: ${error.message}", error)
                // If a file cannot be played, skip to next item automatically instead of pausing!
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                } else if (player.mediaItemCount > 1) {
                    player.seekToDefaultPosition(0)
                    player.prepare()
                    player.play()
                }
            }
        })

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, filter)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun applyFadeInIfEnabled() {
        serviceScope.launch {
            val prefs = preferencesManager.preferencesFlow.first()
            if (prefs.fadeInEnabled) {
                fadeInJob?.cancel()
                fadeInJob = launch {
                    val duration = prefs.fadeInDurationMs.coerceAtLeast(300L)
                    val steps = 20
                    val interval = duration / steps
                    for (i in 1..steps) {
                        val volume = i.toFloat() / steps
                        player.volume = volume
                        delay(interval)
                    }
                    player.volume = 1.0f
                }
            } else {
                player.volume = 1.0f
            }
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) return

        sleepTimerJob = serviceScope.launch {
            delay(minutes * 60 * 1000L)
            // Gradual fade out before pausing
            val steps = 15
            val currentVol = player.volume
            for (i in steps downTo 0) {
                player.volume = currentVol * (i.toFloat() / steps)
                delay(100)
            }
            player.pause()
            player.volume = 1.0f
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    private fun recordListeningHistory() {
        val currentTrack = lastTrackId
        if (currentTrack <= 0) return

        val durationPlayed = System.currentTimeMillis() - playStartTimestamp
        val totalDuration = try { player.duration.coerceAtLeast(0L) } catch (_: Exception) { 0L }
        // Threshold: Only count play if listened for at least 15 seconds
        if (durationPlayed >= 15000L) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val prefs = preferencesManager.preferencesFlow.first()
                    if (!prefs.privateListeningMode) {
                        database.historyDao().recordHistory(
                            ListeningHistoryEntity(
                                songId = currentTrack,
                                durationPlayedMs = durationPlayed,
                                completed = totalDuration > 0L && durationPlayed >= (totalDuration - 5000L).coerceAtLeast(0L)
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error recording history: ${e.message}")
                }
            }
        }
    }

    private fun saveQueueState() {
        val currentMedia = try { player.currentMediaItem } catch (_: Exception) { null }
        val songId = currentMedia?.mediaId?.toLongOrNull()
        val currentPosition = try { player.currentPosition } catch (_: Exception) { 0L }
        val repeatMode = try { player.repeatMode } catch (_: Exception) { Player.REPEAT_MODE_OFF }
        val isShuffle = try { player.shuffleModeEnabled } catch (_: Exception) { false }

        serviceScope.launch(Dispatchers.IO) {
            try {
                database.queueDao().saveState(
                    SavedQueueState(
                        currentSongId = songId,
                        currentPositionMs = currentPosition,
                        repeatMode = repeatMode,
                        isShuffle = isShuffle
                    )
                )
            } catch (e: Exception) {
                Log.e(tag, "Error saving queue state: ${e.message}")
            }
        }
    }

    private fun updateWidget() {
        try {
            val intent = Intent(this, SoundWaveAppWidgetProvider::class.java).apply {
                action = SoundWaveAppWidgetProvider.ACTION_UPDATE_WIDGET
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error updating widget: ${e.message}")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            ACTION_NEXT -> player.seekToNextMediaItem()
            ACTION_PREV -> player.seekToPreviousMediaItem()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val activePlayer = mediaSession?.player
        // Keep playing in background if playback is active or queued
        if (activePlayer == null || !activePlayer.playWhenReady || activePlayer.mediaItemCount == 0 || activePlayer.playbackState == Player.STATE_ENDED) {
            releaseWakeLock()
            stopSelf()
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        unregisterReceiver(becomingNoisyReceiver)
        sleepTimerJob?.cancel()
        fadeInJob?.cancel()
        audioEffectsManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(COMMAND_SET_SPEED, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_START_SLEEP_TIMER, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_CANCEL_SLEEP_TIMER, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_EQ_ENABLED, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_EQ_BAND, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_BASS_BOOST, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_VIRTUALIZER, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_EQ_PRESET, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                COMMAND_SET_SPEED -> {
                    val speed = args.getFloat("speed", 1.0f)
                    player.playbackParameters = PlaybackParameters(speed)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_START_SLEEP_TIMER -> {
                    val minutes = args.getInt("minutes", 0)
                    startSleepTimer(minutes)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_CANCEL_SLEEP_TIMER -> {
                    cancelSleepTimer()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_SET_EQ_ENABLED -> {
                    val enabled = args.getBoolean("enabled", false)
                    audioEffectsManager.setEnabled(enabled)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_SET_EQ_BAND -> {
                    val band = args.getShort("band", 0)
                    val level = args.getShort("level", 0)
                    audioEffectsManager.setBandLevel(band, level)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_SET_BASS_BOOST -> {
                    val strength = args.getShort("strength", 0)
                    audioEffectsManager.setBassBoost(strength)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_SET_VIRTUALIZER -> {
                    val strength = args.getShort("strength", 0)
                    audioEffectsManager.setVirtualizer(strength)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_SET_EQ_PRESET -> {
                    val index = args.getShort("presetIndex", 0)
                    audioEffectsManager.usePreset(index)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
}

