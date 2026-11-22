package com.spectro.app.core.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bassBoost: Int = 0, // 0 - 1000
    val bandLevels: List<Int> = emptyList(), // levels in mB
    val bandFrequencies: List<Int> = emptyList(), // frequencies in Hz
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500,
    val presets: List<String> = emptyList(),
    val currentPreset: Int = -1 // -1 is Custom
)

object AudioEqualizer {
    private const val PREFS_NAME = "audio_equalizer_prefs"
    private const val KEY_ENABLED = "eq_enabled"
    private const val KEY_BASS = "eq_bass"
    private const val KEY_PRESET = "eq_preset"
    private const val KEY_BAND_PREFIX = "eq_band_"

    private var equalizer: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null
    private var currentSessionId: Int = 0
    private var prefs: SharedPreferences? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state = _state.asStateFlow()

    private val BuiltinPresets = listOf(
        "Datar (Flat)" to listOf(0f, 0f, 0f, 0f, 0f),
        "Bass Booster" to listOf(0.65f, 0.45f, 0.1f, 0f, -0.1f),
        "Vokal Jernih" to listOf(-0.2f, 0.1f, 0.55f, 0.4f, 0.1f),
        "Rock" to listOf(0.55f, 0.3f, -0.1f, 0.25f, 0.55f),
        "Pop" to listOf(-0.1f, 0.25f, 0.5f, 0.3f, -0.1f),
        "Jazz" to listOf(0.4f, 0.2f, -0.1f, 0.2f, 0.5f),
        "Hip-Hop" to listOf(0.65f, 0.5f, 0f, 0.2f, 0.35f),
        "EDM / Electronic" to listOf(0.6f, 0.4f, -0.1f, 0.35f, 0.55f),
        "Akustik" to listOf(0.4f, 0.3f, 0.2f, 0.3f, 0.4f),
        "Treble Booster" to listOf(-0.2f, -0.1f, 0f, 0.45f, 0.75f),
        "Deep Bass & Vocal" to listOf(0.7f, 0.35f, 0.45f, 0.2f, 0.1f)
    )

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs?.getBoolean(KEY_ENABLED, false) ?: false
        val savedBass = prefs?.getInt(KEY_BASS, 0) ?: 0
        val savedPreset = prefs?.getInt(KEY_PRESET, -1) ?: -1
        _state.value = _state.value.copy(
            isEnabled = isEnabled,
            bassBoost = savedBass,
            currentPreset = savedPreset
        )
    }

    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        currentSessionId = audioSessionId

        val isEnabled = prefs?.getBoolean(KEY_ENABLED, false) ?: false
        if (!isEnabled) {
            release()
            return
        }
        if (equalizer != null) return

        try {
            val eq = Equalizer(0, audioSessionId)
            val numBands = eq.numberOfBands.toInt()
            val min = eq.bandLevelRange[0].toInt()
            val max = eq.bandLevelRange[1].toInt()
            val freqs = (0 until numBands).map { eq.getCenterFreq(it.toShort()) / 1000 }
            val presetList = BuiltinPresets.map { it.first }

            val savedPreset = prefs?.getInt(KEY_PRESET, -1) ?: -1
            val savedLevels = (0 until numBands).map { band ->
                prefs?.getInt("$KEY_BAND_PREFIX$band", 0) ?: 0
            }

            if (savedPreset in BuiltinPresets.indices) {
                val (_, curve) = BuiltinPresets[savedPreset]
                (0 until numBands).forEach { band ->
                    val ratio = curve.getOrElse(band) { curve.lastOrNull() ?: 0f }
                    val level = if (ratio >= 0f) (ratio * max).toInt() else (-ratio * min).toInt()
                    runCatching { eq.setBandLevel(band.toShort(), level.coerceIn(min, max).toShort()) }
                }
            } else {
                savedLevels.forEachIndexed { band, level ->
                    runCatching { eq.setBandLevel(band.toShort(), level.coerceIn(min, max).toShort()) }
                }
            }
            runCatching { eq.enabled = true }
            equalizer = eq

            val actualLevels = (0 until numBands).map { runCatching { eq.getBandLevel(it.toShort()).toInt() }.getOrDefault(0) }

            val bb = BassBoost(0, audioSessionId)
            val savedBass = prefs?.getInt(KEY_BASS, 0) ?: 0
            if (runCatching { bb.strengthSupported }.getOrDefault(false)) {
                runCatching { bb.setStrength(savedBass.coerceIn(0, 1000).toShort()) }
            }
            runCatching { bb.enabled = true }
            bassBoostEffect = bb

            _state.value = _state.value.copy(
                isEnabled = true,
                bassBoost = savedBass,
                bandLevels = actualLevels,
                bandFrequencies = freqs,
                minLevel = min,
                maxLevel = max,
                presets = presetList,
                currentPreset = savedPreset
            )
        } catch (t: Throwable) {
            Log.e("AudioEqualizer", "Error attaching equalizer: ${t.message}")
            release()
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
            _state.value = _state.value.copy(isEnabled = enabled)
            if (enabled) {
                attach(currentSessionId)
            } else {
                release()
            }
        } catch (t: Throwable) {
            Log.e("AudioEqualizer", "Error toggling equalizer: ${t.message}")
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (bassBoostEffect?.strengthSupported == true) {
                bassBoostEffect?.setStrength(clamped.toShort())
            }
            prefs?.edit()?.putInt(KEY_BASS, clamped)?.apply()
            _state.value = _state.value.copy(bassBoost = clamped)
        } catch (e: Exception) {
            Log.e("AudioEqualizer", "Error setting bass boost: ${e.message}")
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        val eq = equalizer ?: return
        val min = _state.value.minLevel
        val max = _state.value.maxLevel
        val clamped = level.coerceIn(min, max)
        try {
            eq.setBandLevel(band.toShort(), clamped.toShort())
            prefs?.edit()?.putInt("$KEY_BAND_PREFIX$band", clamped)?.putInt(KEY_PRESET, -1)?.apply()
            val currentLevels = _state.value.bandLevels.toMutableList()
            if (band in currentLevels.indices) {
                currentLevels[band] = clamped
            }
            _state.value = _state.value.copy(bandLevels = currentLevels, currentPreset = -1)
        } catch (e: Exception) {
            Log.e("AudioEqualizer", "Error setting band level: ${e.message}")
        }
    }

    fun usePreset(presetIndex: Int) {
        val eq = equalizer ?: return
        val min = _state.value.minLevel
        val max = _state.value.maxLevel
        val numBands = eq.numberOfBands.toInt()
        try {
            if (presetIndex in BuiltinPresets.indices) {
                val (_, curve) = BuiltinPresets[presetIndex]
                val levels = (0 until numBands).map { band ->
                    val ratio = curve.getOrElse(band) { curve.lastOrNull() ?: 0f }
                    val level = if (ratio >= 0f) (ratio * max).toInt() else (-ratio * min).toInt()
                    val clamped = level.coerceIn(min, max)
                    runCatching { eq.setBandLevel(band.toShort(), clamped.toShort()) }
                    clamped
                }
                val editor = prefs?.edit()
                editor?.putInt(KEY_PRESET, presetIndex)
                levels.forEachIndexed { band, level ->
                    editor?.putInt("$KEY_BAND_PREFIX$band", level)
                }
                editor?.apply()
                _state.value = _state.value.copy(bandLevels = levels, currentPreset = presetIndex)
            }
        } catch (e: Exception) {
            Log.e("AudioEqualizer", "Error using preset: ${e.message}")
        }
    }

    @Synchronized
    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            // ignore
        }
        equalizer = null
        try {
            bassBoostEffect?.release()
        } catch (e: Exception) {
            // ignore
        }
        bassBoostEffect = null
    }
}
