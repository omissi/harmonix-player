package com.alomessi.harmonix.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    suspend fun scan(minimumDurationMs: Long, minimumSizeBytes: Long): List<Track> = withContext(Dispatchers.IO) {
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val tracks = mutableListOf<Track>()
        val legacyGenres = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) loadLegacyGenres() else emptyMap()
        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            "${MediaStore.Audio.Media.DURATION} >= ?",
        )
        val args = mutableListOf(minimumDurationMs.toString())
        if (minimumSizeBytes > 0L) {
            selectionParts += "${MediaStore.Audio.Media.SIZE} >= ?"
            args += minimumSizeBytes.toString()
        }
        val selection = selectionParts.joinToString(" AND ")
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC, ${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, args.toTypedArray(), sort)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else -1
            val relativeColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val filePath = if (dataColumn >= 0) cursor.getString(dataColumn).orEmpty() else ""
                val relativePath = if (relativeColumn >= 0) cursor.getString(relativeColumn).orEmpty() else ""
                val folderPath = relativePath.ifBlank { File(filePath).parent.orEmpty() }.trimEnd('/')
                val folderName = folderPath.substringAfterLast('/').ifBlank { "Music" }
                val contentUri = ContentUris.withAppendedId(collection, id)
                val artwork = if (albumId > 0) {
                    ContentUris.withAppendedId(
                        android.net.Uri.parse("content://media/external/audio/albumart"),
                        albumId,
                    ).toString()
                } else null

                tracks += Track(
                    id = id,
                    title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown title" },
                    artist = cursor.getString(artistColumn).orEmpty()
                        .takeUnless { it == "<unknown>" }
                        .orEmpty()
                        .ifBlank { "Unknown artist" },
                    album = cursor.getString(albumColumn).orEmpty()
                        .takeUnless { it == "<unknown>" }
                        .orEmpty()
                        .ifBlank { "Unknown album" },
                    albumId = albumId,
                    duration = cursor.getLong(durationColumn),
                    size = cursor.getLong(sizeColumn),
                    uri = contentUri.toString(),
                    artworkUri = artwork,
                    folder = folderName,
                    path = folderPath,
                    dateAdded = cursor.getLong(dateColumn) * 1_000L,
                    trackNumber = cursor.getInt(trackColumn) % 1_000,
                    year = cursor.getInt(yearColumn),
                    genre = if (genreColumn >= 0) cursor.getString(genreColumn).orEmpty().ifBlank { "Unknown genre" }
                    else legacyGenres[id].orEmpty().ifBlank { "Unknown genre" },
                    mimeType = cursor.getString(mimeColumn).orEmpty(),
                )
            }
        }
        tracks
    }

    private fun loadLegacyGenres(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val resolver = context.contentResolver
        val genreProjection = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
        runCatching {
            resolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, genreProjection, null, null, null)?.use { genres ->
                val idColumn = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameColumn = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genres.moveToNext()) {
                    val genreId = genres.getLong(idColumn)
                    val name = genres.getString(nameColumn).orEmpty().ifBlank { "Unknown genre" }
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    resolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null,
                        null,
                        null,
                    )?.use { members ->
                        val audioIdColumn = members.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (members.moveToNext()) result[members.getLong(audioIdColumn)] = name
                    }
                }
            }
        }
        return result
    }
}
