package eu.kanade.tachiyomi.ui.player.settings.dialogs.subtitle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel

@Composable
fun SubtitleSettingsDialog(
    screenModel: PlayerSettingsScreenModel,
    onDismissRequest: () -> Unit,
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(id = R.string.player_subtitle_settings_delay_tab),
            stringResource(id = R.string.player_subtitle_settings_font_tab),
            stringResource(id = R.string.player_subtitle_settings_color_tab),
        ),
        hideSystemBars = true,
    ) { contentPadding, page ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> SubtitleDelayPage(screenModel)
                1 -> SubtitleFontPage(screenModel)
                2 -> SubtitleColorPage(screenModel)
            }
        }
    }
}

@Composable
fun SubtitlePreview(
    isBold: Boolean,
    isItalic: Boolean,
    textColor: Color,
    borderColor: Color,
    backgroundColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .background(color = backgroundColor),
    ) {
        Text(
            text = stringResource(R.string.player_subtitle_settings_example),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight =
                if (isBold) FontWeight.Bold else FontWeight.Medium,
                fontStyle =
                if (isItalic) FontStyle.Italic else FontStyle.Normal,
                shadow = Shadow(color = borderColor, blurRadius = 7.5f),
                color = textColor,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
