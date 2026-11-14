package com.bishify.player.online

import android.net.Uri
import com.bishify.player.model.Song

/**
 * ArtworkRepository manages high-definition artwork resolution and transformations.
 */
class ArtworkRepository {

    /**
     * Ensures artwork URI is upgraded to high definition (e.g. 600x600 for iTunes).
     */
    fun getHighResArtworkUri(song: Song): String? {
        val raw = song.artworkUri ?: return null
        return if (raw.contains("100x100bb") || raw.contains("170x170bb")) {
            raw.replace("100x100bb", "600x600bb").replace("170x170bb", "600x600bb")
        } else {
            raw
        }
    }
}

