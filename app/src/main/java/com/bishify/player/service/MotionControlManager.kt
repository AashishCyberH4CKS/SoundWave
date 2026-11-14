package com.bishify.player.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.media3.common.Player
import kotlin.math.sqrt

/**
 * Handles Motion Gestures:
 * 1. Flip to Pause: When phone is flipped face down on a desk, pause music immediately.
 * 2. Shake to Skip: When phone is shaken (in pocket or in hand), skip to the next track.
 */
class MotionControlManager(
    private val context: Context,
    private val player: Player,
    var flipToPauseEnabled: Boolean = true,
    var shakeToSkipEnabled: Boolean = true
) : SensorEventListener {

    private val tag = "MotionControlManager"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var lastShakeTime = 0L
    private var lastFlipTime = 0L
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var shakeAcceleration = 0f
    private var isProximityNear = false
    private var isRegistered = false

    fun startListening() {
        if (isRegistered || sensorManager == null) return
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        isRegistered = true
    }

    fun stopListening() {
        if (!isRegistered || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = event.sensor.maximumRange
                isProximityNear = event.values[0] < maxRange
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val now = System.currentTimeMillis()

                // 1. Flip Facedown Detection (Phone screen facing downward: Z-axis is negative, e.g. < -7.0)
                if (flipToPauseEnabled && player.isPlaying && z < -7.0f) {
                    if (now - lastFlipTime > 1500L) {
                        lastFlipTime = now
                        Log.d(tag, "Flip to Pause triggered: z=$z")
                        player.pause()
                    }
                }

                // 2. Shake to Skip Detection
                if (shakeToSkipEnabled && player.playbackState != Player.STATE_IDLE) {
                    lastAcceleration = currentAcceleration
                    currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    val delta = currentAcceleration - lastAcceleration
                    shakeAcceleration = shakeAcceleration * 0.9f + delta

                    // Threshold ~12f for intentional shake
                    if (shakeAcceleration > 11.5f && (now - lastShakeTime > 2000L)) {
                        lastShakeTime = now
                        Log.d(tag, "Shake to Skip triggered: accel=$shakeAcceleration")
                        if (player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                        } else {
                            player.seekTo(0, 0L)
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

