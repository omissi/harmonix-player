package com.alomessi.harmonix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alomessi.harmonix.BuildConfig
import com.alomessi.harmonix.data.AppSettings
import com.alomessi.harmonix.util.formatFileSize

@Composable
fun SettingsScreen(
    settings: AppSettings,
    isScanning: Boolean,
    trackCount: Int,
    hiddenTrackCount: Int,
    sleepTimerEndsAt: Long?,
    onLanguage: (String) -> Unit,
    onTheme: (String) -> Unit,
    onAccent: (String) -> Unit,
    onMinimumDuration: (Long) -> Unit,
    onMinimumSize: (Long) -> Unit,
    onPauseOnDisconnect: (Boolean) -> Unit,
    onRememberLastSong: (Boolean) -> Unit,
    onAlbumGrid: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenHiddenTracks: () -> Unit,
    onAddWidget: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choiceDialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(tr("الإعدادات", "Settings"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(tr("خصّص تطبيق Harmonix", "Make Harmonix yours"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item { SettingsLabel(tr("المظهر واللغة", "Appearance & language")) }
        item {
            SettingsCard {
                SettingItem(
                    Icons.Default.Language,
                    tr("اللغة", "Language"),
                    if (settings.language == "ar") "العربية" else "English",
                ) { choiceDialog = "language" }
                SettingItem(
                    Icons.Default.DarkMode,
                    tr("المظهر", "Theme"),
                    when (settings.theme) {
                        "light" -> tr("فاتح", "Light")
                        "dark" -> tr("داكن", "Dark")
                        else -> tr("حسب النظام", "System default")
                    },
                ) { choiceDialog = "theme" }
                SettingItem(
                    Icons.Default.ColorLens,
                    tr("لون التطبيق", "App color"),
                    when (settings.accent) {
                        "blue" -> tr("أزرق", "Blue")
                        "orange" -> tr("برتقالي", "Orange")
                        "pink" -> tr("وردي", "Pink")
                        else -> tr("بنفسجي", "Violet")
                    },
                ) { choiceDialog = "accent" }
            }
        }

        item { SettingsLabel(tr("الصوت والتشغيل", "Audio & playback")) }
        item {
            SettingsCard {
                SettingItem(Icons.Default.Equalizer, tr("معادل الصوت", "Equalizer"), tr("الجهير والإعدادات الجاهزة", "Bass boost and presets"), onOpenEqualizer)
                SettingItem(
                    Icons.Default.Timer,
                    tr("مؤقت النوم", "Sleep timer"),
                    if (sleepTimerEndsAt == null) tr("غير مفعّل", "Off") else tr("مفعّل الآن", "Active now"),
                    onOpenSleepTimer,
                )
                SettingSwitch(
                    Icons.Default.Headphones,
                    tr("إيقاف عند فصل السماعة", "Pause on headset disconnect"),
                    tr("يمنع استمرار الصوت من مكبر الهاتف", "Prevents unexpected speaker playback"),
                    settings.pauseOnHeadsetDisconnect,
                    onPauseOnDisconnect,
                )
                SettingSwitch(
                    Icons.Default.History,
                    tr("تذكّر آخر تشغيل", "Remember last playback"),
                    tr("استعادة القائمة والموضع عند فتح التطبيق", "Restore the queue and position when the app opens"),
                    settings.rememberLastSong,
                    onRememberLastSong,
                )
            }
        }

        item { SettingsLabel(tr("المكتبة", "Library")) }
        item {
            SettingsCard {
                SettingItem(
                    Icons.Default.Refresh,
                    tr("فحص ملفات الصوت", "Scan audio files"),
                    tr("$trackCount أغنية في المكتبة", "$trackCount songs in library"),
                    onRefresh,
                    trailing = {
                        if (isScanning) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    },
                )
                SettingItem(
                    Icons.Default.Info,
                    tr("الحد الأدنى لمدة الأغنية", "Minimum song length"),
                    if (settings.minimumDurationMs == 0L) tr("بدون حد", "No limit")
                    else tr("${settings.minimumDurationMs / 1_000} ثانية", "${settings.minimumDurationMs / 1_000} seconds"),
                ) { choiceDialog = "duration" }
                SettingItem(
                    Icons.Default.Info,
                    tr("الحد الأدنى لحجم الملف", "Minimum file size"),
                    if (settings.minimumSizeBytes == 0L) tr("بدون حد", "No limit") else formatFileSize(settings.minimumSizeBytes),
                ) { choiceDialog = "size" }
                SettingItem(
                    Icons.Default.VisibilityOff,
                    tr("الأغاني المخفية", "Hidden songs"),
                    tr("$hiddenTrackCount أغنية مخفية", "$hiddenTrackCount hidden songs"),
                    onOpenHiddenTracks,
                )
                SettingSwitch(
                    Icons.Default.GridView,
                    tr("عرض الألبومات كشبكة", "Album grid"),
                    tr("بطاقات كبيرة لأغلفة الألبومات", "Large album artwork cards"),
                    settings.albumGrid,
                    onAlbumGrid,
                )
            }
        }

        item { SettingsLabel(tr("الشاشة الرئيسية", "Home screen")) }
        item {
            SettingsCard {
                SettingItem(
                    Icons.Default.Widgets,
                    tr("إضافة Widget للموسيقى", "Add music widget"),
                    tr("تحكم بالتشغيل من الشاشة الرئيسية", "Control playback from your home screen"),
                    onAddWidget,
                )
            }
        }

        item { SettingsLabel(tr("النسخ الاحتياطي", "Backup")) }
        item {
            SettingsCard {
                SettingItem(Icons.Default.Backup, tr("تصدير نسخة احتياطية", "Export backup"), tr("القوائم والمفضلة والكلمات والتعديلات", "Playlists, favorites, lyrics and edits"), onExportBackup)
                SettingItem(Icons.Default.Restore, tr("استعادة نسخة احتياطية", "Restore backup"), tr("اختر ملف Harmonix JSON", "Choose a Harmonix JSON file"), onImportBackup)
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth().padding(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Equalizer, null, tint = MaterialTheme.colorScheme.onPrimary) }
                    Spacer(Modifier.height(10.dp))
                    Text("Harmonix Player", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("v${BuildConfig.VERSION_NAME} • ALOMESSI TECH", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("مشغّل موسيقى محلي سريع، خاص وبدون إعلانات", "A fast, private and ad-free local music player"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }

    when (choiceDialog) {
        "language" -> ChoiceDialog(
            tr("اختر اللغة", "Choose language"), settings.language,
            listOf("ar" to "العربية", "en" to "English"),
            { choiceDialog = null }, { onLanguage(it); choiceDialog = null },
        )
        "theme" -> ChoiceDialog(
            tr("اختر المظهر", "Choose theme"), settings.theme,
            listOf("system" to tr("حسب النظام", "System default"), "light" to tr("فاتح", "Light"), "dark" to tr("داكن", "Dark")),
            { choiceDialog = null }, { onTheme(it); choiceDialog = null },
        )
        "accent" -> ChoiceDialog(
            tr("لون التطبيق", "App color"), settings.accent,
            listOf("violet" to tr("بنفسجي", "Violet"), "blue" to tr("أزرق", "Blue"), "pink" to tr("وردي", "Pink"), "orange" to tr("برتقالي", "Orange")),
            { choiceDialog = null }, { onAccent(it); choiceDialog = null },
        )
        "duration" -> ChoiceDialog(
            tr("الحد الأدنى للمدة", "Minimum duration"), settings.minimumDurationMs.toString(),
            listOf(0L, 5_000L, 10_000L, 30_000L, 60_000L).map {
                it.toString() to if (it == 0L) tr("بدون حد", "No limit") else tr("${it / 1_000} ثانية", "${it / 1_000} seconds")
            },
            { choiceDialog = null }, { onMinimumDuration(it.toLong()); choiceDialog = null },
        )
        "size" -> ChoiceDialog(
            tr("الحد الأدنى للحجم", "Minimum file size"), settings.minimumSizeBytes.toString(),
            listOf(0L, 100_000L, 500_000L, 1_000_000L, 5_000_000L).map {
                it.toString() to if (it == 0L) tr("بدون حد", "No limit") else formatFileSize(it)
            },
            { choiceDialog = null }, { onMinimumSize(it.toLong()); choiceDialog = null },
        )
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
    ) { Column(content = content) }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = trailing,
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SettingSwitch(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChecked) },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onChecked(!checked) },
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    selected: String,
    options: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == value, onClick = { onSelect(value) })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("إغلاق", "Close")) } },
    )
}
