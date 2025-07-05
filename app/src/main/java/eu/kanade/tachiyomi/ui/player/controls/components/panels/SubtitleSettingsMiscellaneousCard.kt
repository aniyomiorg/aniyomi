/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player.controls.components.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignVerticalCenter
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.player.components.ExpandableCard
import eu.kanade.presentation.player.components.SliderItem
import eu.kanade.presentation.player.components.SwitchPreference
import eu.kanade.tachiyomi.ui.player.controls.CARDS_MAX_WIDTH
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.toFixed
import eu.kanade.tachiyomi.ui.player.controls.panelCardsColors
import eu.kanade.tachiyomi.ui.player.settings.SubtitlePreferences
import `is`.xyz.mpv.MPVLib
import tachiyomi.core.common.preference.deleteAndGet
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun SubtitlesMiscellaneousCard(modifier: Modifier = Modifier) {
    val preferences = remember { Injekt.get<SubtitlePreferences>() }
    var isExpanded by remember { mutableStateOf(true) }
    ExpandableCard(
        isExpanded,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)) {
                Icon(Icons.Default.Tune, null)
                Text(stringResource(AYMR.strings.player_sheets_sub_misc_title))
            }
        },
        onExpand = { isExpanded = !isExpanded },
        modifier.widthIn(max = CARDS_MAX_WIDTH),
        colors = panelCardsColors(),
    ) {
        Column {
            var overrideAssSubs by remember {
                mutableStateOf(MPVLib.getPropertyString("sub-ass-override").also { println(it) } == "force")
            }
            SwitchPreference(
                overrideAssSubs,
                onValueChange = {
                    overrideAssSubs = it
                    preferences.overrideSubsASS().set(it)
                    MPVLib.setPropertyString("sub-ass-override", if (it) "force" else "scale")
                },
                content = { Text(stringResource(AYMR.strings.player_sheets_sub_override_ass)) },
                modifier = Modifier
                    .fillMaxWidth(),
            )
            var subScale by remember {
                mutableStateOf(MPVLib.getPropertyDouble("sub-scale").toFloat())
            }
            var subPos by remember {
                mutableStateOf(MPVLib.getPropertyInt("sub-pos"))
            }
            SliderItem(
                label = stringResource(AYMR.strings.player_sheets_sub_scale),
                value = subScale,
                valueText = subScale.toFixed(2).toString(),
                onChange = {
                    subScale = it
                    preferences.subtitleFontScale().set(it)
                    MPVLib.setPropertyDouble("sub-scale", it.toDouble())
                },
                max = 5f,
                icon = {
                    Icon(
                        Icons.Default.FormatSize,
                        null,
                    )
                },
            )
            SliderItem(
                label = stringResource(AYMR.strings.player_sheets_sub_position),
                value = subPos,
                valueText = subPos.toString(),
                onChange = {
                    subPos = it
                    preferences.subtitlePos().set(it)
                    MPVLib.setPropertyInt("sub-pos", it)
                },
                max = 150,
                icon = {
                    Icon(
                        Icons.Default.AlignVerticalCenter,
                        null,
                    )
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = MaterialTheme.padding.medium, bottom = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        preferences.subtitlePos().deleteAndGet().let {
                            subPos = it
                            MPVLib.setPropertyInt("sub-pos", it)
                        }
                        preferences.subtitleFontScale().deleteAndGet().let {
                            subScale = it
                            MPVLib.setPropertyDouble("sub-scale", it.toDouble())
                        }
                        preferences.overrideSubsASS().deleteAndGet().let { overrideAssSubs = it }
                        MPVLib.setPropertyString("sub-ass-override", "scale") // mpv's default is 'scale'
                    },
                ) {
                    Row {
                        Icon(Icons.Default.EditOff, null)
                        Text(stringResource(MR.strings.action_reset))
                    }
                }
            }
        }
    }
}
