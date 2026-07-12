package com.alomessi.harmonix.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.data.TrackOverride

@Composable
fun LyricsScreen(
    track: Track,
    savedLyrics: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onImport: () -> Unit,
) {
    var value by rememberSaveable(track.id, savedLyrics) { mutableStateOf(savedLyrics) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("رجوع", "Back")) }
            Column(Modifier.weight(1f)) {
                Text(tr("كلمات الأغنية", "Lyrics"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Lyrics, null, tint = MaterialTheme.colorScheme.primary)
        }

        Text(
            tr(
                "أضف كلماتك المحلية أو نصًا تملك حق استخدامه. لا يجلب التطبيق الكلمات من الإنترنت.",
                "Add local lyrics or text you have permission to use. The app does not fetch lyrics from the internet.",
            ),
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onImport, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(Icons.Default.FileOpen, null)
            Text(tr("استيراد ملف LRC أو TXT", "Import LRC or TXT"))
        }
        OutlinedTextField(
            value = value,
            onValueChange = { value = it.take(100_000) },
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp, vertical = 8.dp),
            placeholder = { Text(tr("اكتب أو الصق الكلمات هنا…", "Write or paste lyrics here…")) },
            shape = RoundedCornerShape(20.dp),
        )
        Row(Modifier.fillMaxWidth().padding(18.dp)) {
            FilledTonalButton(onClick = { value = "" }, modifier = Modifier.weight(1f)) {
                Text(tr("مسح", "Clear"))
            }
            Spacer(Modifier.size(10.dp))
            Button(onClick = { onSave(value) }, modifier = Modifier.weight(1f)) {
                Text(tr("حفظ الكلمات", "Save lyrics"))
            }
        }
    }
}

@Composable
fun EditTagDialog(
    track: Track,
    onDismiss: () -> Unit,
    onSave: (TrackOverride) -> Unit,
    onPickArtwork: () -> Unit,
    onReset: () -> Unit,
) {
    var title by remember(track.id, track.title) { mutableStateOf(track.title) }
    var artist by remember(track.id, track.artist) { mutableStateOf(track.artist) }
    var album by remember(track.id, track.album) { mutableStateOf(track.album) }
    var genre by remember(track.id, track.genre) { mutableStateOf(track.genre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, null) },
        title = { Text(tr("تحرير بيانات الأغنية", "Edit song details")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(track.artworkUri, Modifier.size(72.dp), 18)
                    TextButton(onClick = onPickArtwork) {
                        Icon(Icons.Default.Image, null)
                        Text(tr("اختيار غلاف", "Choose artwork"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(title, { title = it }, label = { Text(tr("العنوان", "Title")) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(artist, { artist = it }, label = { Text(tr("الفنان", "Artist")) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(album, { album = it }, label = { Text(tr("الألبوم", "Album")) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(genre, { genre = it }, label = { Text(tr("النوع الموسيقي", "Genre")) }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                Text(
                    tr("تُحفظ التعديلات داخل Harmonix دون تغيير الملف الأصلي.", "Changes are saved inside Harmonix without altering the original file."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(TrackOverride(title = title, artist = artist, album = album, genre = genre))
                onDismiss()
            }, enabled = title.isNotBlank()) { Text(tr("حفظ", "Save")) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onReset(); onDismiss() }) {
                    Icon(Icons.Default.Restore, null)
                    Text(tr("استعادة", "Reset"))
                }
                TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) }
            }
        },
    )
}

@Composable
fun HiddenTracksScreen(
    tracks: List<Track>,
    onBack: () -> Unit,
    onUnhide: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("رجوع", "Back")) }
            Column(Modifier.weight(1f)) {
                Text(tr("الأغاني المخفية", "Hidden songs"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(tr("${tracks.size} أغنية", "${tracks.size} songs"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (tracks.isEmpty()) {
            EmptyState(
                tr("لا توجد أغانٍ مخفية", "No hidden songs"),
                tr("يمكنك إخفاء أي أغنية من قائمة خياراتها", "Hide any song from its options menu"),
                Modifier.weight(1f),
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) {
                items(tracks.size, key = { tracks[it].id }) { index ->
                    val track = tracks[index]
                    Row(
                        Modifier.fillMaxWidth().clickable { onUnhide(track.id) }.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArt(track.artworkUri, Modifier.size(54.dp), 14)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Visibility, tr("إظهار", "Unhide"), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackSpeedDialog(current: Float, onDismiss: () -> Unit, onSelect: (Float) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("سرعة التشغيل", "Playback speed")) },
        text = {
            Column {
                listOf(.5f, .75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(speed); onDismiss() }.padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${speed}×", Modifier.weight(1f), fontWeight = if (current == speed) FontWeight.Black else FontWeight.Normal)
                        if (current == speed) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("إغلاق", "Close")) } },
    )
}
