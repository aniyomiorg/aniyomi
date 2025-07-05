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

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.BorderStyle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yubyf.truetypeparser.TTFFile
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.player.components.ExpandableCard
import eu.kanade.presentation.player.components.ExposedTextDropDownMenu
import eu.kanade.presentation.player.components.SliderItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.controls.CARDS_MAX_WIDTH
import eu.kanade.tachiyomi.ui.player.controls.panelCardsColors
import eu.kanade.tachiyomi.ui.player.settings.SubtitleJustification
import eu.kanade.tachiyomi.ui.player.settings.SubtitlePreferences
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.core.common.preference.deleteAndGet
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SubtitleSettingsTypographyCard(
    modifier: Modifier = Modifier,
) {
    val preferences = remember { Injekt.get<SubtitlePreferences>() }
    val storageManager = remember { Injekt.get<StorageManager>() }
    var isExpanded by remember { mutableStateOf(true) }

    val fontsDir = storageManager.getFontsDirectory()
    val fonts by remember { mutableStateOf(mutableListOf(preferences.subtitleFont().defaultValue())) }
    var fontsLoadingIndicator: (@Composable () -> Unit)? by remember {
        val indicator: (@Composable () -> Unit) = {
            CircularProgressIndicator(Modifier.size(32.dp))
        }
        mutableStateOf(indicator)
    }
    LaunchedEffect(Unit) {
        if (fontsDir == null) {
            fontsLoadingIndicator = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            fontsDir.listFiles()?.filter { file ->
                file.name?.lowercase()?.matches(FONT_EXTENSION_REGEX) == true
            }?.mapNotNull {
                runCatching { TTFFile.open(it.openInputStream()).families.values.first() }.getOrNull()
            }?.let {
                fonts.addAll(
                    it.distinct(),
                )
            }
            fontsLoadingIndicator = null
        }
    }

    ExpandableCard(
        isExpanded = isExpanded,
        onExpand = { isExpanded = !isExpanded },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            ) {
                Icon(Icons.Default.FormatColorText, null)
                Text(stringResource(AYMR.strings.player_sheets_sub_typography_title))
            }
        },
        modifier = modifier.widthIn(max = CARDS_MAX_WIDTH),
        colors = panelCardsColors(),
    ) {
        Column {
            var isBold by remember { mutableStateOf(MPVLib.getPropertyBoolean("sub-bold")) }
            var isItalic by remember { mutableStateOf(MPVLib.getPropertyBoolean("sub-italic")) }
            var justify by remember {
                mutableStateOf(
                    SubtitleJustification.entries.first {
                        it.value == MPVLib.getPropertyString("sub-justify")
                    },
                )
            }
            var font by remember { mutableStateOf(MPVLib.getPropertyString("sub-font")) }
            var fontSize by remember {
                mutableStateOf(MPVLib.getPropertyInt("sub-font-size"))
            }
            var borderStyle by remember {
                mutableStateOf(
                    SubtitlesBorderStyle.entries.first { it.value == MPVLib.getPropertyString("sub-border-style") },
                )
            }
            var borderSize by remember {
                mutableStateOf(
                    MPVLib.getPropertyInt("sub-border-size"),
                )
            }
            var shadowOffset by remember {
                mutableStateOf(
                    MPVLib.getPropertyInt("sub-shadow-offset"),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = MaterialTheme.padding.extraSmall, end = MaterialTheme.padding.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconToggleButton(
                    checked = isBold,
                    onCheckedChange = {
                        isBold = it
                        preferences.boldSubtitles().set(it)
                        MPVLib.setPropertyBoolean("sub-bold", it)
                    },
                ) {
                    Icon(
                        Icons.Default.FormatBold,
                        null,
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconToggleButton(
                    checked = isItalic,
                    onCheckedChange = {
                        isItalic = it
                        preferences.italicSubtitles().set(it)
                        MPVLib.setPropertyBoolean("sub-italic", it)
                    },
                ) {
                    Icon(
                        Icons.Default.FormatItalic,
                        null,
                        modifier = Modifier.size(32.dp),
                    )
                }
                SubtitleJustification.entries.minus(SubtitleJustification.Auto).forEach { justification ->
                    IconToggleButton(
                        checked = justify == justification,
                        onCheckedChange = {
                            justify = justification
                            MPVLib.setPropertyBoolean("sub-ass-justify", it)
                            if (it) {
                                preferences.subtitleJustification().set(justification)
                                MPVLib.setPropertyString("sub-justify", justification.value)
                            } else {
                                preferences.subtitleJustification().set(SubtitleJustification.Auto)
                                MPVLib.setPropertyString("sub-justify", SubtitleJustification.Auto.value)
                            }
                        },
                    ) {
                        Icon(justification.icon, null)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    resetTypography(preferences)
                    isBold = MPVLib.getPropertyBoolean("sub-bold")
                    isItalic = MPVLib.getPropertyBoolean("sub-italic")
                    justify =
                        SubtitleJustification.entries.first { it.value == MPVLib.getPropertyString("sub-justify") }
                    font = MPVLib.getPropertyString("sub-font")
                    fontSize = MPVLib.getPropertyInt("sub-font-size")
                    borderStyle =
                        SubtitlesBorderStyle.entries.first { it.value == MPVLib.getPropertyString("sub-border-style") }
                    borderSize = MPVLib.getPropertyInt("sub-border-size")
                    shadowOffset = MPVLib.getPropertyInt("sub-shadow-offset")
                }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.FormatClear, null)
                        Text(stringResource(MR.strings.action_reset))
                    }
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.outline_brand_family_24),
                    null,
                    modifier = Modifier.size(32.dp),
                )
                ExposedTextDropDownMenu(
                    selectedValue = font,
                    options = fonts.toImmutableList(),
                    label = stringResource(AYMR.strings.player_sheets_sub_typography_font),
                    onValueChangedEvent = {
                        font = it
                        preferences.subtitleFont().set(it)
                        MPVLib.setPropertyString("sub-font", it)
                    },
                    leadingIcon = fontsLoadingIndicator,
                )
            }
            SliderItem(
                label = stringResource(AYMR.strings.player_sheets_sub_typography_font_size),
                max = 100,
                min = 1,
                value = fontSize,
                valueText = fontSize.toString(),
                onChange = {
                    fontSize = it
                    preferences.subtitleFontSize().set(it)
                    MPVLib.setPropertyInt("sub-font-size", it)
                },
            ) {
                Icon(Icons.Default.FormatSize, null)
            }

            var selectingBorderStyle by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                selectingBorderStyle = !selectingBorderStyle
                            },
                        )
                        .padding(
                            horizontal = MaterialTheme.padding.medium,
                            vertical = MaterialTheme.padding.small,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.large),
                ) {
                    Icon(Icons.Default.BorderStyle, null)
                    Column {
                        Text(
                            text = stringResource(AYMR.strings.player_sheets_sub_typography_border_style),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(borderStyle.titleRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                DropdownMenu(expanded = selectingBorderStyle, onDismissRequest = { selectingBorderStyle = false }) {
                    SubtitlesBorderStyle.entries.map {
                        DropdownMenuItem(
                            text = { Text(stringResource(it.titleRes)) },
                            onClick = {
                                borderStyle = it
                                preferences.borderStyleSubtitles().set(it)
                                MPVLib.setPropertyString("sub-border-style", it.value)
                                selectingBorderStyle = false
                            },
                            trailingIcon = {
                                if (borderStyle == it) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                    )
                                }
                            },
                        )
                    }
                }
            }
            SliderItem(
                stringResource(AYMR.strings.player_sheets_sub_typography_border_size),
                value = borderSize,
                valueText = borderSize.toString(),
                onChange = {
                    borderSize = it
                    preferences.subtitleBorderSize().set(it)
                    MPVLib.setPropertyInt("sub-border-size", it)
                },
                max = 100,
                icon = { Icon(Icons.Default.BorderColor, null) },
            )
            SliderItem(
                stringResource(AYMR.strings.player_sheets_subtitles_shadow_offset),
                value = shadowOffset,
                valueText = shadowOffset.toString(),
                onChange = {
                    shadowOffset = it
                    preferences.shadowOffsetSubtitles().set(it)
                    MPVLib.setPropertyInt("sub-shadow-offset", it)
                },
                max = 100,
                icon = { Icon(painterResource(R.drawable.sharp_shadow_24), null) },
            )
        }
    }
}

