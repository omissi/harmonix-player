package com.alomessi.harmonix.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.alomessi.harmonix.MainActivity
import com.alomessi.harmonix.R

class MusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, context.getString(R.string.widget_nothing_playing)).orEmpty()
        val artist = prefs.getString(KEY_ARTIST, context.getString(R.string.widget_choose_song)).orEmpty()
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it, title, artist, isPlaying) }
    }

    companion object {
        private const val PREFS = "harmonix_widget"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_IS_PLAYING = "is_playing"

        fun publish(context: Context, title: String, artist: String, isPlaying: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .putBoolean(KEY_IS_PLAYING, isPlaying)
                .apply()
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MusicWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach {
                updateWidget(context, manager, it, title, artist, isPlaying)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            title: String,
            artist: String,
            isPlaying: Boolean,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_player).apply {
                setTextViewText(R.id.widget_title, title.ifBlank { context.getString(R.string.widget_nothing_playing) })
                setTextViewText(R.id.widget_artist, artist.ifBlank { context.getString(R.string.widget_choose_song) })
                setImageViewResource(R.id.widget_play_pause, if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
                setOnClickPendingIntent(R.id.widget_previous, commandIntent(context, WidgetCommandReceiver.ACTION_PREVIOUS, 1))
                setOnClickPendingIntent(R.id.widget_play_pause, commandIntent(context, WidgetCommandReceiver.ACTION_TOGGLE, 2))
                setOnClickPendingIntent(R.id.widget_next, commandIntent(context, WidgetCommandReceiver.ACTION_NEXT, 3))
                setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun commandIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, WidgetCommandReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                4,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
