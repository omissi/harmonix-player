package com.alomessi.harmonix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alomessi.harmonix.player.EqualizerState

@Composable
fun EqualizerScreen(
    state: EqualizerState,
    onBack: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onBand: (Int, Float) -> Unit,
    onBass: (Float) -> Unit,
    onVirtualizer: (Float) -> Unit,
    onReverb: (String) -> Unit,
    onPreset: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("رجوع", "Back")) }
            Column(Modifier.weight(1f)) {
                Text(tr("معادل الصوت", "Equalizer"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(tr("خصّص تجربة الاستماع", "Shape your listening experience"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.enabled, onCheckedChange = onEnabled, enabled = state.available)
        }

        if (!state.available) {
            EmptyState(
                tr("ابدأ تشغيل أغنية", "Start playing a song"),
                tr("سيتم تفعيل معادل الصوت بمجرد بدء التشغيل", "The equalizer becomes available when playback starts"),
                Modifier.fillMaxWidth(),
            )
            return@Column
        }

        Text(tr("الإعدادات الجاهزة", "Presets"), Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontWeight = FontWeight.Bold)
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf("flat", "bass", "rock", "jazz", "classical", "vocal")) { preset ->
                val label = when (preset) {
                    "bass" -> tr("جهير", "Bass")
                    "rock" -> tr("روك", "Rock")
                    "jazz" -> tr("جاز", "Jazz")
                    "classical" -> tr("كلاسيكي", "Classical")
                    "vocal" -> tr("صوت", "Vocal")
                    else -> tr("مسطح", "Flat")
                }
                FilterChip(selected = state.preset == preset, onClick = { onPreset(preset) }, label = { Text(label) })
            }
        }

        Card(
            Modifier.fillMaxWidth().padding(18.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Equalizer, null, tint = MaterialTheme.colorScheme.primary)
                    Text(tr("نطاقات التردد", "Frequency bands"), Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                state.bandLevels.forEachIndexed { index, level ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.frequencies.getOrNull(index).orEmpty(), Modifier.weight(.25f), style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = level,
                            onValueChange = { onBand(index, it) },
                            valueRange = -1f..1f,
                            enabled = state.enabled,
                            modifier = Modifier.weight(.75f),
                        )
                        Text("%+.0f".format(level * 10), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Card(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary)
                    Text(tr("تعزيز الجهير", "Bass boost"), Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${(state.bassStrength * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = state.bassStrength, onValueChange = onBass, enabled = state.enabled)
                if (state.virtualizerAvailable) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("اتساع الصوت", "Virtualizer"), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${(state.virtualizerStrength * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(value = state.virtualizerStrength, onValueChange = onVirtualizer, enabled = state.enabled)
                }
            }
        }

        if (state.reverbAvailable) {
            Text(tr("صدى المكان", "Reverb"), Modifier.padding(horizontal = 20.dp, vertical = 12.dp), fontWeight = FontWeight.Bold)
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf("none", "small_room", "medium_room", "large_room", "medium_hall", "large_hall", "plate")) { reverb ->
                    val label = when (reverb) {
                        "small_room" -> tr("غرفة صغيرة", "Small room")
                        "medium_room" -> tr("غرفة متوسطة", "Medium room")
                        "large_room" -> tr("غرفة كبيرة", "Large room")
                        "medium_hall" -> tr("قاعة متوسطة", "Medium hall")
                        "large_hall" -> tr("قاعة كبيرة", "Large hall")
                        "plate" -> tr("معدني", "Plate")
                        else -> tr("بدون", "None")
                    }
                    FilterChip(selected = state.reverbPreset == reverb, onClick = { onReverb(reverb) }, label = { Text(label) })
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
