package com.bishify.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.ui.components.SoundWaveTopBar
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.LocalSoundWaveColors
import com.bishify.player.ui.viewmodel.MusicViewModel

@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundWaveColors.current
    val playbackManager = viewModel.playbackManager

    var eqEnabled by remember { mutableStateOf(true) }
    var bassBoost by remember { mutableFloatStateOf(0f) }
    var virtualizer by remember { mutableFloatStateOf(0f) }

    // Standard 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bandNames = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val bandLevels = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }

    var selectedPreset by remember { mutableStateOf("Flat") }
    val presets = listOf("Flat", "Rock", "Pop", "Jazz", "Classical", "Bass Boost")

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SoundWaveTopBar(
            title = "Equalizer & Effects",
            onBackClick = onBackClick,
            actions = {
                IconButton(onClick = {
                    for (i in 0 until bandLevels.size) {
                        bandLevels[i] = 0f
                        playbackManager.sendEqBand(i.toShort(), 0)
                    }
                    bassBoost = 0f
                    virtualizer = 0f
                    playbackManager.sendBassBoost(0)
                    playbackManager.sendVirtualizer(0)
                    selectedPreset = "Flat"
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = colors.textSecondary
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Toggle Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Audio Enhancements",
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (eqEnabled) "Active on audio session" else "Disabled",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = {
                                eqEnabled = it
                                playbackManager.sendEqEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.primary,
                                checkedTrackColor = colors.primary.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            // Presets Selection
            item {
                Text(
                    text = "Presets",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.take(3).forEach { preset ->
                        PresetButton(
                            name = preset,
                            isSelected = selectedPreset == preset,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedPreset = preset
                                applyPresetLevels(preset, bandLevels, onApply = { b, l ->
                                    playbackManager.sendEqBand(b, l)
                                })
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.drop(3).forEach { preset ->
                        PresetButton(
                            name = preset,
                            isSelected = selectedPreset == preset,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedPreset = preset
                                applyPresetLevels(preset, bandLevels, onApply = { b, l ->
                                    playbackManager.sendEqBand(b, l)
                                })
                            }
                        )
                    }
                }
            }

            // Equalizer Frequency Bands
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "5-Band Equalizer",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        bandNames.forEachIndexed { index, freqName ->
                            val level = bandLevels.getOrElse(index) { 0f }
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(freqName, color = colors.textPrimary, fontSize = 13.sp)
                                    val dB = (level * 15f).toInt()
                                    Text(
                                        "${if (dB > 0) "+$dB" else "$dB"} dB",
                                        color = if (dB != 0) colors.primary else colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Slider(
                                    value = level,
                                    onValueChange = { newLevel ->
                                        bandLevels[index] = newLevel
                                        selectedPreset = "Custom"
                                        val millibels = (newLevel * 1500).toInt().toShort()
                                        playbackManager.sendEqBand(index.toShort(), millibels)
                                    },
                                    valueRange = -1f..1f,
                                    enabled = eqEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.primary,
                                        activeTrackColor = colors.primary,
                                        inactiveTrackColor = colors.surface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bass Boost & Virtualizer Sliders
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Bass Boost
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bass Boost", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(bassBoost * 100).toInt()}%", color = colors.primary, fontSize = 13.sp)
                        }
                        Slider(
                            value = bassBoost,
                            onValueChange = {
                                bassBoost = it
                                val strength = (it * 1000).toInt().toShort()
                                playbackManager.sendBassBoost(strength)
                            },
                            enabled = eqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Virtualizer 3D Surround
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("3D Surround (Virtualizer)", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(virtualizer * 100).toInt()}%", color = colors.primary, fontSize = 13.sp)
                        }
                        Slider(
                            value = virtualizer,
                            onValueChange = {
                                virtualizer = it
                                val strength = (it * 1000).toInt().toShort()
                                playbackManager.sendVirtualizer(strength)
                            },
                            enabled = eqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surface
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetButton(
    name: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalSoundWaveColors.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) colors.primary else colors.surfaceElevated,
            contentColor = if (isSelected) androidx.compose.ui.graphics.Color.White else colors.textPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = modifier
    ) {
        Text(name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun applyPresetLevels(
    preset: String,
    bandLevels: androidx.compose.runtime.snapshots.SnapshotStateList<Float>,
    onApply: (Short, Short) -> Unit
) {
    val levels = when (preset) {
        "Rock" -> listOf(0.4f, 0.2f, -0.1f, 0.3f, 0.5f)
        "Pop" -> listOf(-0.1f, 0.2f, 0.4f, 0.2f, -0.1f)
        "Jazz" -> listOf(0.3f, 0.1f, -0.1f, 0.2f, 0.3f)
        "Classical" -> listOf(0.3f, 0.2f, -0.1f, 0.2f, 0.4f)
        "Bass Boost" -> listOf(0.8f, 0.5f, 0.1f, 0.0f, 0.0f)
        else -> listOf(0f, 0f, 0f, 0f, 0f)
    }

    for (i in 0 until bandLevels.size) {
        val lvl = levels.getOrElse(i) { 0f }
        bandLevels[i] = lvl
        onApply(i.toShort(), (lvl * 1500).toInt().toShort())
    }
}

