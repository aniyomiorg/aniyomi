package eu.kanade.tachiyomi.ui.player.settings.dialogs.subtitle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib
import tachiyomi.presentation.core.components.material.ReadItemAlpha

@Composable
fun SubtitleStylePage(screenModel: PlayerSettingsScreenModel) {
    val overrideSubtitles by screenModel.preferences.overrideSubtitlesStyle().collectAsState()

    val updateOverride = {
        val overrideType = if (overrideSubtitles) "no" else "force"
        screenModel.togglePreference(PlayerPreferences::overrideSubtitlesStyle)
        MPVLib.setPropertyString("sub-ass-override", overrideType)
    }
    Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { updateOverride() }),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(id = R.string.player_override_subtitle_style))
            Switch(
                checked = overrideSubtitles,
                onCheckedChange = { updateOverride() },
            )
        }
        if (overrideSubtitles) {
            SubtitleStyle(screenModel = screenModel)
        }
    }
}

@Composable
private fun SubtitleStyle(
    screenModel: PlayerSettingsScreenModel,
) {
    val boldSubtitles by screenModel.preferences.boldSubtitles().collectAsState()
    val italicSubtitles by screenModel.preferences.italicSubtitles().collectAsState()

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

    val textColor = Color(screenModel.preferences.textColorSubtitles().get())
    val borderColor = Color(screenModel.preferences.borderColorSubtitles().get())
    val backgroundColor = Color(screenModel.preferences.backgroundColorSubtitles().get())

    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Outlined.FormatSize,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )

        val boldAlpha = if (boldSubtitles) 1f else ReadItemAlpha
        Icon(
            imageVector = Icons.Outlined.FormatBold,
            contentDescription = null,
            modifier = Modifier
                .alpha(boldAlpha)
                .size(32.dp)
                .clickable(onClick = updateBold),
        )

        val italicAlpha = if (italicSubtitles) 1f else ReadItemAlpha
        Icon(
            imageVector = Icons.Outlined.FormatItalic,
            contentDescription = null,
            modifier = Modifier
                .alpha(italicAlpha)
                .size(32.dp)
                .clickable(onClick = updateItalic),
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SubtitlePreview(
            isBold = screenModel.preferences.boldSubtitles().get(),
            isItalic = screenModel.preferences.italicSubtitles().get(),
            textColor = textColor,
            borderColor = borderColor,
            backgroundColor = backgroundColor,
        )
    }
}
