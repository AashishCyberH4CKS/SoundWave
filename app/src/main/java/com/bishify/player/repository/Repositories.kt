package com.bishify.player.repository

import android.content.Context
import com.bishify.player.local.PlaylistEntity
import com.bishify.player.local.PlaylistSongCrossRef
import com.bishify.player.local.SoundWaveDatabase
import com.bishify.player.mediastore.MediaStoreScanner
import com.bishify.player.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * LocalMusicRepository manages offline device music scanning and filtering.
 */
class LocalMusicRepository(
    private val context: Context,
    private val database: SoundWaveDatabase = SoundWaveDatabase.getInstance(context),
    private val scanner: MediaStoreScanner = MediaStoreScanner(context)
) {
    suspend fun scanLocalSongs(): List<Song> {
        val excluded = database.excludedFolderDao().getExcludedFolderPathsSync().toSet()
        return scanner.scanSongs(excluded)
    }

    fun observeMediaStoreChanges(): Flow<Unit> {
        return scanner.observeMediaStoreChanges()
    }

    suspend fun extractAlbums(songs: List<Song>): List<Album> = scanner.getAlbums(songs)
    suspend fun extractArtists(songs: List<Song>): List<Artist> = scanner.getArtists(songs)
    suspend fun extractFolders(songs: List<Song>): List<Folder> = scanner.getFolders(songs)
}

/**
 * PlaylistRepository manages user-created playlists and playlist tracks in Room.
 */
class PlaylistRepository(
    context: Context,
    private val database: SoundWaveDatabase = SoundWaveDatabase.getInstance(context)
) {
    val allPlaylists: Flow<List<PlaylistEntity>> = database.playlistDao().getAllPlaylists()

    suspend fun createPlaylist(name: String) {
        if (name.isBlank()) return
        database.playlistDao().insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun renamePlaylist(id: Long, newName: String) {
        if (newName.isBlank()) return
        val current = database.playlistDao().getPlaylistById(id) ?: return
        database.playlistDao().updatePlaylist(current.copy(name = newName.trim(), modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deletePlaylist(id: Long) {
        database.playlistDao().deletePlaylistById(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        database.playlistDao().insertPlaylistSong(PlaylistSongCrossRef(playlistId = playlistId, songId = songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        database.playlistDao().removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>> {
        return database.playlistDao().getSongIdsForPlaylist(playlistId)
    }
}
