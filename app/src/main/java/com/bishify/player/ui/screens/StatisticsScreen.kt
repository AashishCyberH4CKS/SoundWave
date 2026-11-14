package com.bishify.player.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.ui.components.SoundWaveTopBar
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.LocalSoundWaveColors
import com.bishify.player.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun StatisticsScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalSoundWaveColors.current
    val totalTimeMs by viewModel.totalListeningTimeMs.collectAsState()
    val totalPlays by viewModel.totalPlays.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val scope = rememberCoroutineScope()

    val topArtists = remember(mostPlayedSongs) {
        mostPlayedSongs.map { it.artist }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    // Determine Listening Persona based on time of day
    val listeningPersona = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..5 -> "🌙 Night Owl"
            in 6..11 -> "🌅 Early Bird"
            in 12..17 -> "⚡ Midday Voyager"
            else -> "✨ Twilight Dreamer"
        }
    }

    val totalMs = totalTimeMs ?: 0L
    val totalHours = totalMs / (1000 * 60 * 60)
    val totalMinutes = (totalMs / (1000 * 60)) % 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SoundWaveTopBar(
            title = "SoundWave Wrapped",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SoundWave Wrapped Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.35f),
                                    Color(0xFF3B82F6).copy(alpha = 0.25f),
                                    Color.Black
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ YOUR MUSIC WRAPPED",
                                color = Color(0xFFA78BFA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )

                            // Share Wrapped Button
                            Button(
                                onClick = {
                                    val topTrackNames = mostPlayedSongs.take(3).mapIndexed { i, s -> "${i + 1}. ${s.title} - ${s.artist}" }.joinToString("\n")
                                    val shareText = """
                                        🌊 SoundWave Wrapped
                                        ---------------------------
                                        🎧 Persona: $listeningPersona
                                        ⏱️ Total Time: ${totalHours}h ${totalMinutes}m
                                        🔥 Total Plays: $totalPlays songs
                                        
                                        🏆 Top Tracks:
                                        $topTrackNames
                                        
                                        Crafted with SoundWave Music Player!
                                    """.trimIndent()

                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share SoundWave Wrapped"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = listeningPersona,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You've spent $totalHours hours and $totalMinutes minutes immersing in music across $totalPlays plays.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        if (topArtists.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Favorite Artists: ${topArtists.joinToString(", ")}",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

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
                            Text("Total Time", color = colors.textSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${totalHours}h ${totalMinutes}m",
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
                    text = "🏆 Top Most Played Songs",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (mostPlayedSongs.isEmpty()) {
                item {
                    Text("No plays recorded yet. Listen to songs to populate your Wrapped analytics.", color = colors.textSecondary, fontSize = 13.sp)
                }
            } else {
                itemsIndexed(mostPlayedSongs) { index, song ->
                    val medal = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "#${index + 1}"
                    }
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
                                text = medal,
                                color = if (index < 3) colors.primary else colors.textTertiary,
                                fontSize = if (index < 3) 20.sp else 14.sp,
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

