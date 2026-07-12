package com.alomessi.harmonix.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.alomessi.harmonix.widget.MusicWidgetProvider

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && pauseOnDisconnectEnabled()) {
                player.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(false)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        AudioEffectsManager.attach(audioSessionId)
                        AudioEffectsManager.reverbEffectId()?.let { effectId ->
                            player.setAuxEffectInfo(AuxEffectInfo(effectId, 1f))
                        }
                    }

                    override fun onEvents(player: Player, events: Player.Events) {
                        MusicWidgetProvider.publish(
                            this@PlaybackService,
                            player.mediaMetadata.title?.toString().orEmpty(),
                            player.mediaMetadata.artist?.toString().orEmpty(),
                            player.isPlaying,
                        )
                    }
                })
            }

        mediaSession = MediaSession.Builder(this, player).build()
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(noisyReceiver, filter)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(noisyReceiver) }
        AudioEffectsManager.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun pauseOnDisconnectEnabled(): Boolean = runCatching {
        val raw = getSharedPreferences("harmonix_preferences", Context.MODE_PRIVATE)
            .getString("settings", null)
        if (raw.isNullOrBlank()) true else org.json.JSONObject(raw).optBoolean("pauseOnHeadsetDisconnect", true)
    }.getOrDefault(true)
}
