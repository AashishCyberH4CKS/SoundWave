package com.bishify.player.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.model.Song
import com.bishify.player.ui.components.SoundWaveTopBar
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.LocalSoundWaveColors
import com.bishify.player.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@Composable
fun RingtoneCutterScreen(
    song: Song,
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundWaveColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackManager = viewModel.playbackManager

    val totalMs = song.durationMs.coerceAtLeast(5000L)
    var startMs by remember { mutableFloatStateOf(0f) }
    var endMs by remember { mutableFloatStateOf(30000f.coerceAtMost(totalMs.toFloat())) }
    var ringtoneName by remember { mutableStateOf("${song.title} Ringtone") }

    var isExporting by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SoundWaveTopBar(
            title = "Ringtone Cutter",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Track Info Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.surfaceElevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = song.title,
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${song.artist} • Total: ${Song.formatDuration(totalMs)}",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Trimming Controls Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.surfaceElevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Selected Duration: ${((endMs - startMs) / 1000).toInt()}s",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${Song.formatDuration(startMs.toLong())} -> ${Song.formatDuration(endMs.toLong())}",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Start Position: ${Song.formatDuration(startMs.toLong())}", color = colors.textPrimary, fontSize = 12.sp)
                    Slider(
                        value = startMs,
                        onValueChange = { newStart ->
                            startMs = newStart.coerceAtMost(endMs - 1000f)
                            if (endMs - startMs > 30000f) {
                                endMs = startMs + 30000f
                            }
                        },
                        valueRange = 0f..totalMs.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("End Position: ${Song.formatDuration(endMs.toLong())}", color = colors.textPrimary, fontSize = 12.sp)
                    Slider(
                        value = endMs,
                        onValueChange = { newEnd ->
                            endMs = newEnd.coerceAtLeast(startMs + 1000f)
                            if (endMs - startMs > 30000f) {
                                startMs = (endMs - 30000f).coerceAtLeast(0f)
                            }
                        },
                        valueRange = 0f..totalMs.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = colors.secondary, activeTrackColor = colors.secondary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preview Button
                    OutlinedButton(
                        onClick = {
                            playbackManager.playSingle(song)
                            playbackManager.seekTo(startMs.toLong())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview Selection")
                    }
                }
            }

            // Ringtone Filename Input
            OutlinedTextField(
                value = ringtoneName,
                onValueChange = { ringtoneName = it },
                label = { Text("Ringtone Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            // Export Action Button
            Button(
                onClick = {
                    isExporting = true
                    errorMessage = null
                    scope.launch {
                        val result = viewModel.cutRingtone(
                            sourcePath = song.dataPath,
                            outputName = ringtoneName,
                            startMs = startMs.toLong(),
                            endMs = endMs.toLong()
                        )
                        isExporting = false
                        result.onSuccess { uri ->
                            exportedUri = uri
                            Toast.makeText(context, "Ringtone exported successfully!", Toast.LENGTH_LONG).show()
                        }.onFailure { err ->
                            errorMessage = err.message ?: "Failed to export ringtone"
                        }
                    }
                },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.ContentCut, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Ringtone Audio", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Exported Ringtone Actions
            if (exportedUri != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ringtone Saved to Device Ringtones",
                            color = Color(0xFF4BB543),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!Settings.System.canWrite(context)) {
                                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                            data = Uri.parse("package:" + context.packageName)
                                        }
                                        context.startActivity(intent)
                                        Toast.makeText(context, "Grant permission to modify ringtones", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                }
                                try {
                                    RingtoneManager.setActualDefaultRingtoneUri(
                                        context,
                                        RingtoneManager.TYPE_RINGTONE,
                                        exportedUri
                                    )
                                    Toast.makeText(context, "Set as default ringtone!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error setting ringtone: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set as Default Phone Ringtone")
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = colors.secondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
