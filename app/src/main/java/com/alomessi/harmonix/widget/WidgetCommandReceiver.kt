package com.alomessi.harmonix.widget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.alomessi.harmonix.player.PlaybackService

class WidgetCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching {
                val controller = future.get()
                when (intent.action) {
                    ACTION_PREVIOUS -> controller.seekToPreviousMediaItem()
                    ACTION_NEXT -> controller.seekToNextMediaItem()
                    ACTION_TOGGLE -> if (controller.isPlaying) controller.pause() else controller.play()
                }
            }
            MediaController.releaseFuture(future)
            result.finish()
        }, ContextCompat.getMainExecutor(context))
    }

    companion object {
        const val ACTION_PREVIOUS = "com.alomessi.harmonix.widget.PREVIOUS"
        const val ACTION_TOGGLE = "com.alomessi.harmonix.widget.TOGGLE"
        const val ACTION_NEXT = "com.alomessi.harmonix.widget.NEXT"
    }
}
