package com.alomessi.harmonix.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.data.UserPlaylist
import com.alomessi.harmonix.util.formatDuration
import com.alomessi.harmonix.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(
    track: Track,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onRingtone: () -> Unit,
    onEdit: () -> Unit,
    onLyrics: () -> Unit,
    onHide: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(track.artworkUri, Modifier.size(64.dp), 18)
            Column(Modifier.padding(horizontal = 14.dp)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SheetItem(Icons.Default.Favorite, if (isFavorite) tr("إزالة من المفضلة", "Remove from favorites") else tr("إضافة للمفضلة", "Add to favorites")) { onFavorite(); onDismiss() }
        SheetItem(Icons.Default.PlaylistAdd, tr("إضافة إلى قائمة تشغيل", "Add to playlist")) { onAddToPlaylist() }
        SheetItem(Icons.Default.Edit, tr("تحرير بيانات الأغنية", "Edit song details")) { onEdit() }
        SheetItem(Icons.Default.Lyrics, tr("كلمات الأغنية", "Lyrics")) { onLyrics() }
        SheetItem(Icons.Default.VisibilityOff, tr("إخفاء من المكتبة", "Hide from library")) { onHide(); onDismiss() }
        SheetItem(Icons.Default.Share, tr("مشاركة ملف الصوت", "Share audio file")) { onShare(); onDismiss() }
        SheetItem(Icons.Default.Notifications, tr("تعيين كنغمة رنين", "Set as ringtone")) { onRingtone(); onDismiss() }
        ListItem(
            headlineContent = { Text(tr("تفاصيل الملف", "File details"), fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("${track.album} • ${formatDuration(track.duration)} • ${formatFileSize(track.size)}\n${track.folder}") },
            leadingContent = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track,
    playlists: List<UserPlaylist>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onCreate: (String) -> UserPlaylist?,
) {
    var createDialog by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(tr("إضافة إلى قائمة تشغيل", "Add to playlist"), Modifier.padding(horizontal = 22.dp, vertical = 10.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SheetItem(Icons.Default.Add, tr("إنشاء قائمة جديدة", "Create new playlist")) { createDialog = true }
        if (playlists.isEmpty()) {
            Text(tr("لا توجد قوائم بعد", "No playlists yet"), Modifier.padding(22.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn {
                items(playlists, key = UserPlaylist::id) { playlist ->
                    SheetItem(Icons.Default.LibraryMusic, playlist.name, tr("${playlist.trackIds.size} أغنية", "${playlist.trackIds.size} songs")) {
                        onAdd(playlist.id)
                        onDismiss()
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (createDialog) {
        CreatePlaylistDialog(
            onDismiss = { createDialog = false },
            onCreate = { name ->
                val created = onCreate(name)
                if (created != null) onAdd(created.id)
                createDialog = false
                onDismiss()
            },
        )
    }
}

@Composable
fun SleepTimerDialog(
    isActive: Boolean,
    onDismiss: () -> Unit,
    onSet: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Alarm, null) },
        title = { Text(tr("مؤقت النوم", "Sleep timer")) },
        text = {
            Column {
                Text(tr("أوقف الموسيقى تلقائيًا بعد:", "Stop playback automatically after:"))
                listOf(15, 30, 45, 60, 90).forEach { minutes ->
                    Text(
                        tr("$minutes دقيقة", "$minutes minutes"),
                        Modifier.fillMaxWidth().clickable { onSet(minutes); onDismiss() }.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        confirmButton = {
            if (isActive) TextButton(onClick = { onCancel(); onDismiss() }) {
                Icon(Icons.Default.TimerOff, null)
                Text(tr("إلغاء المؤقت", "Cancel timer"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إغلاق", "Close")) } },
    )
}

@Composable
private fun SheetItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = if (subtitle == null) null else ({ Text(subtitle) }),
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
