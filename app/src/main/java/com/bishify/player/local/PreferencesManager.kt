package com.bishify.player.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.bishify.player.ui.components.PlayerSkin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soundwave_settings")

enum class ThemePreset {
    GRAPHITE_BURGUNDY,
    AMOLED_CRIMSON,
    MIDNIGHT_BLUE,
    FOREST_GREEN,
    PURPLE_HAZE,
    CUSTOM
}

data class SoundWavePreferences(
    val themePreset: ThemePreset = ThemePreset.GRAPHITE_BURGUNDY,
    val isAmoled: Boolean = false,
    val dynamicArtworkTheme: Boolean = true,
    val customAccentColor: Long = 0xFFE63946,
    val wallpaperUri: String? = null,
    val wallpaperDim: Float = 0.5f,
    val vinylAnimationEnabled: Boolean = true,
    val fadeInEnabled: Boolean = true,
    val fadeInDurationMs: Long = 1200L,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutes: Int = 0,
    val drivingModeEnabled: Boolean = false,
    val privateListeningMode: Boolean = false,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val customJioSaavnApiUrl: String = "",
    val streamingAudioQuality: String = "320",
    val playerSkin: PlayerSkin = PlayerSkin.MODERN_GLASS,
    val downloadQuality: String = "320",
    val downloadWifiOnly: Boolean = false,
    val maxSimultaneousDownloads: Int = 3
)

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val IS_AMOLED = booleanPreferencesKey("is_amoled")
        val DYNAMIC_ARTWORK_THEME = booleanPreferencesKey("dynamic_artwork_theme")
        val CUSTOM_ACCENT_COLOR = longPreferencesKey("custom_accent_color")
        val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
        val VINYL_ANIMATION_ENABLED = booleanPreferencesKey("vinyl_animation_enabled")
        val FADE_IN_ENABLED = booleanPreferencesKey("fade_in_enabled")
        val FADE_IN_DURATION_MS = longPreferencesKey("fade_in_duration_ms")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        val DRIVING_MODE_ENABLED = booleanPreferencesKey("driving_mode_enabled")
        val PRIVATE_LISTENING_MODE = booleanPreferencesKey("private_listening_mode")
        val PAUSE_ON_HEADSET_DISCONNECT = booleanPreferencesKey("pause_on_headset_disconnect")
        val CUSTOM_JIOSAAVN_API_URL = stringPreferencesKey("custom_jiosaavn_api_url")
        val STREAMING_AUDIO_QUALITY = stringPreferencesKey("streaming_audio_quality")
        val PLAYER_SKIN = stringPreferencesKey("player_skin")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val MAX_SIMULTANEOUS_DOWNLOADS = intPreferencesKey("max_simultaneous_downloads")
    }

    val preferencesFlow: Flow<SoundWavePreferences> = context.dataStore.data
        .catch { exception ->
            android.util.Log.e("PreferencesManager", "DataStore read error: ${exception.message}")
            emit(emptyPreferences())
        }
        .map { preferences ->
            val themeStr = preferences[PreferencesKeys.THEME_PRESET] ?: ThemePreset.GRAPHITE_BURGUNDY.name
            val theme = try {
                ThemePreset.valueOf(themeStr)
            } catch (e: Exception) {
                ThemePreset.GRAPHITE_BURGUNDY
            }

            SoundWavePreferences(
                themePreset = theme,
                isAmoled = preferences[PreferencesKeys.IS_AMOLED] ?: false,
                dynamicArtworkTheme = preferences[PreferencesKeys.DYNAMIC_ARTWORK_THEME] ?: true,
                customAccentColor = preferences[PreferencesKeys.CUSTOM_ACCENT_COLOR] ?: 0xFFE63946,
                wallpaperUri = preferences[PreferencesKeys.WALLPAPER_URI],
                wallpaperDim = preferences[PreferencesKeys.WALLPAPER_DIM] ?: 0.5f,
                vinylAnimationEnabled = preferences[PreferencesKeys.VINYL_ANIMATION_ENABLED] ?: true,
                fadeInEnabled = preferences[PreferencesKeys.FADE_IN_ENABLED] ?: true,
                fadeInDurationMs = preferences[PreferencesKeys.FADE_IN_DURATION_MS] ?: 1200L,
                playbackSpeed = preferences[PreferencesKeys.PLAYBACK_SPEED] ?: 1.0f,
                sleepTimerMinutes = preferences[PreferencesKeys.SLEEP_TIMER_MINUTES] ?: 0,
                drivingModeEnabled = preferences[PreferencesKeys.DRIVING_MODE_ENABLED] ?: false,
                privateListeningMode = preferences[PreferencesKeys.PRIVATE_LISTENING_MODE] ?: false,
                pauseOnHeadsetDisconnect = preferences[PreferencesKeys.PAUSE_ON_HEADSET_DISCONNECT] ?: true,
                customJioSaavnApiUrl = preferences[PreferencesKeys.CUSTOM_JIOSAAVN_API_URL] ?: "",
                streamingAudioQuality = preferences[PreferencesKeys.STREAMING_AUDIO_QUALITY] ?: "320",
                playerSkin = try {
                    PlayerSkin.valueOf(preferences[PreferencesKeys.PLAYER_SKIN] ?: PlayerSkin.MODERN_GLASS.name)
                } catch (_: Exception) {
                    PlayerSkin.MODERN_GLASS
                },
                downloadQuality = preferences[PreferencesKeys.DOWNLOAD_QUALITY] ?: "320",
                downloadWifiOnly = preferences[PreferencesKeys.DOWNLOAD_WIFI_ONLY] ?: false,
                maxSimultaneousDownloads = preferences[PreferencesKeys.MAX_SIMULTANEOUS_DOWNLOADS] ?: 3
            )
        }

    suspend fun setPlayerSkin(skin: PlayerSkin) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYER_SKIN] = skin.name
        }
    }

    suspend fun setThemePreset(theme: ThemePreset) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PRESET] = theme.name
        }
    }

    suspend fun setAmoled(isAmoled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AMOLED] = isAmoled
        }
    }

    suspend fun setDynamicArtworkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_ARTWORK_THEME] = enabled
        }
    }

    suspend fun setCustomAccentColor(colorLong: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_ACCENT_COLOR] = colorLong
        }
    }

    suspend fun setWallpaperUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[PreferencesKeys.WALLPAPER_URI] = uri
            } else {
                preferences.remove(PreferencesKeys.WALLPAPER_URI)
            }
        }
    }

    suspend fun setWallpaperDim(dim: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WALLPAPER_DIM] = dim
        }
    }

    suspend fun setVinylAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VINYL_ANIMATION_ENABLED] = enabled
        }
    }

    suspend fun setFadeInEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FADE_IN_ENABLED] = enabled
        }
    }

    suspend fun setFadeInDurationMs(durationMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FADE_IN_DURATION_MS] = durationMs
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SLEEP_TIMER_MINUTES] = minutes
        }
    }

    suspend fun setDrivingModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DRIVING_MODE_ENABLED] = enabled
        }
    }

    suspend fun setPrivateListeningMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVATE_LISTENING_MODE] = enabled
        }
    }

    suspend fun setPauseOnHeadsetDisconnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAUSE_ON_HEADSET_DISCONNECT] = enabled
        }
    }

    suspend fun setCustomJioSaavnApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_JIOSAAVN_API_URL] = url.trim()
        }
    }

    suspend fun setStreamingAudioQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STREAMING_AUDIO_QUALITY] = quality.trim()
        }
    }

    suspend fun setDownloadQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_QUALITY] = quality.trim()
        }
    }

    suspend fun setDownloadWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun setMaxSimultaneousDownloads(max: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_SIMULTANEOUS_DOWNLOADS] = max.coerceIn(1, 5)
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}

