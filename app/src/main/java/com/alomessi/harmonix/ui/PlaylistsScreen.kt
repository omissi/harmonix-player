package com.alomessi.harmonix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.data.UserPlaylist

@Composable
fun PlaylistsScreen(
    tracks: List<Track>,
    favorites: Set<Long>,
    recentIds: List<Long>,
    playCounts: Map<Long, Int>,
    playlists: List<UserPlaylist>,
    onOpenTracks: (String, List<Long>) -> Unit,
    onOpenPlaylist: (UserPlaylist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var createDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<UserPlaylist?>(null) }
    var deleteTarget by remember { mutableStateOf<UserPlaylist?>(null) }
    val favoritesTitle = tr("المفضلة", "Favorites")
    val recentlyPlayedTitle = tr("استمعت مؤخرًا", "Recently played")
    val mostPlayedTitle = tr("الأكثر تشغيلًا", "Most played")
    val mostPlayedIds = tracks.sortedByDescending { playCounts[it.id] ?: 0 }
        .filter { (playCounts[it.id] ?: 0) > 0 }
        .map(Track::id)
    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text(tr("قوائم التشغيل", "Playlists"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        tr("نظّم موسيقاك كما تحب", "Organize your music your way"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SmartPlaylistCard(
                        favoritesTitle, favorites.size, Icons.Default.Favorite,
                        listOf(Color(0xFFFF477E), Color(0xFFFF7B54)), Modifier.weight(1f),
                    ) { onOpenTracks(favoritesTitle, favorites.toList()) }
                    SmartPlaylistCard(
                        tr("مؤخرًا", "Recent"), recentIds.size, Icons.Default.History,
                        listOf(Color(0xFF6A5AE0), Color(0xFF8F62FF)), Modifier.weight(1f),
                    ) { onOpenTracks(recentlyPlayedTitle, recentIds) }
                }
            }
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).clickable {
                        onOpenTracks(mostPlayedTitle, mostPlayedIds)
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(
                                Brush.linearGradient(listOf(Color(0xFF18A999), Color(0xFF45C5B5))),
                            ),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.TrendingUp, null, tint = Color.White) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("الأكثر تشغيلًا", "Most played"), fontWeight = FontWeight.Bold)
                            Text(
                                tr("${mostPlayedIds.size} أغنية", "${mostPlayedIds.size} songs"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Default.PlaylistPlay, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item { SectionHeader(tr("قوائمي", "My playlists")) }
            if (playlists.isEmpty()) {
                item {
                    EmptyState(
                        tr("أنشئ أول قائمة تشغيل", "Create your first playlist"),
                        tr("اجمع أغانيك المفضلة في قوائم خاصة", "Collect your favorite songs in custom playlists"),
                    )
                }
            } else {
                items(playlists, key = UserPlaylist::id) { playlist ->
                    PlaylistRow(
                        playlist,
                        tracks,
                        onOpenPlaylist,
                        onRename = { renameTarget = playlist },
                        onDelete = { deleteTarget = playlist },
                    )
                }
            }
            item { Spacer(Modifier.height(130.dp)) }
        }

        FloatingActionButton(
            onClick = { createDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 116.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { Icon(Icons.Default.Add, tr("قائمة جديدة", "New playlist")) }
    }

    if (createDialog) {
        CreatePlaylistDialog(
            onDismiss = { createDialog = false },
            onCreate = { onCreatePlaylist(it); createDialog = false },
        )
    }
    renameTarget?.let { playlist ->
        RenamePlaylistDialog(
            playlist = playlist,
            onDismiss = { renameTarget = null },
            onRename = { onRenamePlaylist(playlist.id, it); renameTarget = null },
        )
    }
    deleteTarget?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(tr("حذف قائمة التشغيل؟", "Delete playlist?")) },
            text = { Text(tr("سيتم حذف قائمة ${playlist.name} فقط، ولن تُحذف ملفات الأغاني.", "Only ${playlist.name} will be deleted. Audio files stay on your device.")) },
            confirmButton = {
                Button(onClick = { onDeletePlaylist(playlist.id); deleteTarget = null }) {
                    Text(tr("حذف", "Delete"))
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(tr("إلغاء", "Cancel")) } },
        )
    }
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    count: Int,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(Modifier.background(Brush.linearGradient(colors)).padding(16.dp).fillMaxWidth()) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(25.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(tr("$count أغنية", "$count songs"), color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: UserPlaylist,
    tracks: List<Track>,
    onOpen: (UserPlaylist) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val artwork = playlist.trackIds.mapNotNull { id -> tracks.find { it.id == id }?.artworkUri }.firstOrNull()
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(playlist) }.padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(artwork, Modifier.size(62.dp), 18)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text(
                tr("${playlist.trackIds.size} أغنية", "${playlist.trackIds.size} songs"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, tr("خيارات", "Options"), tint = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(tr("إعادة التسمية", "Rename")) },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = { menuExpanded = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text(tr("حذف", "Delete")) },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = { menuExpanded = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun RenamePlaylistDialog(playlist: UserPlaylist, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember(playlist.id) { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("إعادة تسمية القائمة", "Rename playlist")) },
        text = { OutlinedTextField(name, { name = it }, label = { Text(tr("اسم القائمة", "Playlist name")) }, singleLine = true) },
        confirmButton = { Button(onClick = { onRename(name) }, enabled = name.isNotBlank()) { Text(tr("حفظ", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) } },
    )
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("قائمة تشغيل جديدة", "New playlist")) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("اسم القائمة", "Playlist name")) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text(tr("إنشاء", "Create")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) } },
    )
}
