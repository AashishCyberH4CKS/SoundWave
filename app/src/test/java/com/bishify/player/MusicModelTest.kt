package com.bishify.player

import com.bishify.player.model.RepeatState
import com.bishify.player.model.Song
import com.bishify.player.model.SortDirection
import com.bishify.player.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicModelTest {

    @Test
    fun testSongDurationFormatting() {
        assertEquals("0:00", Song.formatDuration(0L))
        assertEquals("0:05", Song.formatDuration(5000L))
        assertEquals("1:00", Song.formatDuration(60000L))
        assertEquals("3:45", Song.formatDuration(225000L))
        assertEquals("1:01:05", Song.formatDuration(3665000L))
    }

    @Test
    fun testSongPropertiesAndCopy() {
        val song = Song(
            id = 101L,
            title = "Midnight Echoes",
            artist = "SoundWave Sounds",
            album = "Offline Journey",
            durationMs = 180000L,
            dataPath = "/storage/emulated/0/Music/track.mp3",
            albumId = 55L,
            artworkUri = "content://media/external/audio/albumart/55",
            dateAdded = 1600000000L,
            trackNumber = 1,
            size = 8000000L,
            folderName = "Music",
            isFavorite = false
        )

        assertEquals("Midnight Echoes", song.title)
        assertEquals("3:00", song.formattedDuration)
        assertFalse(song.isFavorite)

        val favoriteSong = song.copy(isFavorite = true)
        assertTrue(favoriteSong.isFavorite)
        assertEquals(song.id, favoriteSong.id)
    }

    @Test
    fun testRepeatStateCycles() {
        val states = listOf(RepeatState.OFF, RepeatState.ALL, RepeatState.ONE)
        assertEquals(3, states.size)
        assertEquals(RepeatState.OFF, states[0])
        assertEquals(RepeatState.ALL, states[1])
        assertEquals(RepeatState.ONE, states[2])
    }

    @Test
    fun testSortingLogic() {
        val songA = Song(1L, "Apple", "Artist B", "Album C", 1000L, "/a.mp3", 1L, null, 100L)
        val songB = Song(2L, "Banana", "Artist A", "Album B", 2000L, "/b.mp3", 2L, null, 200L)
        val songC = Song(3L, "Cherry", "Artist C", "Album A", 3000L, "/c.mp3", 3L, null, 300L)

        val list = listOf(songC, songA, songB)

        val byTitle = list.sortedBy { it.title }
        assertEquals("Apple", byTitle[0].title)
        assertEquals("Banana", byTitle[1].title)
        assertEquals("Cherry", byTitle[2].title)

        val byArtist = list.sortedBy { it.artist }
        assertEquals("Artist A", byArtist[0].artist)
        assertEquals("Artist B", byArtist[1].artist)
        assertEquals("Artist C", byArtist[2].artist)

        val byDurationDesc = list.sortedByDescending { it.durationMs }
        assertEquals(3000L, byDurationDesc[0].durationMs)
        assertEquals(2000L, byDurationDesc[1].durationMs)
        assertEquals(1000L, byDurationDesc[2].durationMs)
    }
}

