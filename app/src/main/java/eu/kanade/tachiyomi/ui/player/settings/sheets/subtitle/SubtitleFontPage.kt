package eu.kanade.tachiyomi.ui.player.settings.sheets.subtitle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.OutlinedNumericChooser
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib
import tachiyomi.presentation.core.components.material.ReadItemAlpha
import tachiyomi.presentation.core.components.material.padding

@Composable
fun SubtitleFontPage(screenModel: PlayerSettingsScreenModel) {
    screenModel.OverrideSubtitlesSwitch {
        SubtitleFont(screenModel = screenModel)
    }
}

@Composable
private fun SubtitleFont(
    screenModel: PlayerSettingsScreenModel,
) {
    val boldSubtitles by screenModel.preferences.boldSubtitles().collectAsState()
    val italicSubtitles by screenModel.preferences.italicSubtitles().collectAsState()
    val subtitleFontSize by screenModel.preferences.subtitleFontSize().collectAsState()
    val textColor by screenModel.preferences.textColorSubtitles().collectAsState()
    val borderColor by screenModel.preferences.borderColorSubtitles().collectAsState()
    val backgroundColor by screenModel.preferences.backgroundColorSubtitles().collectAsState()

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

    val onSizeChanged: (Int) -> Unit = {
        MPVLib.setPropertyInt("sub-font-size", it)
        screenModel.preferences.subtitleFontSize().set(it)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.tiny),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Outlined.FormatSize,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )

            OutlinedNumericChooser(
                label = stringResource(id = R.string.player_font_size_text_field),
                placeholder = "55",
                suffix = "",
                value = subtitleFontSize,
                step = 1,
                min = 1,
                onValueChanged = onSizeChanged,
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

        SubtitlePreview(
            isBold = boldSubtitles,
            isItalic = italicSubtitles,
            textColor = Color(textColor),
            borderColor = Color(borderColor),
            backgroundColor = Color(backgroundColor),
        )
    }
}
