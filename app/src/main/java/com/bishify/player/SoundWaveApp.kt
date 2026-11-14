package com.bishify.player

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.bishify.player.local.SoundWaveDatabase
import com.bishify.player.player.PlaybackManager
import com.bishify.player.util.CrashHandler

class SoundWaveApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        try {
            // Initialize Room DB and PlaybackManager
            SoundWaveDatabase.getInstance(this)
            PlaybackManager.getInstance(this)
        } catch (e: Exception) {
            android.util.Log.e("SoundWaveApp", "Failed to initialize singletons: ${e.message}", e)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("bishify_artwork_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}

