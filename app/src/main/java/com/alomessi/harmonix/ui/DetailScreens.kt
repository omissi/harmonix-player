package com.alomessi.harmonix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.player.PlayerUiState

@Composable
fun TrackCollectionScreen(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    favoriteIds: Set<Long>,
    currentTrackId: Long?,
    onBack: () -> Unit,
    onPlayQueue: (List<Track>, Int, Boolean) -> Unit,
    onFavorite: (Long) -> Unit,
    onMore: (Track) -> Unit,
    onRemove: ((Long) -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("رجوع", "Back")) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = { onPlayQueue(tracks, 0, false) }, enabled = tracks.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null)
                Text(tr("تشغيل الكل", "Play all"))
            }
            FilledTonalButton(onClick = { onPlayQueue(tracks, 0, true) }, enabled = tracks.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Shuffle, null)
                Text(tr("عشوائي", "Shuffle"))
            }
        }

        if (tracks.isEmpty()) {
            EmptyState(
                tr("القائمة فارغة", "This list is empty"),
                tr("أضف بعض الأغاني وستظهر هنا", "Add some songs and they will appear here"),
                Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            (index + 1).toString(),
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TrackRow(
                            track = track,
                            isCurrent = currentTrackId == track.id,
                            isFavorite = track.id in favoriteIds,
                            onClick = { onPlayQueue(tracks, index, false) },
                            onFavorite = { onFavorite(track.id) },
                            onMore = { onMore(track) },
                            modifier = Modifier.weight(1f),
                        )
                        if (onRemove != null) {
                            IconButton(onClick = { onRemove(track.id) }) {
                                Icon(Icons.Default.Delete, tr("إزالة", "Remove"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
fun QueueScreen(
    tracks: List<Track>,
    state: PlayerUiState,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val queue = state.queueIds.mapIndexedNotNull { queueIndex, id ->
        tracks.find { it.id == id }?.let { queueIndex to it }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("رجوع", "Back")) }
            Column {
                Text(tr("قائمة الانتظار", "Playing queue"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(tr("${queue.size} أغنية", "${queue.size} songs"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (queue.isEmpty()) {
            EmptyState(tr("لا توجد قائمة تشغيل", "Queue is empty"), tr("شغّل أغنية لبدء القائمة", "Play a song to start a queue"))
        } else {
            LazyColumn {
                itemsIndexed(queue, key = { index, item -> "${item.second.id}-$index" }) { index, item ->
                    val (queueIndex, track) = item
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            Modifier.size(34.dp),
                            shape = CircleShape,
                            color = if (queueIndex == state.currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { onSelect(queueIndex) },
                        ) { Box(contentAlignment = Alignment.Center) { Text((queueIndex + 1).toString(), fontWeight = FontWeight.Bold) } }
                        TrackRow(
                            track,
                            isCurrent = queueIndex == state.currentIndex,
                            isFavorite = false,
                            onClick = { onSelect(queueIndex) },
                            onFavorite = {},
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
