package com.alomessi.harmonix.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class UserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("harmonix_preferences", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _favorites = MutableStateFlow(readLongSet(KEY_FAVORITES))
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow(readPlaylists())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow(readLongList(KEY_RECENT))
    val recentlyPlayed: StateFlow<List<Long>> = _recentlyPlayed.asStateFlow()

    private val _playCounts = MutableStateFlow(readCounts())
    val playCounts: StateFlow<Map<Long, Int>> = _playCounts.asStateFlow()

    private val _hiddenTrackIds = MutableStateFlow(readLongSet(KEY_HIDDEN))
    val hiddenTrackIds: StateFlow<Set<Long>> = _hiddenTrackIds.asStateFlow()

    private val _trackOverrides = MutableStateFlow(readTrackOverrides())
    val trackOverrides: StateFlow<Map<Long, TrackOverride>> = _trackOverrides.asStateFlow()

    private val _lyrics = MutableStateFlow(readStringMap(KEY_LYRICS))
    val lyrics: StateFlow<Map<Long, String>> = _lyrics.asStateFlow()

    private val _lastPlayback = MutableStateFlow(readLastPlayback())
    val lastPlayback: StateFlow<LastPlayback> = _lastPlayback.asStateFlow()

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        preferences.edit().putString(KEY_SETTINGS, updated.toJson().toString()).apply()
    }

    fun toggleFavorite(trackId: Long) {
        val updated = _favorites.value.toMutableSet().apply {
            if (!add(trackId)) remove(trackId)
        }
        _favorites.value = updated
        writeLongSet(KEY_FAVORITES, updated)
    }

    fun hideTrack(trackId: Long) {
        val updated = _hiddenTrackIds.value + trackId
        _hiddenTrackIds.value = updated
        writeLongSet(KEY_HIDDEN, updated)
    }

    fun unhideTrack(trackId: Long) {
        val updated = _hiddenTrackIds.value - trackId
        _hiddenTrackIds.value = updated
        writeLongSet(KEY_HIDDEN, updated)
    }

    fun saveTrackOverride(trackId: Long, override: TrackOverride) {
        val normalized = override.copy(
            title = override.title?.trim()?.takeIf(String::isNotBlank),
            artist = override.artist?.trim()?.takeIf(String::isNotBlank),
            album = override.album?.trim()?.takeIf(String::isNotBlank),
            genre = override.genre?.trim()?.takeIf(String::isNotBlank),
            artworkUri = override.artworkUri?.trim()?.takeIf(String::isNotBlank),
        )
        val updated = _trackOverrides.value.toMutableMap().apply { put(trackId, normalized) }
        _trackOverrides.value = updated
        writeTrackOverrides(updated)
    }

    fun setCustomArtwork(trackId: Long, artworkUri: String) {
        val existing = _trackOverrides.value[trackId] ?: TrackOverride()
        saveTrackOverride(trackId, existing.copy(artworkUri = artworkUri))
    }

    fun resetTrackOverride(trackId: Long) {
        val updated = _trackOverrides.value - trackId
        _trackOverrides.value = updated
        writeTrackOverrides(updated)
    }

    fun saveLyrics(trackId: Long, text: String) {
        val cleaned = text.trim().take(100_000)
        val updated = _lyrics.value.toMutableMap().apply {
            if (cleaned.isBlank()) remove(trackId) else put(trackId, cleaned)
        }
        _lyrics.value = updated
        preferences.edit().putString(KEY_LYRICS, JSONObject().apply {
            updated.forEach { (id, lyrics) -> put(id.toString(), lyrics) }
        }.toString()).apply()
    }

    fun saveLastPlayback(queueIds: List<Long>, mediaId: Long?, position: Long) {
        val value = LastPlayback(queueIds.take(500), mediaId, position.coerceAtLeast(0L))
        if (value == _lastPlayback.value) return
        _lastPlayback.value = value
        preferences.edit().putString(KEY_LAST_PLAYBACK, JSONObject().apply {
            put("queueIds", JSONArray(value.queueIds))
            if (value.mediaId != null) put("mediaId", value.mediaId)
            put("position", value.position)
        }.toString()).apply()
    }

    fun createPlaylist(name: String): UserPlaylist? {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return null
        val playlist = UserPlaylist(UUID.randomUUID().toString(), cleanName, emptyList(), System.currentTimeMillis())
        savePlaylists(_playlists.value + playlist)
        return playlist
    }

    fun renamePlaylist(id: String, name: String) {
        if (name.isBlank()) return
        savePlaylists(_playlists.value.map { if (it.id == id) it.copy(name = name.trim()) else it })
    }

    fun deletePlaylist(id: String) {
        savePlaylists(_playlists.value.filterNot { it.id == id })
    }

    fun addToPlaylist(playlistId: String, trackId: Long) {
        savePlaylists(_playlists.value.map { playlist ->
            if (playlist.id == playlistId && trackId !in playlist.trackIds) {
                playlist.copy(trackIds = playlist.trackIds + trackId)
            } else playlist
        })
    }

    fun removeFromPlaylist(playlistId: String, trackId: Long) {
        savePlaylists(_playlists.value.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(trackIds = playlist.trackIds - trackId) else playlist
        })
    }

    fun recordPlayed(trackId: Long) {
        val recent = (listOf(trackId) + _recentlyPlayed.value.filterNot { it == trackId }).take(50)
        _recentlyPlayed.value = recent
        writeLongList(KEY_RECENT, recent)

        val counts = _playCounts.value.toMutableMap()
        counts[trackId] = (counts[trackId] ?: 0) + 1
        _playCounts.value = counts
        preferences.edit().putString(KEY_PLAY_COUNTS, JSONObject().apply {
            counts.forEach { (id, count) -> put(id.toString(), count) }
        }.toString()).apply()
    }

    fun exportBackup(): String = JSONObject().apply {
        put("version", 2)
        put("settings", _settings.value.toJson())
        put("favorites", JSONArray(_favorites.value.toList()))
        put("hiddenTrackIds", JSONArray(_hiddenTrackIds.value.toList()))
        put("recentlyPlayed", JSONArray(_recentlyPlayed.value))
        put("playCounts", JSONObject().apply {
            _playCounts.value.forEach { (id, count) -> put(id.toString(), count) }
        })
        put("trackOverrides", trackOverridesToJson(_trackOverrides.value))
        put("lyrics", JSONObject().apply {
            _lyrics.value.forEach { (id, value) -> put(id.toString(), value) }
        })
        put("lastPlayback", JSONObject().apply {
            put("queueIds", JSONArray(_lastPlayback.value.queueIds))
            _lastPlayback.value.mediaId?.let { put("mediaId", it) }
            put("position", _lastPlayback.value.position)
        })
        put("playlists", playlistsToJson(_playlists.value))
    }.toString(2)

    fun importBackup(json: String): Boolean = runCatching {
        val root = JSONObject(json)
        val favorites = root.optJSONArray("favorites").toLongList().toSet()
        val hidden = root.optJSONArray("hiddenTrackIds").toLongList().toSet()
        val recent = root.optJSONArray("recentlyPlayed").toLongList()
        val countsObject = root.optJSONObject("playCounts") ?: JSONObject()
        val counts = buildMap {
            countsObject.keys().forEach { key -> put(key.toLong(), countsObject.optInt(key)) }
        }
        val playlists = parsePlaylists(root.optJSONArray("playlists") ?: JSONArray())
        val overrides = parseTrackOverrides(root.optJSONObject("trackOverrides") ?: JSONObject())
        val lyrics = parseStringMap(root.optJSONObject("lyrics") ?: JSONObject())
        val restoredSettings = root.optJSONObject("settings")?.toSettings() ?: _settings.value
        val restoredPlayback = root.optJSONObject("lastPlayback")?.toLastPlayback() ?: _lastPlayback.value

        _settings.value = restoredSettings
        preferences.edit().putString(KEY_SETTINGS, restoredSettings.toJson().toString()).apply()
        _favorites.value = favorites
        writeLongSet(KEY_FAVORITES, favorites)
        _hiddenTrackIds.value = hidden
        writeLongSet(KEY_HIDDEN, hidden)
        _recentlyPlayed.value = recent
        writeLongList(KEY_RECENT, recent)
        _playCounts.value = counts
        preferences.edit().putString(KEY_PLAY_COUNTS, countsObject.toString()).apply()
        _trackOverrides.value = overrides
        writeTrackOverrides(overrides)
        _lyrics.value = lyrics
        preferences.edit().putString(KEY_LYRICS, JSONObject().apply {
            lyrics.forEach { (id, value) -> put(id.toString(), value) }
        }.toString()).apply()
        _lastPlayback.value = restoredPlayback
        preferences.edit().putString(KEY_LAST_PLAYBACK, JSONObject().apply {
            put("queueIds", JSONArray(restoredPlayback.queueIds))
            restoredPlayback.mediaId?.let { put("mediaId", it) }
            put("position", restoredPlayback.position)
        }.toString()).apply()
        savePlaylists(playlists)
        true
    }.getOrDefault(false)

    private fun readSettings(): AppSettings = runCatching {
        val raw = preferences.getString(KEY_SETTINGS, null) ?: return AppSettings()
        JSONObject(raw).toSettings()
    }.getOrDefault(AppSettings())

    private fun JSONObject.toSettings() = AppSettings(
        language = optString("language", "ar"),
        theme = optString("theme", "system"),
        accent = optString("accent", "violet"),
        minimumDurationMs = optLong("minimumDurationMs", 10_000L),
        minimumSizeBytes = optLong("minimumSizeBytes", 0L),
        pauseOnHeadsetDisconnect = optBoolean("pauseOnHeadsetDisconnect", true),
        rememberLastSong = optBoolean("rememberLastSong", true),
        albumGrid = optBoolean("albumGrid", true),
    )

    private fun AppSettings.toJson() = JSONObject().apply {
        put("language", language)
        put("theme", theme)
        put("accent", accent)
        put("minimumDurationMs", minimumDurationMs)
        put("minimumSizeBytes", minimumSizeBytes)
        put("pauseOnHeadsetDisconnect", pauseOnHeadsetDisconnect)
        put("rememberLastSong", rememberLastSong)
        put("albumGrid", albumGrid)
    }

    private fun readPlaylists(): List<UserPlaylist> = runCatching {
        parsePlaylists(JSONArray(preferences.getString(KEY_PLAYLISTS, "[]")))
    }.getOrDefault(emptyList())

    private fun parsePlaylists(array: JSONArray): List<UserPlaylist> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(UserPlaylist(
                id = item.optString("id", UUID.randomUUID().toString()),
                name = item.optString("name", "Playlist"),
                trackIds = item.optJSONArray("trackIds").toLongList(),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            ))
        }
    }

    private fun playlistsToJson(playlists: List<UserPlaylist>) = JSONArray().apply {
        playlists.forEach { playlist ->
            put(JSONObject().apply {
                put("id", playlist.id)
                put("name", playlist.name)
                put("trackIds", JSONArray(playlist.trackIds))
                put("createdAt", playlist.createdAt)
            })
        }
    }

    private fun savePlaylists(playlists: List<UserPlaylist>) {
        _playlists.value = playlists
        preferences.edit().putString(KEY_PLAYLISTS, playlistsToJson(playlists).toString()).apply()
    }

    private fun readTrackOverrides(): Map<Long, TrackOverride> = runCatching {
        parseTrackOverrides(JSONObject(preferences.getString(KEY_TRACK_OVERRIDES, "{}")))
    }.getOrDefault(emptyMap())

    private fun parseTrackOverrides(json: JSONObject): Map<Long, TrackOverride> = buildMap {
        json.keys().forEach { key ->
            val item = json.getJSONObject(key)
            put(key.toLong(), TrackOverride(
                title = item.optString("title").takeIf(String::isNotBlank),
                artist = item.optString("artist").takeIf(String::isNotBlank),
                album = item.optString("album").takeIf(String::isNotBlank),
                genre = item.optString("genre").takeIf(String::isNotBlank),
                artworkUri = item.optString("artworkUri").takeIf(String::isNotBlank),
            ))
        }
    }

    private fun trackOverridesToJson(values: Map<Long, TrackOverride>) = JSONObject().apply {
        values.forEach { (id, value) ->
            put(id.toString(), JSONObject().apply {
                value.title?.let { put("title", it) }
                value.artist?.let { put("artist", it) }
                value.album?.let { put("album", it) }
                value.genre?.let { put("genre", it) }
                value.artworkUri?.let { put("artworkUri", it) }
            })
        }
    }

    private fun writeTrackOverrides(values: Map<Long, TrackOverride>) {
        preferences.edit().putString(KEY_TRACK_OVERRIDES, trackOverridesToJson(values).toString()).apply()
    }

    private fun readStringMap(key: String): Map<Long, String> = runCatching {
        parseStringMap(JSONObject(preferences.getString(key, "{}")))
    }.getOrDefault(emptyMap())

    private fun parseStringMap(json: JSONObject): Map<Long, String> = buildMap {
        json.keys().forEach { key -> put(key.toLong(), json.optString(key)) }
    }

    private fun readLastPlayback(): LastPlayback = runCatching {
        JSONObject(preferences.getString(KEY_LAST_PLAYBACK, "{}")).toLastPlayback()
    }.getOrDefault(LastPlayback())

    private fun JSONObject.toLastPlayback() = LastPlayback(
        queueIds = optJSONArray("queueIds").toLongList(),
        mediaId = if (has("mediaId") && !isNull("mediaId")) optLong("mediaId") else null,
        position = optLong("position", 0L),
    )

    private fun readLongSet(key: String): Set<Long> = readLongList(key).toSet()

    private fun readLongList(key: String): List<Long> = runCatching {
        JSONArray(preferences.getString(key, "[]")).toLongList()
    }.getOrDefault(emptyList())

    private fun writeLongSet(key: String, values: Set<Long>) = writeLongList(key, values.toList())

    private fun writeLongList(key: String, values: List<Long>) {
        preferences.edit().putString(key, JSONArray(values).toString()).apply()
    }

    private fun readCounts(): Map<Long, Int> = runCatching {
        val json = JSONObject(preferences.getString(KEY_PLAY_COUNTS, "{}"))
        buildMap { json.keys().forEach { key -> put(key.toLong(), json.optInt(key)) } }
    }.getOrDefault(emptyMap())

    private fun JSONArray?.toLongList(): List<Long> {
        if (this == null) return emptyList()
        return buildList { for (index in 0 until length()) add(optLong(index)) }
    }

    companion object {
        private const val KEY_SETTINGS = "settings"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_RECENT = "recently_played"
        private const val KEY_PLAY_COUNTS = "play_counts"
        private const val KEY_HIDDEN = "hidden_track_ids"
        private const val KEY_TRACK_OVERRIDES = "track_overrides"
        private const val KEY_LYRICS = "lyrics"
        private const val KEY_LAST_PLAYBACK = "last_playback"
    }
}
