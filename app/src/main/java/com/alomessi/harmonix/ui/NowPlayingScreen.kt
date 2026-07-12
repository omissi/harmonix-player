package com.alomessi.harmonix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.player.PlayerUiState
import com.alomessi.harmonix.ui.theme.HarmonixBlue
import com.alomessi.harmonix.ui.theme.HarmonixPink
import com.alomessi.harmonix.ui.theme.HarmonixPurple
import com.alomessi.harmonix.util.formatDuration
import com.alomessi.harmonix.util.safeProgress
import kotlin.math.roundToLong

@Composable
fun NowPlayingScreen(
    track: Track,
    state: PlayerUiState,
    isFavorite: Boolean,
    sleepTimerEndsAt: Long?,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenSpeed: () -> Unit,
) {
    var isDragging by remember { mutableStateOf(false) }
    var draggedProgress by remember { mutableFloatStateOf(0f) }
    val progress = if (isDragging) draggedProgress else safeProgress(state.position, state.duration)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artSize = minOf(maxWidth - 52.dp, 410.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("رجوع", "Back")) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("قيد التشغيل الآن", "NOW PLAYING"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(track.album, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onAddToPlaylist) { Icon(Icons.Default.PlaylistAdd, tr("إضافة لقائمة", "Add to playlist")) }
            }

            Spacer(Modifier.height(12.dp))
            Box {
                Box(
                    Modifier.size(artSize + 14.dp).clip(RoundedCornerShape(38.dp)).background(
                        Brush.linearGradient(
                            listOf(HarmonixPurple.copy(alpha = .28f), HarmonixPink.copy(alpha = .18f), HarmonixBlue.copy(alpha = .25f)),
                        ),
                    ),
                )
                AlbumArt(track.artworkUri, Modifier.size(artSize).align(Alignment.Center), 32)
            }
            Spacer(Modifier.height(26.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        tr("المفضلة", "Favorite"),
                        tint = if (isFavorite) HarmonixPink else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(29.dp),
                    )
                }
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, tr("مشاركة", "Share")) }
            }

            Spacer(Modifier.height(14.dp))
            Slider(
                value = progress,
                onValueChange = { isDragging = true; draggedProgress = it },
                onValueChangeFinished = {
                    onSeek((draggedProgress * state.duration).roundToLong())
                    isDragging = false
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
                Text(formatDuration(if (isDragging) (draggedProgress * state.duration).roundToLong() else state.position), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(state.duration), style = MaterialTheme.typography.labelMedium)
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShuffle) {
                    Icon(Icons.Default.Shuffle, tr("عشوائي", "Shuffle"), tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) {
                    Icon(Icons.Default.SkipPrevious, tr("السابق", "Previous"), modifier = Modifier.size(40.dp))
                }
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 12.dp,
                    onClick = onToggle,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            state.playbackState == Player.STATE_BUFFERING -> CircularProgressIndicator(Modifier.size(31.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                            state.isPlaying -> Icon(Icons.Default.Pause, tr("إيقاف مؤقت", "Pause"), Modifier.size(42.dp))
                            else -> Icon(Icons.Default.PlayArrow, tr("تشغيل", "Play"), Modifier.size(46.dp))
                        }
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
                    Icon(Icons.Default.SkipNext, tr("التالي", "Next"), modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = onRepeat) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        tr("التكرار", "Repeat"),
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PlayerTool(Icons.Default.QueueMusic, tr("القائمة", "Queue"), onOpenQueue)
                PlayerTool(Icons.Default.Equalizer, tr("المعادل", "Equalizer"), onOpenEqualizer)
                PlayerTool(Icons.Default.Lyrics, tr("الكلمات", "Lyrics"), onOpenLyrics)
                PlayerTool(
                    Icons.Default.Timer,
                    if (sleepTimerEndsAt == null) tr("المؤقت", "Timer") else tr("مفعّل", "Active"),
                    onOpenTimer,
                    active = sleepTimerEndsAt != null,
                )
                PlayerTool(Icons.Default.Speed, "${state.playbackSpeed}×", onOpenSpeed, active = state.playbackSpeed != 1f)
            }
        }
    }
}

@Composable
private fun PlayerTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            onClick = onClick,
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) } }
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}
