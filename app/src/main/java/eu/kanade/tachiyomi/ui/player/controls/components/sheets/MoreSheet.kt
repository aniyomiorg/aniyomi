package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.player.components.PlayerSheet
import eu.kanade.tachiyomi.ui.player.settings.AdvancedPlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.AudioChannels
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import `is`.xyz.mpv.MPVLib
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.MPVKtSpacing
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun MoreSheet(
    remainingTime: Int,
    onStartTimer: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    onEnterFiltersPanel: () -> Unit,
    // TODO(customButtons)
    // customButtons: ImmutableList<CustomButtonEntity>,
    modifier: Modifier = Modifier,
) {
    val advancedPreferences = remember { Injekt.get<AdvancedPlayerPreferences>() }
    val audioPreferences = remember { Injekt.get<AudioPreferences>() }
    val statisticsPage by advancedPreferences.playerStatisticsPage().collectAsState()

    PlayerSheet(
        onDismissRequest,
        modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.MPVKtSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.smaller),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.player_sheets_more_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.extraSmall),
                ) {
                    var isSleepTimerDialogShown by remember { mutableStateOf(false) }
                    IconToggleButton(
                        checked = remainingTime > 0,
                        onCheckedChange = { isSleepTimerDialogShown = true },
                    ) {
                        Icon(Icons.Outlined.Timer, null)
                        if (isSleepTimerDialogShown) {
                            TimePickerDialog(
                                remainingTime = remainingTime,
                                onDismissRequest = { isSleepTimerDialogShown = false },
                                onTimeSelect = onStartTimer
                            )
                        }
                    }
                    TextButton(onClick = onEnterFiltersPanel) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.extraSmall),
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                            Text(text = stringResource(MR.strings.player_sheets_filters_title))
                        }
                    }
                }
            }
            Text(stringResource(MR.strings.player_sheets_stats_page_title))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.smaller),
            ) {
                items(6) { page ->
                    FilterChip(
                        label = {
                            Text(
                                stringResource(
                                    if (page == 0) MR.strings.player_sheets_tracks_off else MR.strings.player_sheets_stats_page_chip,
                                    page,
                                ),
                            )
                        },
                        onClick = {
                            if ((page == 0) xor (statisticsPage == 0)) {
                                MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
                            }
                            if (page != 0) {
                                MPVLib.command(arrayOf("script-binding", "stats/display-page-$page"))
                            }
                            advancedPreferences.playerStatisticsPage().set(page)
                        },
                        selected = statisticsPage == page,
                    )
                }
            }

            // TODO(customButtons)
            //if (customButtons.isNotEmpty()) {
            //    Text(text = stringResource(MR.strings.player_sheets_custom_buttons_title))
            //    FlowRow(
            //        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            //        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
            //        maxItemsInEachRow = Int.MAX_VALUE,
            //    ) {
            //        customButtons.forEach { button ->
            //
            //            val inputChipInteractionSource = remember { MutableInteractionSource() }
            //
            //            Box {
            //                FilterChip(
            //                    onClick = {},
            //                    label = { Text(text = button.title) },
            //                    selected = false,
            //                    interactionSource = inputChipInteractionSource,
            //                )
            //                Box(
            //                    modifier = Modifier
            //                        .matchParentSize()
            //                        .combinedClickable(
            //                            onClick = button::execute,
            //                            onLongClick = button::executeLongClick,
            //                            interactionSource = inputChipInteractionSource,
            //                            indication = null,
            //                        )
            //                )
            //            }
            //        }
            //    }
            //}
            Text(text = stringResource(MR.strings.pref_audio_channels))
            val audioChannels by audioPreferences.audioChannels().collectAsState()
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.smaller),
            ) {
                items(AudioChannels.entries) {
                    FilterChip(
                        selected = audioChannels == it,
                        onClick = {
                            audioPreferences.audioChannels().set(it)
                            if (it == AudioChannels.ReverseStereo) {
                                MPVLib.setPropertyString(AudioChannels.AutoSafe.property, AudioChannels.AutoSafe.value)
                            } else {
                                MPVLib.setPropertyString(AudioChannels.ReverseStereo.property, "")
                            }
                            MPVLib.setPropertyString(it.property, it.value)
                        },
                        label = { Text(text = stringResource(it.titleRes)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    remainingTime: Int = 0,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.padding(MaterialTheme.MPVKtSpacing.medium),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .width(IntrinsicSize.Max)
                    .animateContentSize()
                    .padding(MaterialTheme.MPVKtSpacing.medium),
            ) {
                var currentLayoutType by rememberSaveable { mutableIntStateOf(0) }
                Text(
                    text = stringResource(
                        if (currentLayoutType == 1) {
                            MR.strings.timer_picker_pick_time
                        } else {
                            MR.strings.timer_picker_enter_timer
                        },
                    ),
                )

                val state = rememberTimePickerState(
                    remainingTime / 3600,
                    (remainingTime % 3600) / 60,
                    is24Hour = true,
                )
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    if (currentLayoutType == 1) {
                        TimePicker(state = state)
                    } else {
                        TimeInput(state = state)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(onClick = { currentLayoutType = if (currentLayoutType == 0) 1 else 0 }) {
                        Icon(
                            imageVector = if (currentLayoutType == 0) Icons.Outlined.Schedule else Icons.Default.KeyboardAlt,
                            contentDescription = null,
                        )
                    }
                    Row {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(MR.strings.action_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onTimeSelect(state.hour * 3600 + state.minute * 60)
                                onDismissRequest()
                            },
                        ) {
                            Text(stringResource(MR.strings.action_ok))
                        }
                    }
                }
            }
        }
    }
}
