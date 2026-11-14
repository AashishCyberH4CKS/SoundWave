package com.bishify.player.online

import com.bishify.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MusicGenreChannel(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String
)

/**
 * MusicCatalogueRepository provides curated categories, top trending charts,
 * and genre-based discovery channels from legal online catalogues.
 */
class MusicCatalogueRepository(
    val primaryProvider: StreamingSourceProvider = JioSaavnSourceProvider()
) {
    val supportedGenres = listOf(
        MusicGenreChannel("pop", "Pop", "Catchy modern hits and chart-toppers", "🔥"),
        MusicGenreChannel("rock", "Rock", "Classic rock, alternative and modern riffs", "🎸"),
        MusicGenreChannel("hiphop", "Hip-Hop", "Beats, rhythm, lyrical flows and urban vibes", "🎤"),
        MusicGenreChannel("electronic", "Electronic / EDM", "Dance grooves, house, synth and trance", "⚡"),
        MusicGenreChannel("rnb", "R&B / Soul", "Smooth vocals, soulful melodies and groove", "✨"),
        MusicGenreChannel("ambient", "Ambient / Chill", "Relaxing soundscapes, lo-fi and meditation", "🌙"),
        MusicGenreChannel("classical", "Classical", "Timeless orchestral masterpieces and piano", "🎻"),
        MusicGenreChannel("jazz", "Jazz", "Smooth brass, improvisation and swing", "🎷")
    )

    suspend fun getTrendingTracks(limit: Int = 30): Result<List<Song>> = withContext(Dispatchers.IO) {
        primaryProvider.getTrendingTracks(limit)
    }

    suspend fun getTracksByGenre(genre: String, limit: Int = 25): Result<List<Song>> = withContext(Dispatchers.IO) {
        primaryProvider.getTracksByGenre(genre, limit)
    }
}

