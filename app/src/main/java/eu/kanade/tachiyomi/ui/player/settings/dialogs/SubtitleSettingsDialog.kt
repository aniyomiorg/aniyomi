package eu.kanade.tachiyomi.ui.player.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.material.ReadItemAlpha

@Composable
fun SubtitleSettingsDialog(
    screenModel: PlayerSettingsScreenModel,
    onDismissRequest: () -> Unit,
) {
    val overrideSubtitles by screenModel.preferences.overrideSubtitlesStyle().collectAsState()
    val boldSubtitles by screenModel.preferences.boldSubtitles().collectAsState()
    val italicSubtitles by screenModel.preferences.italicSubtitles().collectAsState()

    val updateOverride = {
        val overrideType = if (overrideSubtitles) "no" else "force"
        screenModel.togglePreference(PlayerPreferences::overrideSubtitlesStyle)
        MPVLib.setPropertyString("sub-ass-override", overrideType)
    }

    val updateBold = {
        val toBold = if (boldSubtitles) "no" else "yes"
        screenModel.togglePreference(PlayerPreferences::boldSubtitles)
        MPVLib.setPropertyString("sub-bold", toBold)
    }

    val updateItalic = {
        val toItalicize = if (italicSubtitles) "no" else "yes"
        screenModel.togglePreference(PlayerPreferences::italicSubtitles)
        MPVLib.setPropertyString("sub-italic", toItalicize)
    }

    PlayerDialog(
        titleRes = R.string.player_subtitle_settings,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            CheckboxItem(
                label = stringResource(R.string.player_override_subtitle_style),
                checked = overrideSubtitles,
                onClick = updateOverride,
            )
            if (overrideSubtitles) {
                Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Outlined.FormatSize,
                            contentDescription = null,
                        )
                    }

                    IconButton(onClick = updateBold) {
                        val alpha = if (boldSubtitles) 1f else ReadItemAlpha
                        Icon(
                            imageVector = Icons.Outlined.FormatBold,
                            contentDescription = null,
                            modifier = Modifier.alpha(alpha),
                        )
                    }

                    IconButton(onClick = updateItalic) {
                        val alpha = if (italicSubtitles) 1f else ReadItemAlpha
                        Icon(
                            imageVector = Icons.Outlined.FormatItalic,
                            contentDescription = null,
                            modifier = Modifier.alpha(alpha),
                        )
                    }
                }
            }
        }
    }
}
