package com.bishify.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.bishify.player.R
import com.bishify.player.player.PlaybackManager
import com.bishify.player.service.SoundWavePlaybackService
import com.bishify.player.ui.MainActivity

class SoundWaveAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.soundwave.ACTION_UPDATE_WIDGET"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SoundWaveAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_soundwave_layout)
        val playbackManager = PlaybackManager.getInstance(context)
        val currentSong = playbackManager.currentSong.value
        val isPlaying = playbackManager.isPlaying.value

        views.setTextViewText(R.id.widget_title, currentSong?.title ?: "BISHIFY")
        views.setTextViewText(R.id.widget_artist, currentSong?.artist ?: "Offline Music Player")

        val playIconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        views.setImageViewResource(R.id.widget_btn_play_pause, playIconRes)

        // PendingIntent to launch app
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

        // Play/Pause Intent to Service
        val playIntent = Intent(context, SoundWavePlaybackService::class.java).apply {
            action = SoundWavePlaybackService.ACTION_PLAY_PAUSE
        }
        val playPendingIntent = PendingIntent.getService(
            context,
            1,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPendingIntent)

        // Next Intent to Service
        val nextIntent = Intent(context, SoundWavePlaybackService::class.java).apply {
            action = SoundWavePlaybackService.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            context,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

        // Previous Intent to Service
        val prevIntent = Intent(context, SoundWavePlaybackService::class.java).apply {
            action = SoundWavePlaybackService.ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getService(
            context,
            3,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

