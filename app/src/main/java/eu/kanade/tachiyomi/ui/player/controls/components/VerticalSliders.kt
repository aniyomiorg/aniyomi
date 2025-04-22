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

package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.padding
import java.text.NumberFormat
import kotlin.math.roundToInt

private fun percentage(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    return ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
}

private fun percentage(value: Int, range: ClosedRange<Int>): Float {
    return ((value - range.start).toFloat() / (range.endInclusive - range.start)).coerceIn(0f, 1f)
}

@Composable
fun VerticalSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    overflowValue: Float? = null,
    overflowRange: ClosedFloatingPointRange<Float>? = null,
) {
    require(range.contains(value)) { "Value must be within the provided range" }
    VerticalSliderInternal(
        percentage = percentage(value, range),
        overflowPercentage = overflowValue?.let { ov -> overflowRange?.let { or -> percentage(ov, or) } },
        modifier = modifier,
    )
}

@Composable
fun VerticalSlider(
    value: Int,
    range: ClosedRange<Int>,
    modifier: Modifier = Modifier,
    overflowValue: Int? = null,
    overflowRange: ClosedRange<Int>? = null,
) {
    require(range.contains(value)) { "Value must be within the provided range" }
    VerticalSliderInternal(
        percentage = percentage(value, range),
        overflowPercentage = overflowValue?.let { ov -> overflowRange?.let { or -> percentage(ov, or) } },
        modifier = modifier,
    )
}

@Composable
private fun VerticalSliderInternal(
    percentage: Float,
    modifier: Modifier = Modifier,
    overflowPercentage: Float? = null,
) {
    Box(
        modifier = modifier.height(120.dp).aspectRatio(0.45f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(fraction = 0.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val targetHeight by animateFloatAsState(percentage, label = "vsliderheight")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(targetHeight)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
            if (overflowPercentage != null) {
                val overflowHeight by animateFloatAsState(
                    targetValue = overflowPercentage,
                    label = "vslideroverflowheight",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(overflowHeight)
                        .background(MaterialTheme.colorScheme.errorContainer),
                )
            }
        }
    }
}

@Composable
fun BrightnessSlider(
    brightness: Float,
    positiveRange: ClosedFloatingPointRange<Float>,
    negativeRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        Text(
            text = (brightness * 100).toInt().toString(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        VerticalSlider(
            value = brightness.coerceIn(0f, 1f),
            range = positiveRange,
            overflowRange = negativeRange,
            overflowValue = (-brightness).coerceIn(0f..0.75f),
        )
        Icon(
            imageVector = when (percentage(brightness, positiveRange)) {
                in -1f..0f -> Icons.Default.ModeNight
                in 0f..0.3f -> Icons.Default.BrightnessLow
                in 0.3f..0.6f -> Icons.Default.BrightnessMedium
                in 0.6f..1f -> Icons.Default.BrightnessHigh
                else -> Icons.Default.BrightnessMedium
            },
            contentDescription = null,
        )
    }
}

@Composable
fun VolumeSlider(
    volume: Int,
    mpvVolume: Int,
    range: ClosedRange<Int>,
    boostRange: ClosedRange<Int>?,
    modifier: Modifier = Modifier,
    displayAsPercentage: Boolean = false,
) {
    val percentage = (percentage(volume, range) * 100).roundToInt()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        val boostVolume = mpvVolume - 100
        val (deviceVolumeString, boostVolumeString) = getVolumeSliderText(
            volume,
            boostVolume,
            percentage,
            displayAsPercentage,
        )
        Text(
            text = deviceVolumeString,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Box {
            VerticalSlider(
                value = if (displayAsPercentage) percentage else volume,
                range = if (displayAsPercentage) 0..100 else range,
                overflowValue = boostVolume,
                overflowRange = boostRange,
            )

            Text(
                text = boostVolumeString,
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(1f, 1f),
                        blurRadius = 4f,
                    ),
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        Icon(
            imageVector = when (percentage) {
                0 -> Icons.AutoMirrored.Default.VolumeOff
                in 0..30 -> Icons.AutoMirrored.Default.VolumeMute
                in 30..60 -> Icons.AutoMirrored.Default.VolumeDown
                in 60..100 -> Icons.AutoMirrored.Default.VolumeUp
                else -> Icons.AutoMirrored.Default.VolumeOff
            },
            contentDescription = null,
        )
    }
}

val getVolumeSliderText: @Composable (Int, Int, Int, Boolean) -> Pair<String, String> =
    { volume, boostVolume, percentageInt, displayAsPercentage ->
        val percentFormat = remember { NumberFormat.getPercentInstance() }
        val integerFormat = remember { NumberFormat.getIntegerInstance() }
        val percentage = percentageInt / 100f

        val deviceVolumeString = if (displayAsPercentage) {
            percentFormat.format(percentage)
        } else {
            integerFormat.format(volume)
        }

        val boostVolumeString = when (boostVolume) {
            0 -> ""
            in 0..1000 -> "+${integerFormat.format(boostVolume)}"
            in -100..-1 -> "-${integerFormat.format(-boostVolume)}"
            else -> integerFormat.format(boostVolume)
        }

        Pair(deviceVolumeString, boostVolumeString)
    }
