package com.alomessi.harmonix.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alomessi.harmonix.data.AppSettings
import com.alomessi.harmonix.data.MusicRepository
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.data.TrackOverride
import com.alomessi.harmonix.data.UserPlaylist
import com.alomessi.harmonix.data.UserPreferences
import com.alomessi.harmonix.data.withOverride
import com.alomessi.harmonix.player.AudioEffectsManager
import com.alomessi.harmonix.player.PlayerConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HarmonixViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val preferences = UserPreferences(application)
    private val playerConnection = PlayerConnection(application)

    private val _scannedTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks: StateFlow<List<Track>> = _allTracks.asStateFlow()

    private val _hiddenTracks = MutableStateFlow<List<Track>>(emptyList())
    val hiddenTracks: StateFlow<List<Track>> = _hiddenTracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _permissionGranted = MutableStateFlow(hasAudioPermission())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _sleepTimerEndsAt = MutableStateFlow<Long?>(null)
    val sleepTimerEndsAt: StateFlow<Long?> = _sleepTimerEndsAt.asStateFlow()
    private var sleepTimerJob: Job? = null
    private var restoredLastPlayback = false

    val settings = preferences.settings
    val favorites = preferences.favorites
    val playlists = preferences.playlists
    val recentlyPlayed = preferences.recentlyPlayed
    val playCounts = preferences.playCounts
    val hiddenTrackIds = preferences.hiddenTrackIds
    val trackOverrides = preferences.trackOverrides
    val lyrics = preferences.lyrics
    val playerState = playerConnection.state
    val equalizerState = AudioEffectsManager.state

    init {
        if (_permissionGranted.value) refreshLibrary()

        viewModelScope.launch {
            preferences.hiddenTrackIds.collect { rebuildLibrary() }
        }
        viewModelScope.launch {
            preferences.trackOverrides.collect { rebuildLibrary() }
        }

        viewModelScope.launch {
            var saveCounter = 0
            while (isActive) {
                playerConnection.refreshProgress()
                if (++saveCounter >= 10) {
                    saveCounter = 0
                    savePlaybackPosition()
                }
                delay(500L)
            }
        }

        viewModelScope.launch {
            playerState.map { it.mediaId }
                .filterNotNull()
                .distinctUntilChanged()
                .collect {
                    preferences.recordPlayed(it)
                    savePlaybackPosition()
                }
        }
    }

    fun onPermissionsChanged() {
        val granted = hasAudioPermission()
        _permissionGranted.value = granted
        if (granted && _scannedTracks.value.isEmpty()) refreshLibrary()
    }

    fun refreshLibrary() {
        if (!hasAudioPermission() || _isScanning.value) return
        _isScanning.value = true
        viewModelScope.launch {
            try {
                _scannedTracks.value = runCatching {
                    repository.scan(settings.value.minimumDurationMs, settings.value.minimumSizeBytes)
                }.getOrDefault(emptyList())
                rebuildLibrary()
                restoreLastPlaybackIfNeeded()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun play(track: Track, queue: List<Track> = _tracks.value) = playerConnection.play(track, queue)
    fun playQueue(queue: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) =
        playerConnection.playQueue(queue, startIndex, shuffle)

    fun togglePlayPause() = playerConnection.togglePlayPause()
    fun skipNext() = playerConnection.skipNext()
    fun skipPrevious() = playerConnection.skipPrevious()
    fun seekTo(position: Long) = playerConnection.seekTo(position)
    fun seekToQueueIndex(index: Int) = playerConnection.seekToQueueIndex(index)
    fun toggleShuffle() = playerConnection.toggleShuffle()
    fun cycleRepeat() = playerConnection.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playerConnection.setPlaybackSpeed(speed)

    fun toggleFavorite(trackId: Long) = preferences.toggleFavorite(trackId)
    fun hideTrack(trackId: Long) = preferences.hideTrack(trackId)
    fun unhideTrack(trackId: Long) = preferences.unhideTrack(trackId)
    fun saveTrackOverride(trackId: Long, value: TrackOverride) {
        val existing = preferences.trackOverrides.value[trackId]
        preferences.saveTrackOverride(trackId, value.copy(artworkUri = value.artworkUri ?: existing?.artworkUri))
    }
    fun setCustomArtwork(trackId: Long, uri: String) = preferences.setCustomArtwork(trackId, uri)
    fun resetTrackOverride(trackId: Long) = preferences.resetTrackOverride(trackId)
    fun saveLyrics(trackId: Long, value: String) = preferences.saveLyrics(trackId, value)

    fun createPlaylist(name: String): UserPlaylist? = preferences.createPlaylist(name)
    fun renamePlaylist(id: String, name: String) = preferences.renamePlaylist(id, name)
    fun deletePlaylist(id: String) = preferences.deletePlaylist(id)
    fun addToPlaylist(playlistId: String, trackId: Long) = preferences.addToPlaylist(playlistId, trackId)
    fun removeFromPlaylist(playlistId: String, trackId: Long) = preferences.removeFromPlaylist(playlistId, trackId)

    fun setLanguage(language: String) = updateSettings { it.copy(language = language) }
    fun setTheme(theme: String) = updateSettings { it.copy(theme = theme) }
    fun setAccent(accent: String) = updateSettings { it.copy(accent = accent) }
    fun setMinimumDuration(milliseconds: Long) {
        updateSettings { it.copy(minimumDurationMs = milliseconds) }
        refreshLibrary()
    }
    fun setMinimumSize(bytes: Long) {
        updateSettings { it.copy(minimumSizeBytes = bytes) }
        refreshLibrary()
    }
    fun setPauseOnDisconnect(enabled: Boolean) = updateSettings { it.copy(pauseOnHeadsetDisconnect = enabled) }
    fun setRememberLastSong(enabled: Boolean) {
        updateSettings { it.copy(rememberLastSong = enabled) }
        if (!enabled) preferences.saveLastPlayback(emptyList(), null, 0L)
    }
    fun setAlbumGrid(enabled: Boolean) = updateSettings { it.copy(albumGrid = enabled) }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerEndsAt.value = null
            return
        }
        val delayMs = minutes * 60_000L
        _sleepTimerEndsAt.value = System.currentTimeMillis() + delayMs
        sleepTimerJob = viewModelScope.launch {
            delay(delayMs)
            playerConnection.pause()
            _sleepTimerEndsAt.value = null
        }
    }

    fun cancelSleepTimer() = setSleepTimer(0)
    fun exportBackup(): String = preferences.exportBackup()
    fun importBackup(json: String): Boolean {
        val success = preferences.importBackup(json)
        if (success) {
            rebuildLibrary()
            refreshLibrary()
        }
        return success
    }

    private fun rebuildLibrary() {
        val overrides = preferences.trackOverrides.value
        val hidden = preferences.hiddenTrackIds.value
        val all = _scannedTracks.value.map { it.withOverride(overrides[it.id]) }
        _allTracks.value = all
        _hiddenTracks.value = all.filter { it.id in hidden }
        _tracks.value = all.filterNot { it.id in hidden }
    }

    private fun restoreLastPlaybackIfNeeded() {
        if (restoredLastPlayback || !settings.value.rememberLastSong) return
        restoredLastPlayback = true
        val last = preferences.lastPlayback.value
        if (last.queueIds.isEmpty()) return
        val queue = last.queueIds.mapNotNull { id -> _allTracks.value.find { it.id == id } }
        val startIndex = queue.indexOfFirst { it.id == last.mediaId }.coerceAtLeast(0)
        if (queue.isNotEmpty()) playerConnection.restoreQueue(queue, startIndex, last.position)
    }

    private fun savePlaybackPosition() {
        if (!settings.value.rememberLastSong) return
        val state = playerState.value
        if (state.mediaId == null || state.queueIds.isEmpty()) return
        preferences.saveLastPlayback(state.queueIds.filter { it >= 0L }, state.mediaId, state.position)
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) = preferences.updateSettings(transform)

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        savePlaybackPosition()
        playerConnection.release()
        super.onCleared()
    }
}
