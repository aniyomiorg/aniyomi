package eu.kanade.tachiyomi.ui.player.settings.dialogs

import android.graphics.Color
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.getAndSet
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.material.ReadItemAlpha

@Composable
fun SubtitleSettingsDialog(
    screenModel: PlayerSettingsScreenModel,
    onDismissRequest: () -> Unit,
) {
    val overrideSubtitles by screenModel.preferences.overrideSubtitlesStyle().collectAsState()

    val updateOverride = {
        val overrideType = if (overrideSubtitles) "no" else "force"
        screenModel.togglePreference(PlayerPreferences::overrideSubtitlesStyle)
        MPVLib.setPropertyString("sub-ass-override", overrideType)
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
                SubtitleLook(screenModel)

                SubtitleColors(screenModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.SubtitleLook(
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

    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
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
}

@Composable
private fun ColumnScope.SubtitleColors(
    screenModel: PlayerSettingsScreenModel,
) {

    SubtitleColorSlider(
        preference = screenModel.preferences.textColourSubtitles(),
        argb = ARGBValue.ALPHA
    )

    SubtitleColorSlider(
        preference = screenModel.preferences.textColourSubtitles(),
        argb = ARGBValue.RED
    )

    SubtitleColorSlider(
        preference = screenModel.preferences.textColourSubtitles(),
        argb = ARGBValue.GREEN
    )

    SubtitleColorSlider(
        preference = screenModel.preferences.textColourSubtitles(),
        argb = ARGBValue.BLUE
    )
}

@Composable
private fun ColumnScope.SubtitleColorSlider(
    preference: Preference<Int>,
    argb: ARGBValue,
) {
    val colorCode by preference.collectAsState()

    fun getColorValue(currentColor: Int, color: Float, mask: Long, bitShift: Int): Int {
        return (color.toInt() shl bitShift) or (currentColor and mask.inv().toInt())
    }

    Slider(
        value = argb.value(colorCode).toFloat(),
        onValueChange = { newColorValue ->
            preference.getAndSet { getColorValue(it, newColorValue, argb.mask, argb.bitShift) }
        },
        modifier = Modifier.weight(1.5f),
        valueRange = 0f..255f,
        steps = 255,
        // colors = SliderDefaults.colors(thumbColor = )),
    )
}

private enum class ARGBValue(@StringRes val title: Int, val mask: Long, val bitShift: Int, val value: (Int) -> Int) {
    ALPHA(R.string.color_filter_a_value, 0xFF000000L, 24, Color::alpha),
    RED(R.string.color_filter_r_value, 0x00FF0000L, 16, Color::red),
    GREEN(R.string.color_filter_g_value, 0x0000FF00L, 8, Color::green),
    BLUE(R.string.color_filter_b_value, 0x000000FFL, 0, Color::blue),
}
