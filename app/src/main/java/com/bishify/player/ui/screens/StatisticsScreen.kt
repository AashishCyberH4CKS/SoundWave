package com.bishify.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.local.SoundWaveDatabase
import com.bishify.player.ui.components.SoundWaveTopBar
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.LocalSoundWaveColors
import com.bishify.player.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun StatisticsScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundWaveColors.current
    val totalTimeMs by viewModel.totalListeningTimeMs.collectAsState()
    val totalPlays by viewModel.totalPlays.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SoundWaveTopBar(
            title = "Listening Analytics",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = colors.surfaceElevated
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Plays", color = colors.textSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalPlays",
                                color = colors.primary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = colors.surfaceElevated
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Time Listened", color = colors.textSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            val ms = totalTimeMs ?: 0L
                            val hours = ms / (1000 * 60 * 60)
                            val minutes = (ms / (1000 * 60)) % 60
                            Text(
                                text = "${hours}h ${minutes}m",
                                color = colors.primary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Private Mode Info
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Private Mode", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Playback is not recorded to statistics", color = colors.textSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = preferences.privateListeningMode,
                            onCheckedChange = {
                                scope.launch { viewModel.preferencesManager.setPrivateListeningMode(it) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.primary,
                                checkedTrackColor = colors.primary.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            // Most Played Leaderboard
            item {
                Text(
                    text = "Most Played Songs",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (mostPlayedSongs.isEmpty()) {
                item {
                    Text("No plays recorded yet. Listen to songs to populate analytics.", color = colors.textSecondary, fontSize = 13.sp)
                }
            } else {
                itemsIndexed(mostPlayedSongs) { index, song ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.surfaceElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${index + 1}",
                                color = if (index < 3) colors.primary else colors.textTertiary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

