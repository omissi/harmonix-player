package com.alomessi.harmonix

import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.data.toAlbums
import com.alomessi.harmonix.data.toArtists
import com.alomessi.harmonix.data.toGenres
import com.alomessi.harmonix.data.TrackOverride
import com.alomessi.harmonix.data.withOverride
import com.alomessi.harmonix.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLogicTest {
    private fun track(id: Long, albumId: Long, album: String, artist: String) = Track(
        id, "Track $id", artist, album, albumId, 60_000, 100, "content://$id",
        null, "Music", "Music", id, id.toInt(), 2026, "Pop", "audio/mpeg",
    )

    @Test
    fun groupsAlbumsAndArtists() {
        val tracks = listOf(track(1, 10, "One", "Artist"), track(2, 10, "One", "Artist"))
        assertEquals(1, tracks.toAlbums().size)
        assertEquals(2, tracks.toAlbums().first().tracks.size)
        assertEquals(1, tracks.toArtists().size)
    }

    @Test
    fun durationFormattingIsStable() {
        assertEquals("3:05", formatDuration(185_000))
        assertEquals("1:01:01", formatDuration(3_661_000))
    }

    @Test
    fun groupsGenresAndAppliesLocalTags() {
        val original = track(1, 10, "One", "Artist")
        assertEquals(1, listOf(original).toGenres().size)
        val edited = original.withOverride(TrackOverride(title = "Edited", genre = "Jazz"))
        assertEquals("Edited", edited.title)
        assertEquals("Jazz", edited.genre)
    }
}
