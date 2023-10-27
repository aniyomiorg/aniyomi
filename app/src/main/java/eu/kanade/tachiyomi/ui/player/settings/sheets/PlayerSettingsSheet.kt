package eu.kanade.tachiyomi.ui.player.settings.sheets

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import eu.kanade.tachiyomi.ui.player.viewer.HwDecState
import eu.kanade.tachiyomi.ui.player.viewer.PlayerStatsPage
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
    var decoder by remember { mutableStateOf(screenModel.preferences.hwDec().get()) }

    // TODO: Shift to MPV-Lib
    val togglePlayerStatsPage: (Int) -> Unit = { page ->
        if ((statisticsPage == 0) xor (page == 0)) {
            MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
        }
        if (page != 0) {
            MPVLib.command(arrayOf("script-binding", "stats/display-page-$page"))
        }
        statisticsPage = page
        screenModel.preferences.playerStatisticsPage().set(page)
    }

    val togglePlayerDecoder: (HwDecState) -> Unit = { hwDecState ->
        val hwDec = hwDecState.mpvValue
        MPVLib.setOptionString("hwdec", hwDec)
        decoder = hwDec
        screenModel.preferences.hwDec().set(hwDec)
    }

    AdaptiveSheet(
        hideSystemBars = true,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(id = R.string.settings_dialog_header),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp,
            )

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

            // TODO: (Merge_Change) below two Columns to be switched to using 'SettingsChipRow'
            //  from 'SettingsItems.kt'

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.padding.medium),
            ) {
                Text(
                    text = stringResource(id = R.string.player_hwdec_mode),
                    style = MaterialTheme.typography.titleSmall,
                )

                Row(
                    modifier = Modifier.padding(vertical = MaterialTheme.padding.tiny),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    HwDecState.values().forEach {
                        if (!HwDecState.isHwSupported && it.title == "HW+") return@forEach
                        FilterChip(
                            selected = decoder == it.mpvValue,
                            onClick = { togglePlayerDecoder(it) },
                            label = { Text(it.title) },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.padding.medium),
            ) {
                Text(
                    text = stringResource(id = R.string.toggle_player_statistics_page),
                    style = MaterialTheme.typography.titleSmall,
                )

                Row(
                    modifier = Modifier.padding(vertical = MaterialTheme.padding.tiny),
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
