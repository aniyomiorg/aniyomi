package eu.kanade.tachiyomi.ui.player.settings.dialogs.subtitle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.OutlinedNumericChooser
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib

@Composable
fun SubtitleDelayPage(
    screenModel: PlayerSettingsScreenModel,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        val audioDelay by remember { mutableStateOf(screenModel.preferences.rememberAudioDelay()) }
        val subDelay by remember { mutableStateOf(screenModel.preferences.rememberSubtitlesDelay()) }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { screenModel.togglePreference { audioDelay } }),
        ) {
            Text(text = stringResource(id = R.string.player_audio_remember_delay))
            Switch(
                checked = audioDelay.collectAsState().value,
                onCheckedChange = { screenModel.togglePreference { audioDelay } },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { screenModel.togglePreference { subDelay } }),
        ) {
            Text(text = stringResource(id = R.string.player_subtitle_remember_delay))
            Switch(
                checked = subDelay.collectAsState().value,
                onCheckedChange = { screenModel.togglePreference { subDelay } },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
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
