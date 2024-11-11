package eu.kanade.tachiyomi.ui.player.settings.sheets.subtitle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun FiltersPage(
    screenModel: PlayerSettingsScreenModel,
    modifier: Modifier = Modifier,
) {
    if (!screenModel.decoderPreferences.gpuNext().get()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Outlined.Info, null)
            Text(stringResource(MR.strings.player_filters_warning))
        }
    }
    Column(modifier) {
        VideoFilters.entries.forEach { filter ->
            val value by filter.preference(screenModel.preferences).collectAsState()
            SliderItem(
                label = stringResource(filter.title),
                value = value,
                valueText = value.toString(),
                onChange = {
                    filter.preference(screenModel.preferences).set(it)
                    MPVLib.setPropertyInt(filter.mpvProperty, it)
                },
                max = 100,
                min = -100,
            )
        }
    }
}

enum class VideoFilters(
    val title: StringResource,
    val preference: (PlayerPreferences) -> Preference<Int>,
    val mpvProperty: String,
) {
    BRIGHTNESS(
        MR.strings.player_filters_brightness,
        { it.brightnessFilter() },
        "brightness",
    ),
    SATURATION(
        MR.strings.player_filters_saturation,
        { it.saturationFilter() },
        "saturation",
    ),
    CONTRAST(
        MR.strings.player_filters_contrast,
        { it.contrastFilter() },
        "contrast",
    ),
    GAMMA(
        MR.strings.player_filters_gamma,
        { it.gammaFilter() },
        "gamma",
    ),
    HUE(
        MR.strings.player_filters_hue,
        { it.hueFilter() },
        "hue",
    ),
}
