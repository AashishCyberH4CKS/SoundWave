package com.bishify.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.lyrics.LyricLine
import com.bishify.player.lyrics.SongLyrics
import kotlinx.coroutines.delay

@Composable
fun LyricsView(
    lyrics: SongLyrics?,
    isLoading: Boolean,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Determine current active lyric index based on playback position
    val activeIndex = remember(lyrics, currentPositionMs) {
        if (lyrics == null || !lyrics.isSynced || lyrics.lines.isEmpty()) -1
        else {
            val idx = lyrics.lines.indexOfLast { it.timeMs <= currentPositionMs }
            if (idx == -1) 0 else idx
        }
    }

    // Auto-scroll to active lyric smoothly
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyrics?.lines?.isNotEmpty() == true) {
            val scrollTarget = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(scrollTarget)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lyrics?.isSynced == true) "Synchronized Lyrics" else "Lyrics",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Lyrics",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Matching real-time lyrics...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (lyrics == null || (lyrics.lines.isEmpty() && lyrics.plainLyrics.isNullOrBlank())) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No lyrics found for this track.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (lyrics.isSynced && lyrics.lines.isNotEmpty()) {
                // Synced Karaoke Mode
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 40.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        val isActive = index == activeIndex
                        val isPast = index < activeIndex

                        Text(
                            text = line.text.ifBlank { "♪ ♪ ♪" },
                            fontSize = if (isActive) 22.sp else 17.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = when {
                                isActive -> Color.White
                                isPast -> Color.White.copy(alpha = 0.45f)
                                else -> Color.White.copy(alpha = 0.28f)
                            },
                            lineHeight = 28.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSeekTo(line.timeMs)
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        )
                    }
                }
            } else {
                // Plain Lyrics Mode
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp)
                ) {
                    item {
                        Text(
                            text = lyrics.plainLyrics ?: "",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

