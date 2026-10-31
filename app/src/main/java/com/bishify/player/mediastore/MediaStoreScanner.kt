package com.bishify.player.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.bishify.player.model.Album
import com.bishify.player.model.Artist
import com.bishify.player.model.Folder
import com.bishify.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    private val artworkUriBase = Uri.parse("content://media/external/audio/albumart")

    suspend fun scanSongs(excludedFolders: Set<String> = emptySet()): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "<unknown>"
                    val cleanArtist = if (artist.equals("<unknown>", ignoreCase = true)) "Unknown Artist" else artist
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val dataPath = cursor.getString(dataCol) ?: ""
                    val albumId = cursor.getLong(albumIdCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val track = cursor.getInt(trackCol)
                    val size = cursor.getLong(sizeCol)

                    // Check excluded folders
                    val file = File(dataPath)
                    val parentFolder = file.parent ?: ""
                    val folderName = file.parentFile?.name ?: "Music"

                    val isExcluded = excludedFolders.any { excluded ->
                        dataPath.startsWith(excluded, ignoreCase = true)
                    }

                    if (!isExcluded && (dataPath.isNotEmpty())) {
                        val artworkUri = ContentUris.withAppendedId(artworkUriBase, albumId).toString()

                        songsList.add(
                            Song(
                                id = id,
                                title = title,
                                artist = cleanArtist,
                                album = album,
                                durationMs = duration,
                                dataPath = dataPath,
                                albumId = albumId,
                                artworkUri = artworkUri,
                                dateAdded = dateAdded,
                                trackNumber = track,
                                size = size,
                                folderName = folderName
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songsList
    }

    suspend fun getAlbums(songs: List<Song>): List<Album> = withContext(Dispatchers.Default) {
        songs.groupBy { it.albumId }
            .map { (albumId, songsInAlbum) ->
                val first = songsInAlbum.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = first.artist,
                    artworkUri = first.artworkUri,
                    songCount = songsInAlbum.size
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    suspend fun getArtists(songs: List<Song>): List<Artist> = withContext(Dispatchers.Default) {
        songs.groupBy { it.artist.lowercase() }
            .map { (_, songsByArtist) ->
                val first = songsByArtist.first()
                val albumCount = songsByArtist.map { it.albumId }.distinct().size
                Artist(
                    id = first.id,
                    name = first.artist,
                    songCount = songsByArtist.size,
                    albumCount = albumCount
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun getFolders(songs: List<Song>): List<Folder> = withContext(Dispatchers.Default) {
        songs.groupBy { File(it.dataPath).parent ?: "" }
            .filter { it.key.isNotEmpty() }
            .map { (parentPath, songsInFolder) ->
                Folder(
                    path = parentPath,
                    name = File(parentPath).name,
                    songCount = songsInFolder.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun observeMediaStoreChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
}

