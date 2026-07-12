package com.alomessi.harmonix.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.alomessi.harmonix.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val mediaId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val position: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackState: Int = Player.STATE_IDLE,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queueIds: List<Long> = emptyList(),
    val currentIndex: Int = -1,
    val playbackSpeed: Float = 1f,
    val error: String? = null,
)

class PlayerConnection(context: Context) {
    private val appContext = context.applicationContext
    private val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    private val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
    private var controller: MediaController? = null
    private var pendingPlayback: (() -> Unit)? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateState(player)

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(error = error.localizedMessage ?: "Playback error")
        }
    }

    init {
        controllerFuture.addListener({
            runCatching { controllerFuture.get() }
                .onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    updateState(mediaController)
                    pendingPlayback?.invoke()
                    pendingPlayback = null
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(error = error.localizedMessage ?: "Cannot connect to player")
                }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun play(track: Track, queue: List<Track>) {
        val action: () -> Unit = {
            val playableQueue = queue.ifEmpty { listOf(track) }
            val startIndex = playableQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            controller?.apply {
                setMediaItems(playableQueue.map { it.toMediaItem() }, startIndex, 0L)
                prepare()
                play()
            }
            Unit
        }
        if (controller == null) pendingPlayback = action else action()
    }

    fun playQueue(queue: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (queue.isEmpty()) return
        val action: () -> Unit = {
            controller?.apply {
                shuffleModeEnabled = shuffle
                setMediaItems(queue.map { it.toMediaItem() }, startIndex.coerceIn(queue.indices), 0L)
                prepare()
                play()
            }
            Unit
        }
        if (controller == null) pendingPlayback = action else action()
    }

    fun restoreQueue(queue: List<Track>, startIndex: Int, position: Long) {
        if (queue.isEmpty()) return
        val action: () -> Unit = {
            controller?.apply {
                setMediaItems(queue.map { it.toMediaItem() }, startIndex.coerceIn(queue.indices), position.coerceAtLeast(0L))
                prepare()
                pause()
            }
            Unit
        }
        if (controller == null) pendingPlayback = action else action()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun pause() = controller?.pause() ?: Unit
    fun skipNext() = controller?.seekToNextMediaItem() ?: Unit
    fun skipPrevious() = controller?.seekToPreviousMediaItem() ?: Unit
    fun seekTo(position: Long) = controller?.seekTo(position.coerceAtLeast(0L)) ?: Unit
    fun seekToQueueIndex(index: Int) = controller?.seekToDefaultPosition(index) ?: Unit

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(.5f, 2f))
    }

    fun refreshProgress() {
        controller?.let(::updateState)
    }

    fun release() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }

    private fun updateState(player: Player) {
        val metadata = player.mediaMetadata
        val duration = player.duration.takeUnless { it == C.TIME_UNSET || it < 0L } ?: 0L
        _state.value = PlayerUiState(
            connected = true,
            isPlaying = player.isPlaying,
            mediaId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            artworkUri = metadata.artworkUri?.toString(),
            position = player.currentPosition.coerceAtLeast(0L),
            duration = duration,
            bufferedPosition = player.bufferedPosition.coerceAtLeast(0L),
            playbackState = player.playbackState,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queueIds = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId.toLongOrNull() ?: -1L },
            currentIndex = player.currentMediaItemIndex,
            playbackSpeed = player.playbackParameters.speed,
            error = player.playerError?.localizedMessage,
        )
    }

    private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri?.let(Uri::parse))
                .setIsPlayable(true)
                .build(),
        )
        .build()
}
