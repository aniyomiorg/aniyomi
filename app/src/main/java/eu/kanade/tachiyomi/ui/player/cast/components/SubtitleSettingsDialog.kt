package eu.kanade.tachiyomi.ui.player.cast.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LineStyle
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SubtitleSettingsDialog(
    onDismissRequest: () -> Unit,
    initialSettings: SubtitleSettings,
    onSettingsChanged: (SubtitleSettings) -> Unit,
) {
    var fontSize by remember { mutableFloatStateOf(initialSettings.fontSize.value) }
    var textColor by remember { mutableStateOf(initialSettings.textColor) }
    var backgroundColor by remember { mutableStateOf(initialSettings.backgroundColor) }
    var shadowRadius by remember { mutableFloatStateOf(initialSettings.shadowRadius.value) }
    var fontFamily by remember { mutableStateOf(initialSettings.fontFamily) }
    var borderStyle by remember { mutableStateOf(initialSettings.borderStyle) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(TLMR.strings.cast_subtitle_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingSection(
                    title = stringResource(TLMR.strings.cast_subtitle_font_size),
                    icon = Icons.Default.FormatSize,
                ) {
                    Column {
                        Text(
                            text = "${fontSize.toInt()}sp",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Slider(
                            value = fontSize,
                            onValueChange = { fontSize = it },
                            valueRange = 12f..40f,
                            steps = 27,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                    }
                }
                SettingSection(
                    title = stringResource(TLMR.strings.cast_subtitle_text_color),
                    icon = Icons.Default.Palette,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 4,
                    ) {
                        SubtitleColorOptions.forEach { color ->
                            ColorButton(
                                color = color,
                                isSelected = textColor == color,
                                onClick = { textColor = color },
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }

                // Background Color Section
                SettingSection(
                    title = stringResource(TLMR.strings.cast_subtitle_background),
                    icon = Icons.Default.FormatColorFill,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 4,
                    ) {
                        BackgroundColorOptions.forEach { color ->
                            ColorButton(
                                color = color,
                                isSelected = backgroundColor == color,
                                onClick = { backgroundColor = color },
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }

                // Shadow Section
                SettingSection(
                    title = stringResource(TLMR.strings.cast_subtitle_shadow),
                    icon = Icons.Default.Opacity,
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = when {
                                    shadowRadius <= 0 -> stringResource(TLMR.strings.cast_subtitle_shadow_None)
                                    shadowRadius <= 3 -> stringResource(TLMR.strings.cast_subtitle_shadow_Light)
                                    shadowRadius <= 6 -> stringResource(TLMR.strings.cast_subtitle_shadow_Medium)
                                    else -> stringResource(TLMR.strings.cast_subtitle_shadow_Strong)
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = "${shadowRadius.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Slider(
                            value = shadowRadius,
                            onValueChange = { shadowRadius = it },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                    }
                }

                // Font Family Section
                SettingSection(
                    title = stringResource(TLMR.strings.cast_subtitle_font_family),
                    icon = Icons.Default.TextFormat,
                ) {
                    FontFamilySelector(
                        selectedFamily = fontFamily,
                        onFamilySelected = { newFamily ->
                            fontFamily = newFamily
                        },
                    )
                }

                // Border Style Section
                SettingSection(
                    title = stringResource(TLMR.strings.cast_subtitle_border_style),
                    icon = Icons.Default.LineStyle,
                ) {
                    BorderStyleSelector(
                        selectedStyle = borderStyle,
                        onStyleSelected = { borderStyle = it },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Color(0xFF1A1A1A),
                            MaterialTheme.shapes.medium,
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.medium,
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(TLMR.strings.cast_subtitle_preview),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            color = textColor,
                            fontFamily = fontFamily,
                            shadow = when {
                                shadowRadius <= 0 -> null
                                shadowRadius <= 3 -> Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    blurRadius = 0f,
                                )
                                shadowRadius <= 6 -> Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    blurRadius = 2f,
                                )
                                else -> Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    blurRadius = 3f,
                                )
                            },
                        ),
                        modifier = if (backgroundColor != Color.Transparent) {
                            Modifier
                                .background(
                                    backgroundColor,
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        } else {
                            Modifier
                        },
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        val defaultSettings = SubtitleSettings(
                            fontSize = 20.sp,
                            textColor = Color.White,
                            backgroundColor = Color.Transparent,
                            shadowRadius = 2.dp,
                            fontFamily = FontFamily.Default,
                            borderStyle = BorderStyle.NONE,
                        )
                        onSettingsChanged(defaultSettings)
                        onDismissRequest()
                    },
                ) {
                    Text(stringResource(TLMR.strings.cast_subtitle_reset))
                }
                FilledTonalButton(
                    onClick = {
                        val newSettings = SubtitleSettings(
                            fontSize = fontSize.sp,
                            textColor = textColor,
                            backgroundColor = backgroundColor,
                            shadowRadius = shadowRadius.dp,
                            fontFamily = fontFamily,
                            borderStyle = borderStyle,
                        )
                        onSettingsChanged(newSettings)
                        onDismissRequest()
                    },
                ) {
                    Text(stringResource(TLMR.strings.cast_subtitle_apply))
                }
            }
        },
    )
}

@Composable
private fun SettingSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            content()
        }
    }
}