private val FONT_EXTENSION_REGEX = Regex(""".*\.[ot]tf${'$'}""")

fun resetTypography(preferences: SubtitlePreferences) {
    MPVLib.setPropertyBoolean("sub-bold", preferences.boldSubtitles().deleteAndGet())
    MPVLib.setPropertyBoolean("sub-italic", preferences.italicSubtitles().deleteAndGet())
    MPVLib.setPropertyBoolean("sub-ass-justify", preferences.overrideSubsASS().deleteAndGet())
    MPVLib.setPropertyString("sub-justify", preferences.subtitleJustification().deleteAndGet().value)
    MPVLib.setPropertyString("sub-font", preferences.subtitleFont().deleteAndGet())
    MPVLib.setPropertyInt("sub-font-size", preferences.subtitleFontSize().deleteAndGet())
    MPVLib.setPropertyInt("sub-border-size", preferences.subtitleBorderSize().deleteAndGet())
    MPVLib.setPropertyInt("sub-shadow-offset", preferences.shadowOffsetSubtitles().deleteAndGet())
    MPVLib.setPropertyString("sub-border-style", preferences.borderStyleSubtitles().deleteAndGet().value)
}

enum class SubtitlesBorderStyle(
    val value: String,
    val titleRes: StringResource,
) {
    OutlineAndShadow("outline-and-shadow", AYMR.strings.player_sheets_subtitles_border_style_outline_and_shadow),
    OpaqueBox("opaque-box", AYMR.strings.player_sheets_subtitles_border_style_opaque_box),
    BackgroundBox("background-box", AYMR.strings.player_sheets_subtitles_border_style_background_box),
}
