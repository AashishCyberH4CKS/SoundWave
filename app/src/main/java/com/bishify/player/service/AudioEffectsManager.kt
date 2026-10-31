package com.bishify.player.service

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val minLevel: Short,
    val maxLevel: Short,
    var currentLevel: Short
)

data class AudioEffectsState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Short = 0,
    val virtualizerStrength: Short = 0,
    val currentPreset: String = "Flat",
    val availablePresets: List<String> = emptyList(),
    val isSupported: Boolean = true
)

class AudioEffectsManager(private val context: Context) {

    private val tag = "AudioEffectsManager"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var currentSessionId: Int = 0

    var isEnabled: Boolean = false
        private set

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = this@AudioEffectsManager.isEnabled
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to initialize Equalizer: ${e.message}")
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = this@AudioEffectsManager.isEnabled
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to initialize BassBoost: ${e.message}")
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = this@AudioEffectsManager.isEnabled
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to initialize Virtualizer: ${e.message}")
            virtualizer = null
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(tag, "Error setting effect enabled: ${e.message}")
        }
    }

    fun getBands(): List<EqualizerBand> {
        val eq = equalizer ?: return emptyList()
        val list = mutableListOf<EqualizerBand>()
        try {
            val numBands = eq.numberOfBands
            val levelRange = eq.bandLevelRange ?: shortArrayOf(-1500, 1500)
            val min = levelRange[0]
            val max = levelRange[1]

            for (i in 0 until numBands) {
                val band = i.toShort()
                val centerFreq = eq.getCenterFreq(band) / 1000 // Convert mHz to Hz
                val current = eq.getBandLevel(band)
                list.add(EqualizerBand(band, centerFreq, min, max, current))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting bands: ${e.message}")
        }
        return list
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (e: Exception) {
            Log.e(tag, "Error setting band level: ${e.message}")
        }
    }

    fun setBassBoost(strength: Short) {
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error setting bass boost: ${e.message}")
        }
    }

    fun getBassBoostStrength(): Short {
        return try {
            bassBoost?.roundedStrength ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun setVirtualizer(strength: Short) {
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error setting virtualizer: ${e.message}")
        }
    }

    fun getVirtualizerStrength(): Short {
        return try {
            virtualizer?.roundedStrength ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getPresets(): List<String> {
        val eq = equalizer ?: return listOf("Flat", "Rock", "Pop", "Jazz", "Classical")
        val presets = mutableListOf<String>()
        try {
            val num = eq.numberOfPresets
            for (i in 0 until num) {
                presets.add(eq.getPresetName(i.toShort()))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reading presets: ${e.message}")
        }
        if (presets.isEmpty()) {
            return listOf("Flat", "Rock", "Pop", "Jazz", "Classical")
        }
        return presets
    }

    fun usePreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
        } catch (e: Exception) {
            Log.e(tag, "Error using preset: ${e.message}")
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing audio effects: ${e.message}")
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            currentSessionId = 0
        }
    }
}

