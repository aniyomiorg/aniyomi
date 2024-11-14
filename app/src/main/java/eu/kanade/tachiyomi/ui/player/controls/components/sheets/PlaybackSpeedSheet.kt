package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.player.components.PlayerSheet
import eu.kanade.presentation.player.components.SliderItem
import eu.kanade.presentation.player.components.SwitchPreference
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import `is`.xyz.mpv.MPVLib
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.MPVKtSpacing
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun PlaybackSpeedSheet(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = remember { Injekt.get<PlayerPreferences>() }
    val audioPreferences = remember { Injekt.get<AudioPreferences>() }
    PlayerSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = MaterialTheme.MPVKtSpacing.medium),
        ) {
            SliderItem(
                label = stringResource(MR.strings.player_sheets_speed_slider_label),
                value = speed,
                valueText = stringResource(MR.strings.player_speed, speed),
                onChange = onSpeedChange,
                max = 6f,
                min = 0.01f,
            )
            val playbackSpeedPresets by preferences.speedPresets().collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.MPVKtSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.medium),
            ) {
                FilledTonalIconButton(onClick = {
                    preferences.speedPresets().delete()
                }) {
                    Icon(Icons.Default.RestartAlt, null)
                }
                LazyRow(
                    modifier = Modifier
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.extraSmall),
                ) {
                    items(
                        playbackSpeedPresets.map { it.toFloat() }.sorted(),
                        key = { it }
                    ) {
                        InputChip(
                            selected = speed == it,
                            onClick = { onSpeedChange(it) },
                            label = { Text(stringResource(MR.strings.player_speed, it)) },
                            modifier = Modifier
                                .animateItem(),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    modifier = Modifier
                                        .clickable { preferences.speedPresets().set(playbackSpeedPresets.minus(it.toFixed(2).toString())) },
                                )
                            },
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = {
                        preferences.speedPresets().set(playbackSpeedPresets.plus(speed.toFixed(2).toString()))
                    },
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
            val pitchCorrection by audioPreferences.enablePitchCorrection().collectAsState()
            SwitchPreference(
                value = pitchCorrection,
                onValueChange = {
                    audioPreferences.enablePitchCorrection().set(it)
                    MPVLib.setPropertyBoolean("audio-pitch-correction", it)
                },
                content = {
                    Column {
                        Text(text = stringResource(MR.strings.pref_audio_pitch_correction_title))
                        Text(
                            text = stringResource(MR.strings.pref_audio_pitch_correction_summary),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.MPVKtSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.smaller),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { preferences.playerSpeed().set(speed) },
                ) {
                    Text(text = stringResource(MR.strings.player_sheets_speed_make_default))
                }
                FilledIconButton(
                    onClick = {
                        preferences.playerSpeed().delete()
                        onSpeedChange(1f)
                    },
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                }
            }
        }
    }
}

fun Float.toFixed(precision: Int = 1): Float {
    val factor = 10.0f.pow(precision)
    return (this * factor).roundToInt() / factor
}
