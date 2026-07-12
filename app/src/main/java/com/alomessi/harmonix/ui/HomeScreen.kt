package com.alomessi.harmonix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alomessi.harmonix.data.Album
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.data.toAlbums

@Composable
fun HomeScreen(
    tracks: List<Track>,
    recentIds: List<Long>,
    playCounts: Map<Long, Int>,
    favoriteIds: Set<Long>,
    onPlay: (Track, List<Track>) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTracks: (String, List<Long>) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recent = recentIds.mapNotNull { id -> tracks.find { it.id == id } }.take(12)
    val mostPlayed = tracks.sortedByDescending { playCounts[it.id] ?: 0 }.filter { (playCounts[it.id] ?: 0) > 0 }
    val albums = tracks.toAlbums().take(12)
    val favoritesTitle = tr("المفضلة", "Favorites")
    val recentlyPlayedTitle = tr("استمعت مؤخرًا", "Recently played")
    val mostPlayedTitle = tr("الأكثر تشغيلًا", "Most played")

    LazyColumn(modifier) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFFE75CFF)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("HARMONIX", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Text(
                        tr("موسيقاك في مكان واحد", "Your music, one beautiful place"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            GradientCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
                Text(
                    tr("استمع بطريقتك", "Listen your way"),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (tracks.isEmpty()) tr("امنح التطبيق إذن الوصول ثم ابدأ", "Grant access and start listening")
                    else tr("${tracks.size} أغنية جاهزة للتشغيل بدون إنترنت", "${tracks.size} songs ready offline"),
                    color = Color.White.copy(alpha = .85f),
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(CircleShape).background(Color.White).clickable {
                            tracks.firstOrNull()?.let { onPlay(it, tracks) }
                        }.padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF4B2C9D))
                            Spacer(Modifier.width(6.dp))
                            Text(tr("تشغيل", "Play"), color = Color(0xFF30205F), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        tr("${tracks.toAlbums().size} ألبوم", "${tracks.toAlbums().size} albums"),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        item { SectionHeader(tr("الوصول السريع", "Quick access")) }
        item {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    QuickAccessCard(
                        favoritesTitle,
                        favoriteIds.size.toString(),
                        Icons.Default.Favorite,
                        Color(0xFFFF5A83),
                    ) { onOpenTracks(favoritesTitle, favoriteIds.toList()) }
                }
                item {
                    QuickAccessCard(
                        recentlyPlayedTitle,
                        recent.size.toString(),
                        Icons.Default.History,
                        Color(0xFF7C4DFF),
                    ) { onOpenTracks(recentlyPlayedTitle, recent.map(Track::id)) }
                }
                item {
                    QuickAccessCard(
                        mostPlayedTitle,
                        mostPlayed.size.toString(),
                        Icons.Default.TrendingUp,
                        Color(0xFF26A69A),
                    ) { onOpenTracks(mostPlayedTitle, mostPlayed.map(Track::id)) }
                }
                item {
                    QuickAccessCard(
                        tr("المجلدات", "Folders"),
                        tracks.map(Track::folder).distinct().size.toString(),
                        Icons.Default.Folder,
                        Color(0xFFFF8A3D),
                        onOpenLibrary,
                    )
                }
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionHeader(recentlyPlayedTitle, tr("عرض الكل", "See all")) {
                onOpenTracks(recentlyPlayedTitle, recentIds)
            } }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    items(recent, key = Track::id) { track ->
                        SongCard(track) { onPlay(track, recent) }
                    }
                }
            }
        }

        if (albums.isNotEmpty()) {
            item { SectionHeader(tr("الألبومات", "Albums"), tr("المكتبة", "Library"), onOpenLibrary) }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    items(albums, key = Album::id) { album -> AlbumCard(album, onOpenAlbum) }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    tr("لا توجد أغانٍ بعد", "No songs yet"),
                    tr("اضغط تحديث بعد إضافة ملفات الصوت إلى الهاتف", "Add audio files to your phone, then refresh the library"),
                )
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        Modifier.width(154.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = .15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = color) }
            Spacer(Modifier.height(18.dp))
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text(
                tr("$count عنصر", "$count items"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SongCard(track: Track, onClick: () -> Unit) {
    Column(Modifier.width(142.dp).clickable(onClick = onClick)) {
        AlbumArt(track.artworkUri, Modifier.size(142.dp), 22)
        Spacer(Modifier.height(8.dp))
        Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(
            track.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AlbumCard(album: Album, onClick: (Album) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.width(154.dp).clickable { onClick(album) }) {
        AlbumArt(album.artworkUri, Modifier.size(154.dp), 24)
        Spacer(Modifier.height(8.dp))
        Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(
            album.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
