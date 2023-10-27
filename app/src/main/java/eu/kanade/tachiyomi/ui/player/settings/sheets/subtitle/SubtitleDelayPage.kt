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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.OutlinedNumericChooser
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib
import tachiyomi.presentation.core.components.material.padding

@Composable
fun SubtitleDelayPage(
    screenModel: PlayerSettingsScreenModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.tiny)) {
        val audioDelay by remember { mutableStateOf(screenModel.preferences.rememberAudioDelay()) }
        val subDelay by remember { mutableStateOf(screenModel.preferences.rememberSubtitlesDelay()) }

        screenModel.ToggleableRow(
            textRes = R.string.player_audio_remember_delay,
            isChecked = audioDelay.collectAsState().value,
            onClick = { screenModel.togglePreference { audioDelay } },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedNumericChooser(
                label = stringResource(id = R.string.player_audio_delay),
                placeholder = "0",
                suffix = "ms",
                value = (MPVLib.getPropertyDouble(Tracks.AUDIO.mpvProperty) * 1000).toInt(),
                step = 100,
                onValueChanged = {
                    MPVLib.setPropertyDouble(Tracks.AUDIO.mpvProperty, (it / 1000).toDouble())
                    screenModel.preferences.audioDelay().set(it)
                },
            )
        }

        screenModel.NoSubtitlesWarning()

        screenModel.ToggleableRow(
            textRes = R.string.player_subtitle_remember_delay,
            isChecked = subDelay.collectAsState().value,
            onClick = { screenModel.togglePreference { subDelay } },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedNumericChooser(
                label = stringResource(id = R.string.player_subtitle_delay),
                placeholder = "0",
                suffix = "ms",
                value = (MPVLib.getPropertyDouble(Tracks.SUBTITLES.mpvProperty) * 1000).toInt(),
                step = 100,
                onValueChanged = {
                    MPVLib.setPropertyDouble(Tracks.SUBTITLES.mpvProperty, (it / 1000).toDouble())
                    screenModel.preferences.subtitlesDelay().set(it)
                },
            )
        }
    }
}

private enum class Tracks(val mpvProperty: String) {
    SUBTITLES("sub-delay"),
    AUDIO("audio-delay"),
    ;
}
