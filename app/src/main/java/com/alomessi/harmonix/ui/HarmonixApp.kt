package com.alomessi.harmonix.ui

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alomessi.harmonix.data.Album
import com.alomessi.harmonix.data.Artist
import com.alomessi.harmonix.data.Genre
import com.alomessi.harmonix.data.MusicFolder
import com.alomessi.harmonix.data.Track
import com.alomessi.harmonix.data.UserPlaylist
import com.alomessi.harmonix.player.AudioEffectsManager
import com.alomessi.harmonix.ui.theme.HarmonixBlue
import com.alomessi.harmonix.ui.theme.HarmonixPink
import com.alomessi.harmonix.ui.theme.HarmonixPurple
import com.alomessi.harmonix.ui.theme.HarmonixTheme

private enum class RootTab { HOME, LIBRARY, PLAYLISTS, SETTINGS }

private sealed interface OverlayScreen {
    data object NowPlaying : OverlayScreen
    data object Equalizer : OverlayScreen
    data object Queue : OverlayScreen
    data object HiddenTracks : OverlayScreen
    data class Lyrics(val trackId: Long, val returnToPlayer: Boolean) : OverlayScreen
    data class Collection(val title: String, val subtitle: String, val trackIds: List<Long>) : OverlayScreen
    data class Playlist(val id: String) : OverlayScreen
}

