package com.bishify.player.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dataPath: String,
    val albumId: Long,
    val artworkUri: String?,
    val dateAdded: Long,
    val trackNumber: Int = 0,
    val size: Long = 0L,
    val folderName: String = "",
    val isFavorite: Boolean = false
) {
    val formattedDuration: String
        get() = formatDuration(durationMs)

    companion object {
        fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0) return "0:00"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
    }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val year: Int = 0
)

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int = 1
)

data class Folder(
    val path: String,
    val name: String,
    val songCount: Int
)

enum class RepeatState {
    OFF, ALL, ONE
}

enum class SortOption {
    TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

