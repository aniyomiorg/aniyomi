package eu.kanade.tachiyomi.ui.player.settings.sheets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import `is`.xyz.mpv.MPVLib
import tachiyomi.presentation.core.components.material.padding

@Composable
fun PlayerSettingsSheet(
    screenModel: PlayerSettingsScreenModel,
    onDismissRequest: () -> Unit,
) {
    val verticalGesture by remember { mutableStateOf(screenModel.preferences.gestureVolumeBrightness()) }
    val horizontalGesture by remember { mutableStateOf(screenModel.preferences.gestureHorizontalSeek()) }
    var statisticsPage by remember { mutableStateOf(screenModel.preferences.playerStatisticsPage().get()) }

    // TODO: Shift to MPV-Lib
    val togglePlayerStatsPage: (Int) -> Unit = { page ->
        MPVLib.command(arrayOf("script-binding", "stats/display-page-$page"))
        if (statisticsPage == 0 || page == 0) {
            MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
        }
        statisticsPage = page
        screenModel.preferences.playerStatisticsPage().set(page)
    }

    AdaptiveSheet(
        hideSystemBars = true,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.padding(vertical = MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            screenModel.ToggleableRow(
                textRes = R.string.enable_volume_brightness_gestures,
                isChecked = verticalGesture.collectAsState().value,
                onClick = { screenModel.togglePreference { verticalGesture } },
            )

            screenModel.ToggleableRow(
                textRes = R.string.enable_horizontal_seek_gesture,
                isChecked = horizontalGesture.collectAsState().value,
                onClick = { screenModel.togglePreference { horizontalGesture } },
            )

            // TODO: (Merge_Change) below Row to be switched to 'SettingsChipRow'
            //  from 'SettingsItems.kt'

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.toggle_player_statistics_page),
                    style = MaterialTheme.typography.titleSmall,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    PlayerStatsPage.values().forEach {
                        FilterChip(
                            selected = statisticsPage == it.page,
                            onClick = { togglePlayerStatsPage(it.page) },
                            label = { Text(stringResource(it.textRes)) },
                        )
                    }
                }
            }
        }
    }
}

private enum class PlayerStatsPage(val page: Int, @StringRes val textRes: Int) {
    OFF(0, R.string.off),
    PAGE1(1, R.string.player_statistics_page_1),
    PAGE2(2, R.string.player_statistics_page_2),
    PAGE3(3, R.string.player_statistics_page_3),
    ;
}
