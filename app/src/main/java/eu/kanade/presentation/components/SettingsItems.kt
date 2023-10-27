package eu.kanade.presentation.components

import android.view.MotionEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tachiyomi.domain.entries.TriStateFilter
import tachiyomi.presentation.core.components.SettingsItemsPaddings

@Composable
fun TriStateItem(
    label: String,
    state: TriStateFilter,
    enabled: Boolean = true,
    onClick: ((TriStateFilter) -> Unit)?,
) {
    Row(
        modifier = Modifier
            .clickable(
                enabled = enabled && onClick != null,
                onClick = {
                    when (state) {
                        TriStateFilter.DISABLED -> onClick?.invoke(TriStateFilter.ENABLED_IS)
                        TriStateFilter.ENABLED_IS -> onClick?.invoke(TriStateFilter.ENABLED_NOT)
                        TriStateFilter.ENABLED_NOT -> onClick?.invoke(TriStateFilter.DISABLED)
                    }
                },
            )
            .fillMaxWidth()
            .padding(horizontal = SettingsItemsPaddings.Horizontal, vertical = SettingsItemsPaddings.Vertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        val stateAlpha = if (enabled && onClick != null) 1f else ContentAlpha.disabled

        Icon(
            imageVector = when (state) {
                TriStateFilter.DISABLED -> Icons.Rounded.CheckBoxOutlineBlank
                TriStateFilter.ENABLED_IS -> Icons.Rounded.CheckBox
                TriStateFilter.ENABLED_NOT -> Icons.Rounded.DisabledByDefault
            },
            contentDescription = null,
            tint = if (!enabled || state == TriStateFilter.DISABLED) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = stateAlpha)
            } else {
                when (onClick) {
                    null -> MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
                    else -> MaterialTheme.colorScheme.primary
                }
            },
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = stateAlpha),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun SelectItem(
    label: String,
    options: Array<out Any?>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(horizontal = SettingsItemsPaddings.Horizontal, vertical = SettingsItemsPaddings.Vertical),
            label = { Text(text = label) },
            value = options[selectedIndex].toString(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )

        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text.toString()) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun RepeatingIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    maxDelayMillis: Long = 750,
    minDelayMillis: Long = 5,
    delayDecayFactor: Float = .25f,
    content: @Composable () -> Unit,
) {
    val currentClickListener by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }

    IconButton(
        modifier = modifier.pointerInteropFilter {
            pressed = when (it.action) {
                MotionEvent.ACTION_DOWN -> true

                else -> false
            }

            true
        },
        onClick = {},
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )

    LaunchedEffect(pressed, enabled) {
        var currentDelayMillis = maxDelayMillis

        while (enabled && pressed) {
            currentClickListener()
            delay(currentDelayMillis)
            currentDelayMillis =
                (currentDelayMillis - (currentDelayMillis * delayDecayFactor))
                    .toLong().coerceAtLeast(minDelayMillis)
        }
    }
}

@Composable
fun OutlinedNumericChooser(
    label: String,
    placeholder: String,
    suffix: String,
    value: Int,
    step: Int,
    min: Int? = null,
    onValueChanged: (Int) -> Unit,
) {
    var currentValue = value

    val updateValue: (Boolean) -> Unit = {
        currentValue += if (it) step else -step

        if (min != null) currentValue = if (currentValue < min) min else currentValue

        onValueChanged(currentValue)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RepeatingIconButton(
            onClick = { updateValue(false) },
        ) { Icon(imageVector = Icons.Outlined.RemoveCircle, contentDescription = null) }

        OutlinedTextField(
            value = "%d".format(currentValue),
            modifier = Modifier.widthIn(min = 140.dp),

            onValueChange = {
                // Don't allow multiple decimal points, non-numeric characters, or leading zeros
                currentValue = it.trim().replace(Regex("[^-\\d.]"), "").toIntOrNull()
                    ?: currentValue
                onValueChanged(currentValue)
            },

            label = { Text(text = label) },
            placeholder = { Text(text = placeholder) },
            suffix = { Text(text = suffix) },

            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        RepeatingIconButton(
            onClick = { updateValue(true) },
        ) { Icon(imageVector = Icons.Outlined.AddCircle, contentDescription = null) }
    }
}
