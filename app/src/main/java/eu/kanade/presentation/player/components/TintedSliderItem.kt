package eu.kanade.presentation.player.components

import androidx.annotation.IntRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import tachiyomi.presentation.core.components.material.MPVKtSpacing
import kotlin.math.max
import kotlin.math.min

@Composable
fun TintedSliderItem(
    label: String,
    value: Int,
    valueText: String,
    onChange: (Int) -> Unit,
    max: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    min: Int = 0,
    icon: @Composable () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.MPVKtSpacing.medium,
                vertical = MaterialTheme.MPVKtSpacing.smaller,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.large),
    ) {
        icon()
        Column(modifier = Modifier.weight(0.5f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(valueText)
        }

        TintedSlider(
            value = value.toFloat(),
            onValueChange = {
                val newValue = it.toInt()
                if (newValue != value) {
                    onChange(newValue)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            modifier = Modifier.weight(1.5f),
            valueRange = min.toFloat()..max.toFloat(),
            steps = max - min,
            tint = tint,
        )
    }
}

@Composable
fun TintedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.primaryContainer,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = generateSliderColors(tint),
        interactionSource = interactionSource,
    )
}

@Preview
@Composable
private fun PreviewTintedSliderRed() {
    TintedSlider(
        0.5f,
        {},
        tint = Color.Red,
    )
}

@Preview
@Composable
private fun PreviewTintedSliderItemRed() {
    TintedSliderItem(
        "slideritem red",
        1,
        "1",
        {},
        20,
        tint = Color.Red,
    )
}

fun generateSliderColors(baseColor: Color): SliderColors {
    // Utility function to darken a color
    fun darken(color: Color, factor: Float): Color {
        val red = max((color.red * factor), 0f)
        val green = max((color.green * factor), 0f)
        val blue = max((color.blue * factor), 0f)
        return Color(red, green, blue, color.alpha)
    }

    // Utility function to lighten a color
    fun lighten(color: Color, factor: Float): Color {
        val red = min((color.red + (1 - color.red) * factor), 255f)
        val green = min((color.green + (1 - color.green) * factor), 255f)
        val blue = min((color.blue + (1 - color.blue) * factor), 255f)
        return Color(red, green, blue, color.alpha)
    }

    return SliderColors(
        thumbColor = baseColor,
        activeTrackColor = lighten(baseColor, 0.2f),
        activeTickColor = lighten(baseColor, 0.4f),
        inactiveTrackColor = darken(baseColor, 0.2f),
        inactiveTickColor = darken(baseColor, 0.4f),
        disabledThumbColor = baseColor.copy(alpha = 0.5f),
        disabledActiveTrackColor = lighten(baseColor, 0.2f).copy(alpha = 0.5f),
        disabledActiveTickColor = lighten(baseColor, 0.4f).copy(alpha = 0.5f),
        disabledInactiveTrackColor = darken(baseColor, 0.2f).copy(alpha = 0.5f),
        disabledInactiveTickColor = darken(baseColor, 0.4f).copy(alpha = 0.5f),
    )
}
