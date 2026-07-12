package com.alomessi.harmonix.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class EqualizerState(
    val available: Boolean = false,
    val enabled: Boolean = true,
    val bandLevels: List<Float> = emptyList(),
    val frequencies: List<String> = emptyList(),
    val bassStrength: Float = 0f,
    val virtualizerStrength: Float = 0f,
    val virtualizerAvailable: Boolean = false,
    val reverbPreset: String = "none",
    val reverbAvailable: Boolean = false,
    val preset: String = "flat",
)

object AudioEffectsManager {
    private var appContext: Context? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var attachedSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        _state.value = readState(context)
    }

    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == attachedSessionId) return
        releaseInternal()
        attachedSessionId = audioSessionId

        val eq = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        if (eq == null) {
            _state.value = _state.value.copy(available = false)
            return
        }
        equalizer = eq
        bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        @Suppress("DEPRECATION")
        val createdVirtualizer = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
        virtualizer = createdVirtualizer
        presetReverb = runCatching { PresetReverb(0, 0) }.getOrNull()

        val enabled = _state.value.enabled
        runCatching { eq.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { createdVirtualizer?.enabled = enabled }
        runCatching { presetReverb?.enabled = enabled && _state.value.reverbPreset != "none" }

        val count = eq.numberOfBands.toInt()
        val previous = _state.value.bandLevels
        val levels = if (previous.size == count) previous else List(count) { 0f }
        levels.forEachIndexed { index, level -> runCatching { setBandInternal(eq, index, level) } }
        runCatching {
            bassBoost?.takeIf { it.strengthSupported }
                ?.setStrength((_state.value.bassStrength * 1_000f).roundToInt().toShort())
        }
        @Suppress("DEPRECATION")
        runCatching {
            createdVirtualizer?.takeIf { it.strengthSupported }
                ?.setStrength((_state.value.virtualizerStrength * 1_000f).roundToInt().toShort())
        }
        applyReverbInternal(_state.value.reverbPreset)

        _state.value = _state.value.copy(
            available = true,
            bandLevels = levels,
            frequencies = List(count) { formatFrequency(eq.getCenterFreq(it.toShort())) },
            virtualizerAvailable = createdVirtualizer?.strengthSupported == true,
            reverbAvailable = presetReverb != null,
        )
        persist()
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
        runCatching { presetReverb?.enabled = enabled && _state.value.reverbPreset != "none" }
        _state.value = _state.value.copy(enabled = enabled)
        persist()
    }

    fun setBand(index: Int, normalizedLevel: Float) {
        val level = normalizedLevel.coerceIn(-1f, 1f)
        equalizer?.let { runCatching { setBandInternal(it, index, level) } }
        val levels = _state.value.bandLevels.toMutableList()
        if (index in levels.indices) levels[index] = level
        _state.value = _state.value.copy(bandLevels = levels, preset = "custom")
        persist()
    }

    fun setBass(strength: Float) {
        val value = strength.coerceIn(0f, 1f)
        runCatching {
            bassBoost?.takeIf { it.strengthSupported }
                ?.setStrength((value * 1_000f).roundToInt().toShort())
        }
        _state.value = _state.value.copy(bassStrength = value)
        persist()
    }

    @Suppress("DEPRECATION")
    fun setVirtualizer(strength: Float) {
        val value = strength.coerceIn(0f, 1f)
        runCatching {
            virtualizer?.takeIf { it.strengthSupported }
                ?.setStrength((value * 1_000f).roundToInt().toShort())
        }
        _state.value = _state.value.copy(virtualizerStrength = value)
        persist()
    }

    fun setReverb(name: String) {
        val normalized = name.takeIf { it in REVERB_PRESETS } ?: "none"
        applyReverbInternal(normalized)
        _state.value = _state.value.copy(reverbPreset = normalized)
        persist()
    }

    fun reverbEffectId(): Int? = presetReverb?.id

    fun applyPreset(name: String) {
        val curve = when (name) {
            "bass" -> listOf(.85f, .62f, .22f, -.08f, -.18f)
            "rock" -> listOf(.55f, .28f, -.12f, .35f, .62f)
            "jazz" -> listOf(.38f, .12f, .18f, .38f, .48f)
            "classical" -> listOf(.32f, .16f, -.08f, .22f, .44f)
            "vocal" -> listOf(-.18f, -.05f, .42f, .58f, .22f)
            else -> listOf(0f, 0f, 0f, 0f, 0f)
        }
        val count = _state.value.bandLevels.size
        val mapped = when {
            count == 0 -> emptyList()
            count == 1 -> listOf(curve[curve.size / 2])
            else -> List(count) { index ->
                val source = index.toFloat() * (curve.lastIndex.toFloat() / (count - 1).toFloat())
                val low = source.toInt().coerceIn(0, curve.lastIndex)
                val high = (low + 1).coerceAtMost(curve.lastIndex)
                val fraction = source - low
                curve[low] + (curve[high] - curve[low]) * fraction
            }
        }
        equalizer?.let { eq -> mapped.forEachIndexed { index, value -> runCatching { setBandInternal(eq, index, value) } } }
        _state.value = _state.value.copy(bandLevels = mapped, preset = name)
        persist()
    }

    @Synchronized
    fun release() {
        releaseInternal()
        attachedSessionId = C.AUDIO_SESSION_ID_UNSET
        _state.value = _state.value.copy(
            available = false,
            virtualizerAvailable = false,
            reverbAvailable = false,
        )
    }

    private fun applyReverbInternal(name: String) {
        val effect = presetReverb ?: return
        val preset = when (name) {
            "small_room" -> PresetReverb.PRESET_SMALLROOM
            "medium_room" -> PresetReverb.PRESET_MEDIUMROOM
            "large_room" -> PresetReverb.PRESET_LARGEROOM
            "medium_hall" -> PresetReverb.PRESET_MEDIUMHALL
            "large_hall" -> PresetReverb.PRESET_LARGEHALL
            "plate" -> PresetReverb.PRESET_PLATE
            else -> PresetReverb.PRESET_NONE
        }
        runCatching {
            effect.preset = preset
            effect.enabled = _state.value.enabled && preset != PresetReverb.PRESET_NONE
        }
    }

    private fun releaseInternal() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
    }

    private fun setBandInternal(equalizer: Equalizer, index: Int, normalized: Float) {
        val range = equalizer.bandLevelRange
        val min = range[0].toInt()
        val max = range[1].toInt()
        val value = if (normalized >= 0f) normalized * max else -normalized * min
        equalizer.setBandLevel(index.toShort(), value.roundToInt().coerceIn(min, max).toShort())
    }

    private fun formatFrequency(milliHertz: Int): String {
        val hertz = milliHertz / 1_000f
        return if (hertz >= 1_000f) "%.1fk".format(hertz / 1_000f) else "${hertz.roundToInt()}Hz"
    }

    private fun persist() {
        val context = appContext ?: return
        val value = _state.value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, JSONObject().apply {
            put("enabled", value.enabled)
            put("bandLevels", JSONArray(value.bandLevels))
            put("bassStrength", value.bassStrength.toDouble())
            put("virtualizerStrength", value.virtualizerStrength.toDouble())
            put("reverbPreset", value.reverbPreset)
            put("preset", value.preset)
        }.toString()).apply()
    }

    private fun readState(context: Context): EqualizerState = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return EqualizerState()
        val json = JSONObject(raw)
        val bands = json.optJSONArray("bandLevels")
        EqualizerState(
            enabled = json.optBoolean("enabled", true),
            bandLevels = buildList {
                if (bands != null) for (index in 0 until bands.length()) add(bands.optDouble(index).toFloat())
            },
            bassStrength = json.optDouble("bassStrength", 0.0).toFloat(),
            virtualizerStrength = json.optDouble("virtualizerStrength", 0.0).toFloat(),
            reverbPreset = json.optString("reverbPreset", "none"),
            preset = json.optString("preset", "flat"),
        )
    }.getOrDefault(EqualizerState())

    private const val PREFS = "harmonix_audio_effects"
    private const val KEY = "state"
    private val REVERB_PRESETS = setOf(
        "none", "small_room", "medium_room", "large_room", "medium_hall", "large_hall", "plate",
    )
}
