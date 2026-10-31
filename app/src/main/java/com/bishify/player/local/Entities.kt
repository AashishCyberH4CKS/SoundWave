package com.bishify.player.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val coverUri: String? = null
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val sortOrder: Int = 0
)

@Entity(tableName = "favorites")
data class FavoriteSongEntity(
    @PrimaryKey
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "listening_history")
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val durationPlayedMs: Long = 0,
    val completed: Boolean = false
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey
    val path: String
)

@Entity(tableName = "playback_queue")
data class QueueItemEntity(
    @PrimaryKey
    val songId: Long,
    val orderIndex: Int
)

@Entity(tableName = "saved_queue_state")
data class SavedQueueState(
    @PrimaryKey
    val id: Int = 1,
    val currentSongId: Long? = null,
    val currentPositionMs: Long = 0,
    val repeatMode: Int = 0,
    val isShuffle: Boolean = false
)

