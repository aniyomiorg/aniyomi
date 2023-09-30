package eu.kanade.tachiyomi.ui.player.settings.dialogs.subtitle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.RepeatingIconButton
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib

@Composable
fun SubtitleDelayPage(
    screenModel: PlayerSettingsScreenModel,
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        val subDelay by remember { mutableStateOf(screenModel.preferences.rememberSubtitlesDelay()) }
        val audioDelay by remember { mutableStateOf(screenModel.preferences.rememberAudioDelay()) }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(id = R.string.player_subtitle_delay), Modifier.width(80.dp))
            TrackDelay(
                onDelayChanged = {
                    MPVLib.setPropertyDouble(Tracks.SUBTITLES.mpvProperty, it)
                    if (screenModel.preferences.rememberSubtitlesDelay().get()) {
                        screenModel.preferences.subtitlesDelay().set(it.toFloat())
                    }
                },
                Tracks.SUBTITLES,
            )
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(id = R.string.player_audio_delay), Modifier.width(80.dp))
            TrackDelay(
                onDelayChanged = {
                    MPVLib.setPropertyDouble(Tracks.AUDIO.mpvProperty, it)
                    if (screenModel.preferences.rememberAudioDelay().get()) {
                        screenModel.preferences.audioDelay().set(it.toFloat())
                    }
                },
                Tracks.AUDIO,
            )
        }
    }
}

@Composable
private fun TrackDelay(
    onDelayChanged: (Double) -> Unit,
    track: Tracks,
) {
    var currentDelay by rememberSaveable { mutableStateOf(MPVLib.getPropertyDouble(track.mpvProperty)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RepeatingIconButton(
            onClick = {
                currentDelay -= 0.1
                onDelayChanged(currentDelay)
            },
        ) { Icon(imageVector = Icons.Outlined.RemoveCircle, contentDescription = null) }

        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = "%.2f".format(currentDelay),
            onValueChange = {
                // Don't allow multiple decimal points, non-numeric characters, or leading zeros
                currentDelay = it.trim().replace(Regex("[^-\\d.]"), "").toDoubleOrNull()
                    ?: currentDelay
            },
            label = { Text(text = stringResource(id = R.string.player_track_delay_text_field)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        RepeatingIconButton(
            onClick = {
                currentDelay += 0.1
                onDelayChanged(currentDelay)
            },
        ) { Icon(imageVector = Icons.Outlined.AddCircle, contentDescription = null) }
    }
}

private enum class Tracks(val mpvProperty: String) {
    SUBTITLES("sub-delay"),
    AUDIO("audio-delay"),
    ;
}
