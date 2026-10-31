package com.bishify.player.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY modifiedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(crossRefs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getSongCount(playlistId: Long): Flow<Int>
}

@Dao
interface FavoriteSongDao {
    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    fun getFavoriteSongIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavorite(songId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavoriteSync(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteSongEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun recordHistory(item: ListeningHistoryEntity)

    @Query("SELECT songId FROM listening_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentlyPlayedSongIds(limit: Int = 50): Flow<List<Long>>

    @Query("SELECT songId, COUNT(*) as playCount FROM listening_history GROUP BY songId ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongIds(limit: Int = 50): Flow<List<SongPlayCount>>

    @Query("SELECT COUNT(*) FROM listening_history")
    fun getTotalPlays(): Flow<Int>

    @Query("SELECT SUM(durationPlayedMs) FROM listening_history")
    fun getTotalListeningTimeMs(): Flow<Long?>

    @Query("DELETE FROM listening_history")
    suspend fun clearHistory()
}

data class SongPlayCount(
    val songId: Long,
    val playCount: Int
)

@Dao
interface ExcludedFolderDao {
    @Query("SELECT path FROM excluded_folders")
    fun getExcludedFolderPaths(): Flow<List<String>>

    @Query("SELECT path FROM excluded_folders")
    suspend fun getExcludedFolderPathsSync(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun excludeFolder(item: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE path = :path")
    suspend fun includeFolder(path: String)
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM playback_queue ORDER BY orderIndex ASC")
    suspend fun getSavedQueue(): List<QueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueue(items: List<QueueItemEntity>)

    @Query("DELETE FROM playback_queue")
    suspend fun clearQueue()

    @Query("SELECT * FROM saved_queue_state WHERE id = 1")
    suspend fun getSavedState(): SavedQueueState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: SavedQueueState)
}

