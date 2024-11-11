package eu.kanade.tachiyomi.ui.player.settings.sheets.subtitle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.OutlinedNumericChooser
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun StreamsDelayPage(
    screenModel: PlayerSettingsScreenModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)) {
        val audioDelay by remember { mutableStateOf(screenModel.audioPreferences.rememberAudioDelay()) }
        val subDelay by remember { mutableStateOf(screenModel.subtitlePreferences.rememberSubtitlesDelay()) }
        var currentSubDelay by rememberSaveable {
            mutableStateOf(
                (MPVLib.getPropertyDouble(Streams.SUBTITLES.mpvProperty) * 1000)
                    .toInt(),
            )
        }
        var currentAudioDelay by rememberSaveable {
            mutableStateOf(
                (MPVLib.getPropertyDouble(Streams.AUDIO.mpvProperty) * 1000)
                    .toInt(),
            )
        }
        screenModel.ToggleableRow(
            textRes = MR.strings.player_audio_remember_delay,
            isChecked = audioDelay.collectAsState().value,
            onClick = { screenModel.togglePreference { audioDelay } },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedNumericChooser(
                label = stringResource(MR.strings.player_audio_delay),
                placeholder = "0",
                suffix = "ms",
                value = currentAudioDelay,
                step = 100,
                onValueChanged = {
                    MPVLib.setPropertyDouble(Streams.AUDIO.mpvProperty, it / 1000.0)
                    screenModel.audioPreferences.audioDelay().set(it)
                    currentAudioDelay = it
                },
            )
        }

        screenModel.NoSubtitlesWarning()

        screenModel.ToggleableRow(
            textRes = MR.strings.player_subtitle_remember_delay,
            isChecked = subDelay.collectAsState().value,
            onClick = { screenModel.togglePreference { subDelay } },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedNumericChooser(
                label = stringResource(MR.strings.player_subtitle_delay),
                placeholder = "0",
                suffix = "ms",
                value = currentSubDelay,
                step = 100,
                onValueChanged = {
                    MPVLib.setPropertyDouble(Streams.SUBTITLES.mpvProperty, it / 1000.0)
                    screenModel.subtitlePreferences.subtitlesDelay().set(it)
                    currentSubDelay = it
                },
            )
        }
    }
}

private enum class Streams(val mpvProperty: String) {
    SUBTITLES("sub-delay"),
    AUDIO("audio-delay"),
}
