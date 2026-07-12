package com.alomessi.harmonix.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alomessi.harmonix.data.Album
import com.alomessi.harmonix.data.Artist
import com.alomessi.harmonix.data.Genre
import com.alomessi.harmonix.data.MusicFolder
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.data.toAlbums
import com.alomessi.harmonix.data.toArtists
import com.alomessi.harmonix.data.toFolders
import com.alomessi.harmonix.data.toGenres

@Composable
fun LibraryScreen(
    tracks: List<Track>,
    favoriteIds: Set<Long>,
    currentTrackId: Long?,
    isScanning: Boolean,
    albumGrid: Boolean,
    onRefresh: () -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onFavorite: (Long) -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenFolder: (MusicFolder) -> Unit,
    onOpenGenre: (Genre) -> Unit,
    modifier: Modifier = Modifier,
) {
    var category by rememberSaveable { mutableStateOf("songs") }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("title") }
    var sortMenu by remember { mutableStateOf(false) }
    val searched = remember(tracks, query) {
        if (query.isBlank()) tracks else tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) ||
                it.album.contains(query, true) || it.folder.contains(query, true) || it.genre.contains(query, true)
        }
    }
    val sorted = remember(searched, sort) {
        when (sort) {
            "artist" -> searched.sortedBy { it.artist.lowercase() }
            "album" -> searched.sortedBy { it.album.lowercase() }
            "newest" -> searched.sortedByDescending(Track::dateAdded)
            "duration" -> searched.sortedByDescending(Track::duration)
            else -> searched.sortedBy { it.title.lowercase() }
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(tr("مكتبتي", "My library"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    tr("${tracks.size} أغنية على هذا الجهاز", "${tracks.size} songs on this device"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh, enabled = !isScanning) {
                if (isScanning) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, tr("تحديث", "Refresh"))
            }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, tr("فرز", "Sort")) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf(
                        "title" to tr("العنوان", "Title"),
                        "artist" to tr("الفنان", "Artist"),
                        "album" to tr("الألبوم", "Album"),
                        "newest" to tr("الأحدث", "Newest"),
                        "duration" to tr("المدة", "Duration"),
                    ).forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { sort = value; sortMenu = false })
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            placeholder = { Text(tr("ابحث في الأغاني والفنانين والألبومات", "Search songs, artists and albums")) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "songs" to tr("الأغاني", "Songs"),
                "albums" to tr("الألبومات", "Albums"),
                "artists" to tr("الفنانون", "Artists"),
                "folders" to tr("المجلدات", "Folders"),
                "genres" to tr("الأنواع", "Genres"),
            ).forEach { (value, label) ->
                FilterChip(selected = category == value, onClick = { category = value }, label = { Text(label) })
            }
        }

        when (category) {
            "albums" -> AlbumGrid(sorted.toAlbums(), albumGrid, onOpenAlbum, Modifier.weight(1f))
            "artists" -> ArtistList(sorted.toArtists(), onOpenArtist, Modifier.weight(1f))
            "folders" -> FolderList(sorted.toFolders(), onOpenFolder, Modifier.weight(1f))
            "genres" -> GenreList(sorted.toGenres(), onOpenGenre, Modifier.weight(1f))
            else -> {
                if (sorted.isEmpty()) EmptyState(
                    tr("لا توجد نتائج", "No results"),
                    tr("جرّب كلمة بحث أخرى أو حدّث المكتبة", "Try another search or refresh the library"),
                    Modifier.weight(1f),
                ) else LazyColumn(Modifier.weight(1f)) {
                    listItems(sorted, key = Track::id) { track ->
                        TrackRow(
                            track = track,
                            isCurrent = currentTrackId == track.id,
                            isFavorite = track.id in favoriteIds,
                            onClick = { onPlay(track, sorted) },
                            onFavorite = { onFavorite(track.id) },
                            onMore = { onTrackMore(track) },
                        )
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>, grid: Boolean, onOpen: (Album) -> Unit, modifier: Modifier) {
    if (albums.isEmpty()) {
        EmptyState(tr("لا توجد ألبومات", "No albums"), tr("ستظهر الألبومات هنا بعد الفحص", "Albums appear here after scanning"), modifier)
    } else if (grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = modifier,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            gridItems(albums, key = { "${it.id}:${it.name}" }) { album ->
                AlbumCard(album, onOpen, Modifier.fillMaxWidth())
            }
        }
    } else {
        LazyColumn(modifier) {
            listItems(albums, key = { "${it.id}:${it.name}" }) { album -> CollectionRow(
                title = album.name,
                subtitle = tr("${album.tracks.size} أغنية • ${album.artist}", "${album.tracks.size} songs • ${album.artist}"),
                icon = { AlbumArt(album.artworkUri, Modifier.size(58.dp), 15) },
                onClick = { onOpen(album) },
            ) }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

@Composable
private fun ArtistList(artists: List<Artist>, onOpen: (Artist) -> Unit, modifier: Modifier) {
    LazyColumn(modifier) {
        listItems(artists, key = Artist::name) { artist ->
            CollectionRow(
                title = artist.name,
                subtitle = tr("${artist.tracks.size} أغنية • ${artist.albums} ألبوم", "${artist.tracks.size} songs • ${artist.albums} albums"),
                icon = { CollectionIcon(Icons.Default.Person) },
                onClick = { onOpen(artist) },
            )
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun FolderList(folders: List<MusicFolder>, onOpen: (MusicFolder) -> Unit, modifier: Modifier) {
    LazyColumn(modifier) {
        listItems(folders, key = MusicFolder::path) { folder ->
            CollectionRow(
                title = folder.name,
                subtitle = tr("${folder.tracks.size} أغنية", "${folder.tracks.size} songs"),
                icon = { CollectionIcon(Icons.Default.Folder) },
                onClick = { onOpen(folder) },
            )
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun GenreList(genres: List<Genre>, onOpen: (Genre) -> Unit, modifier: Modifier) {
    LazyColumn(modifier) {
        listItems(genres, key = Genre::name) { genre ->
            CollectionRow(
                title = genre.name,
                subtitle = tr("${genre.tracks.size} أغنية", "${genre.tracks.size} songs"),
                icon = { CollectionIcon(Icons.Default.Category) },
                onClick = { onOpen(genre) },
            )
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun CollectionRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CollectionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        Modifier.size(58.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) } }
}
