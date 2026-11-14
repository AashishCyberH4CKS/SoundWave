package com.bishify.player.online

import com.bishify.player.model.Song

data class TrackDetailedMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationFormatted: String,
    val isOnline: Boolean,
    val sourceName: String,
    val genre: String,
    val artworkUrl: String?
)

/**
 * TrackMetadataRepository produces unified metadata summaries for playback and info dialogs.
 */
class TrackMetadataRepository {

    fun getMetadata(song: Song): TrackDetailedMetadata {
        return TrackDetailedMetadata(
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationFormatted = song.formattedDuration,
            isOnline = song.isOnline,
            sourceName = when (song.sourceId) {
                "itunes" -> "Apple Music / iTunes"
                "archive" -> "Internet Archive (Open)"
                else -> "Local Device Storage"
            },
            genre = if (song.genre.isNotBlank()) song.genre else if (song.isOnline) "Online" else "Local",
            artworkUrl = song.artworkUri
        )
    }
}