@Composable
fun HarmonixApp(
    viewModel: HarmonixViewModel,
    onRequestAudioPermission: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onPickArtwork: (Track) -> Unit,
    onImportLyrics: (Track) -> Unit,
    onAddWidget: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val hiddenTracks by viewModel.hiddenTracks.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playCounts by viewModel.playCounts.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val equalizer by viewModel.equalizerState.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimerEndsAt.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()

    val layoutDirection = if (settings.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val context = LocalContext.current

    HarmonixTheme(settings) {
        CompositionLocalProvider(
            LocalLanguage provides settings.language,
            androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection,
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    if (!permissionGranted) {
                        PermissionScreen(onRequestAudioPermission)
                    } else {
                        var tab by rememberSaveable { mutableStateOf(RootTab.HOME) }
                        var overlay by remember { mutableStateOf<OverlayScreen?>(null) }
                        var actionTrack by remember { mutableStateOf<Track?>(null) }
                        var addTrack by remember { mutableStateOf<Track?>(null) }
                        var editTrack by remember { mutableStateOf<Track?>(null) }
                        var showTimer by remember { mutableStateOf(false) }
                        var showSpeed by remember { mutableStateOf(false) }
                        val currentTrack = allTracks.find { it.id == player.mediaId }

                        BackHandler(enabled = overlay != null) { overlay = null }

                        when (val screen = overlay) {
                            OverlayScreen.NowPlaying -> {
                                if (currentTrack != null) {
                                    NowPlayingScreen(
                                        track = currentTrack,
                                        state = player,
                                        isFavorite = currentTrack.id in favorites,
                                        sleepTimerEndsAt = sleepTimer,
                                        onBack = { overlay = null },
                                        onToggle = viewModel::togglePlayPause,
                                        onPrevious = viewModel::skipPrevious,
                                        onNext = viewModel::skipNext,
                                        onSeek = viewModel::seekTo,
                                        onShuffle = viewModel::toggleShuffle,
                                        onRepeat = viewModel::cycleRepeat,
                                        onFavorite = { viewModel.toggleFavorite(currentTrack.id) },
                                        onAddToPlaylist = { addTrack = currentTrack },
                                        onShare = { shareTrack(context, currentTrack) },
                                        onOpenQueue = { overlay = OverlayScreen.Queue },
                                        onOpenEqualizer = { overlay = OverlayScreen.Equalizer },
                                        onOpenTimer = { showTimer = true },
                                        onOpenLyrics = { overlay = OverlayScreen.Lyrics(currentTrack.id, true) },
                                        onOpenSpeed = { showSpeed = true },
                                    )
                                } else {
                                    EmptyState(tr("لا توجد أغنية قيد التشغيل", "Nothing is playing"), tr("اختر أغنية من المكتبة", "Choose a song from your library"))
                                }
                            }
                            OverlayScreen.Equalizer -> EqualizerScreen(
                                state = equalizer,
                                onBack = { overlay = null },
                                onEnabled = AudioEffectsManager::setEnabled,
                                onBand = AudioEffectsManager::setBand,
                                onBass = AudioEffectsManager::setBass,
                                onVirtualizer = AudioEffectsManager::setVirtualizer,
                                onReverb = AudioEffectsManager::setReverb,
                                onPreset = AudioEffectsManager::applyPreset,
                            )
                            OverlayScreen.Queue -> QueueScreen(allTracks, player, { overlay = OverlayScreen.NowPlaying }, viewModel::seekToQueueIndex)
                            OverlayScreen.HiddenTracks -> HiddenTracksScreen(hiddenTracks, { overlay = null }, viewModel::unhideTrack)
                            is OverlayScreen.Lyrics -> {
                                val lyricTrack = allTracks.find { it.id == screen.trackId }
                                if (lyricTrack == null) {
                                    EmptyState(tr("الأغنية غير موجودة", "Song not found"), tr("حدّث المكتبة وحاول مجددًا", "Refresh the library and try again"))
                                } else {
                                    LyricsScreen(
                                        lyricTrack,
                                        lyrics[lyricTrack.id].orEmpty(),
                                        onBack = { overlay = if (screen.returnToPlayer) OverlayScreen.NowPlaying else null },
                                        onSave = { viewModel.saveLyrics(lyricTrack.id, it) },
                                        onImport = { onImportLyrics(lyricTrack) },
                                    )
                                }
                            }
                            is OverlayScreen.Collection -> {
                                val collectionTracks = screen.trackIds.mapNotNull { id -> tracks.find { it.id == id } }
                                TrackCollectionScreen(
                                    title = screen.title,
                                    subtitle = screen.subtitle,
                                    tracks = collectionTracks,
                                    favoriteIds = favorites,
                                    currentTrackId = player.mediaId,
                                    onBack = { overlay = null },
                                    onPlayQueue = viewModel::playQueue,
                                    onFavorite = viewModel::toggleFavorite,
                                    onMore = { actionTrack = it },
                                )
                            }
                            is OverlayScreen.Playlist -> {
                                val playlist = playlists.find { it.id == screen.id }
                                if (playlist == null) {
                                    EmptyState(tr("القائمة غير موجودة", "Playlist not found"), tr("ربما تم حذفها", "It may have been deleted"))
                                } else {
                                    val playlistTracks = playlist.trackIds.mapNotNull { id -> tracks.find { it.id == id } }
                                    TrackCollectionScreen(
                                        title = playlist.name,
                                        subtitle = tr("${playlistTracks.size} أغنية", "${playlistTracks.size} songs"),
                                        tracks = playlistTracks,
                                        favoriteIds = favorites,
                                        currentTrackId = player.mediaId,
                                        onBack = { overlay = null },
                                        onPlayQueue = viewModel::playQueue,
                                        onFavorite = viewModel::toggleFavorite,
                                        onMore = { actionTrack = it },
                                        onRemove = { viewModel.removeFromPlaylist(playlist.id, it) },
                                    )
                                }
                            }
                            null -> {
                                Scaffold(
                                    bottomBar = {
                                        Column {
                                            if (currentTrack != null) {
                                                MiniPlayer(
                                                    currentTrack,
                                                    player,
                                                    onOpen = { overlay = OverlayScreen.NowPlaying },
                                                    onToggle = viewModel::togglePlayPause,
                                                )
                                            }
                                            NavigationBar {
                                                RootTab.entries.forEach { item ->
                                                    val icon = when (item) {
                                                        RootTab.HOME -> Icons.Default.Home
                                                        RootTab.LIBRARY -> Icons.Default.LibraryMusic
                                                        RootTab.PLAYLISTS -> Icons.Default.PlaylistPlay
                                                        RootTab.SETTINGS -> Icons.Default.Settings
                                                    }
                                                    val label = when (item) {
                                                        RootTab.HOME -> tr("الرئيسية", "Home")
                                                        RootTab.LIBRARY -> tr("المكتبة", "Library")
                                                        RootTab.PLAYLISTS -> tr("القوائم", "Playlists")
                                                        RootTab.SETTINGS -> tr("الإعدادات", "Settings")
                                                    }
                                                    NavigationBarItem(
                                                        selected = tab == item,
                                                        onClick = { tab = item },
                                                        icon = { Icon(icon, label) },
                                                        label = { Text(label) },
                                                    )
                                                }
                                            }
                                        }
                                    },
                                ) { padding ->
                                    val contentModifier = Modifier.fillMaxSize().padding(padding)
                                    when (tab) {
                                        RootTab.HOME -> HomeScreen(
                                            tracks, recent, playCounts, favorites,
                                            onPlay = viewModel::play,
                                            onOpenLibrary = { tab = RootTab.LIBRARY },
                                            onOpenTracks = { title, ids ->
                                                overlay = OverlayScreen.Collection(title, tr("${ids.size} أغنية", "${ids.size} songs"), ids)
                                            },
                                            onOpenAlbum = { album -> overlay = album.toOverlay() },
                                            modifier = contentModifier,
                                        )
                                        RootTab.LIBRARY -> LibraryScreen(
                                            tracks, favorites, player.mediaId, isScanning, settings.albumGrid,
                                            onRefresh = viewModel::refreshLibrary,
                                            onPlay = viewModel::play,
                                            onFavorite = viewModel::toggleFavorite,
                                            onTrackMore = { actionTrack = it },
                                            onOpenAlbum = { overlay = it.toOverlay() },
                                            onOpenArtist = { overlay = it.toOverlay() },
                                            onOpenFolder = { overlay = it.toOverlay() },
                                            onOpenGenre = { overlay = it.toOverlay() },
                                            modifier = contentModifier,
                                        )
                                        RootTab.PLAYLISTS -> PlaylistsScreen(
                                            tracks, favorites, recent, playCounts, playlists,
                                            onOpenTracks = { title, ids -> overlay = OverlayScreen.Collection(title, tr("${ids.size} أغنية", "${ids.size} songs"), ids) },
                                            onOpenPlaylist = { overlay = OverlayScreen.Playlist(it.id) },
                                            onCreatePlaylist = { viewModel.createPlaylist(it) },
                                            onRenamePlaylist = viewModel::renamePlaylist,
                                            onDeletePlaylist = viewModel::deletePlaylist,
                                            modifier = contentModifier,
                                        )
                                        RootTab.SETTINGS -> SettingsScreen(
                                            settings = settings,
                                            isScanning = isScanning,
                                            trackCount = tracks.size,
                                            hiddenTrackCount = hiddenTracks.size,
                                            sleepTimerEndsAt = sleepTimer,
                                            onLanguage = viewModel::setLanguage,
                                            onTheme = viewModel::setTheme,
                                            onAccent = viewModel::setAccent,
                                            onMinimumDuration = viewModel::setMinimumDuration,
                                            onMinimumSize = viewModel::setMinimumSize,
                                            onPauseOnDisconnect = viewModel::setPauseOnDisconnect,
                                            onRememberLastSong = viewModel::setRememberLastSong,
                                            onAlbumGrid = viewModel::setAlbumGrid,
                                            onRefresh = viewModel::refreshLibrary,
                                            onOpenEqualizer = { overlay = OverlayScreen.Equalizer },
                                            onOpenSleepTimer = { showTimer = true },
                                            onOpenHiddenTracks = { overlay = OverlayScreen.HiddenTracks },
                                            onAddWidget = onAddWidget,
                                            onExportBackup = onExportBackup,
                                            onImportBackup = onImportBackup,
                                            modifier = contentModifier,
                                        )
                                    }
                                }
                            }
                        }

                        actionTrack?.let { track ->
                            TrackActionsSheet(
                                track = track,
                                isFavorite = track.id in favorites,
                                onDismiss = { actionTrack = null },
                                onFavorite = { viewModel.toggleFavorite(track.id) },
                                onAddToPlaylist = { actionTrack = null; addTrack = track },
                                onShare = { shareTrack(context, track) },
                                onRingtone = { setRingtone(context, track) },
                                onEdit = { actionTrack = null; editTrack = track },
                                onLyrics = { actionTrack = null; overlay = OverlayScreen.Lyrics(track.id, false) },
                                onHide = { viewModel.hideTrack(track.id) },
                            )
                        }
                        editTrack?.let { selected ->
                            val track = allTracks.find { it.id == selected.id } ?: selected
                            EditTagDialog(
                                track = track,
                                onDismiss = { editTrack = null },
                                onSave = { viewModel.saveTrackOverride(track.id, it) },
                                onPickArtwork = { onPickArtwork(track) },
                                onReset = { viewModel.resetTrackOverride(track.id) },
                            )
                        }
                        addTrack?.let { track ->
                            AddToPlaylistSheet(
                                track, playlists,
                                onDismiss = { addTrack = null },
                                onAdd = { viewModel.addToPlaylist(it, track.id) },
                                onCreate = viewModel::createPlaylist,
                            )
                        }
                        if (showTimer) {
                            SleepTimerDialog(
                                isActive = sleepTimer != null,
                                onDismiss = { showTimer = false },
                                onSet = viewModel::setSleepTimer,
                                onCancel = viewModel::cancelSleepTimer,
                            )
                        }
                        if (showSpeed) {
                            PlaybackSpeedDialog(
                                current = player.playbackSpeed,
                                onDismiss = { showSpeed = false },
                                onSelect = viewModel::setPlaybackSpeed,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(HarmonixPurple, HarmonixPink, HarmonixBlue)),
            ),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Storage, null, tint = Color.White, modifier = Modifier.size(54.dp)) }
        Spacer(Modifier.height(26.dp))
        Text(tr("دع موسيقاك تبدأ", "Let your music begin"), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Text(
            tr(
                "يحتاج Harmonix إلى إذن قراءة ملفات الصوت لعرض الأغاني الموجودة على هاتفك وتشغيلها. لا يتم رفع أي ملف إلى الإنترنت.",
                "Harmonix needs audio access to find and play songs stored on your phone. Nothing is uploaded to the internet.",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onRequest) {
            Icon(Icons.Default.Lock, null)
            Text(tr("السماح بالوصول للموسيقى", "Allow music access"), Modifier.padding(horizontal = 8.dp))
        }
    }
}

private fun Album.toOverlay() = OverlayScreen.Collection(name, "$artist • ${tracks.size}", tracks.map(Track::id))
private fun Artist.toOverlay() = OverlayScreen.Collection(name, "${tracks.size}", tracks.map(Track::id))
private fun MusicFolder.toOverlay() = OverlayScreen.Collection(name, path, tracks.map(Track::id))
private fun Genre.toOverlay() = OverlayScreen.Collection(name, "${tracks.size}", tracks.map(Track::id))

private fun shareTrack(context: android.content.Context, track: Track) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = track.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, Uri.parse(track.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, track.title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Toast.makeText(context, "Unable to share this file", Toast.LENGTH_SHORT).show()
    }
}

private fun setRingtone(context: android.content.Context, track: Track) {
    if (!Settings.System.canWrite(context)) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "Allow system settings access, then try again", Toast.LENGTH_LONG).show()
        return
    }
    runCatching {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, Uri.parse(track.uri))
    }.onSuccess {
        Toast.makeText(context, "Ringtone updated", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Could not set this file as ringtone", Toast.LENGTH_SHORT).show()
    }
}