private val SubtitleColorOptions = listOf(
    Color.White,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Magenta,
    Color.Red,
    Color(0xFFFF9800),
    Color(0xFF4CAF50),
)

private val BackgroundColorOptions = listOf(
    Color(0x03000000), // 1% opacity black
    Color(0x19000000), // 10% opacity black
    Color(0x40000000), // 25% opacity black
    Color(0x80000000), // 50% opacity black
    Color(0xFF000000), // 100% opacity black
)

private val FontFamilyOptions = listOf(
    "Default" to FontFamily.Default,
    "Monospace" to FontFamily.Monospace,
    "Serif" to FontFamily.Serif,
    "SansSerif" to FontFamily.SansSerif,
    "Cursive" to FontFamily.Cursive,
)

@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.small,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (color == Color(0x03000000)) {
                Text(
                    "T",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FontFamilySelector(
    selectedFamily: FontFamily,
    onFamilySelected: (FontFamily) -> Unit,
) {
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            FontFamilyOptions.forEach { (name, family) ->
                Surface(
                    onClick = { onFamilySelected(family) },
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selectedFamily == family) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp),
                ) {
                    Text(
                        text = name,
                        fontFamily = family,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun BorderStyleSelector(
    selectedStyle: BorderStyle,
    onStyleSelected: (BorderStyle) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        BorderStyle.values().forEach { style ->
            Surface(
                onClick = { onStyleSelected(style) },
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedStyle == style) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    },
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
            ) {
                Text(
                    text = when (style) {
                        BorderStyle.NONE -> stringResource(TLMR.strings.cast_subtitle_border_none)
                        BorderStyle.OUTLINE -> stringResource(TLMR.strings.cast_subtitle_border_outline)
                        BorderStyle.DROP_SHADOW -> stringResource(TLMR.strings.cast_subtitle_border_drop_shadow)
                        BorderStyle.RAISED -> stringResource(TLMR.strings.cast_subtitle_border_raised)
                        BorderStyle.DEPRESSED -> stringResource(TLMR.strings.cast_subtitle_border_depressed)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

enum class BorderStyle {
    NONE,
    OUTLINE,
    DROP_SHADOW,
    RAISED,
    DEPRESSED,
}

data class SubtitleSettings(
    val fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    val textColor: Color = Color.White,
    val fontFamily: FontFamily = FontFamily.Default,
    val fontWeight: FontWeight = FontWeight.Normal,
    val fontStyle: FontStyle = FontStyle.Normal,
    val shadowRadius: androidx.compose.ui.unit.Dp = 2.dp,
    val backgroundColor: Color = Color.Transparent,
    val borderStyle: BorderStyle = BorderStyle.NONE,
)
