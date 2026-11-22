package com.spectro.app.features.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectro.app.R
import com.spectro.app.core.designsystem.components.OverlaySheet
import com.spectro.app.core.designsystem.components.SpectroSheet
import com.spectro.app.core.designsystem.theme.DarkColors
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.core.playback.AudioEqualizer
import com.spectro.app.features.menu.CardMenuForm
import com.spectro.app.features.menu.CardMenuSection
import com.spectro.app.features.menu.CardSwitchRow
import com.spectro.app.features.menu.ItemMenuDimens
import com.spectro.app.features.menu.MenuCardGroup

object EqualizerSheet {
    fun show() {
        OverlaySheet.show {
            EqualizerSheetContent(onDismiss = OverlaySheet::dismiss)
        }
    }
}

@Composable
internal fun EqualizerSheetContent(onDismiss: () -> Unit) {
    val state by AudioEqualizer.state.collectAsState()

    DarkColors {
        SpectroSheet(onDismiss = onDismiss) {
            CardMenuForm(title = stringResource(R.string.equalizer)) {
                // Master Enable & Bass Boost in one compact card
                MenuCardGroup {
                    CardSwitchRow(
                        title = stringResource(R.string.equalizer_enabled),
                        checked = state.isEnabled,
                        onChange = { AudioEqualizer.setEnabled(it) }
                    )
                    if (state.isEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.equalizer_bass_boost),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${state.bassBoost / 10}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SpectroTheme.colors.playerAccent
                                )
                            }
                            Slider(
                                value = state.bassBoost / 1000f,
                                onValueChange = { AudioEqualizer.setBassBoost((it * 1000).toInt()) },
                                colors = SliderDefaults.colors(
                                    thumbColor = SpectroTheme.colors.playerAccent,
                                    activeTrackColor = SpectroTheme.colors.playerAccent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            )
                        }
                    }
                }

                if (state.isEnabled) {
                    // Presets Section (Compact Pills)
                    if (state.presets.isNotEmpty()) {
                        CardMenuSection(title = stringResource(R.string.equalizer_presets))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            itemsIndexed(state.presets) { index, presetName ->
                                val isSelected = state.currentPreset == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) SpectroTheme.colors.playerAccent
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .clickable { AudioEqualizer.usePreset(index) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = presetName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Compact Bands Section
                    if (state.bandLevels.isNotEmpty()) {
                        CardMenuSection(title = stringResource(R.string.equalizer_bands))
                        MenuCardGroup {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                state.bandLevels.forEachIndexed { bandIndex, level ->
                                    val freqHz = state.bandFrequencies.getOrNull(bandIndex) ?: 0
                                    val freqLabel = formatFrequency(freqHz)
                                    val dbValue = level / 100
                                    val dbLabel = if (dbValue > 0) "+$dbValue dB" else "$dbValue dB"
                                    val min = state.minLevel.toFloat()
                                    val max = state.maxLevel.toFloat()

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = freqLabel,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Slider(
                                            value = level.toFloat().coerceIn(min, max),
                                            valueRange = min..max,
                                            onValueChange = { newLevel ->
                                                AudioEqualizer.setBandLevel(bandIndex, newLevel.toInt())
                                            },
                                            colors = SliderDefaults.colors(
                                                thumbColor = SpectroTheme.colors.playerAccent,
                                                activeTrackColor = SpectroTheme.colors.playerAccent,
                                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .padding(horizontal = 4.dp)
                                        )
                                        Text(
                                            text = dbLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SpectroTheme.colors.playerAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFrequency(hz: Int): String = when {
    hz >= 1000 -> {
        val khz = hz / 1000f
        if (khz % 1f == 0f) "${khz.toInt()} kHz" else "${khz} kHz"
    }
    else -> "$hz Hz"
}
