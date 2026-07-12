package com.alomessi.harmonix.data

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val size: Long,
    val uri: String,
    val artworkUri: String?,
    val folder: String,
    val path: String,
    val dateAdded: Long,
    val trackNumber: Int,
    val year: Int,
    val genre: String,
    val mimeType: String,
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val artworkUri: String?,
    val tracks: List<Track>,
) {
    val duration: Long get() = tracks.sumOf { it.duration }
}

data class Artist(
    val name: String,
    val tracks: List<Track>,
) {
    val albums: Int get() = tracks.map { it.album }.distinct().size
}

data class MusicFolder(
    val name: String,
    val path: String,
    val tracks: List<Track>,
)

data class Genre(
    val name: String,
    val tracks: List<Track>,
)

data class UserPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<Long>,
    val createdAt: Long,
)

data class TrackOverride(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val artworkUri: String? = null,
)

data class LastPlayback(
    val queueIds: List<Long> = emptyList(),
    val mediaId: Long? = null,
    val position: Long = 0L,
)

data class AppSettings(
    val language: String = "ar",
    val theme: String = "system",
    val accent: String = "violet",
    val minimumDurationMs: Long = 10_000L,
    val minimumSizeBytes: Long = 0L,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val rememberLastSong: Boolean = true,
    val albumGrid: Boolean = true,
)

fun List<Track>.toAlbums(): List<Album> =
    groupBy { it.albumId to it.album }
        .map { (key, tracks) ->
            Album(
                id = key.first,
                name = key.second,
                artist = tracks.firstOrNull()?.artist.orEmpty(),
                artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
                tracks = tracks.sortedWith(compareBy<Track> { it.trackNumber }.thenBy { it.title }),
            )
        }
        .sortedBy { it.name.lowercase() }

fun List<Track>.toArtists(): List<Artist> =
    groupBy { it.artist }
        .map { Artist(it.key, it.value.sortedBy(Track::title)) }
        .sortedBy { it.name.lowercase() }

fun List<Track>.toFolders(): List<MusicFolder> =
    groupBy { it.path.ifBlank { it.folder } }
        .map { (path, tracks) ->
            MusicFolder(
                name = tracks.firstOrNull()?.folder?.ifBlank { "Music" } ?: "Music",
                path = path,
                tracks = tracks.sortedBy(Track::title),
            )
        }
        .sortedBy { it.name.lowercase() }

fun List<Track>.toGenres(): List<Genre> =
    groupBy { it.genre.ifBlank { "Unknown genre" } }
        .map { Genre(it.key, it.value.sortedBy(Track::title)) }
        .sortedBy { it.name.lowercase() }

fun Track.withOverride(override: TrackOverride?): Track {
    if (override == null) return this
    return copy(
        title = override.title?.takeIf(String::isNotBlank) ?: title,
        artist = override.artist?.takeIf(String::isNotBlank) ?: artist,
        album = override.album?.takeIf(String::isNotBlank) ?: album,
        genre = override.genre?.takeIf(String::isNotBlank) ?: genre,
        artworkUri = override.artworkUri?.takeIf(String::isNotBlank) ?: artworkUri,
    )
}
