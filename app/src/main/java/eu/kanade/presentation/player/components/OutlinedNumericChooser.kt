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

package eu.kanade.presentation.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun OutlinedNumericChooser(
    value: Int,
    onChange: (Int) -> Unit,
    max: Int,
    step: Int,
    modifier: Modifier = Modifier,
    min: Int = 0,
    suffix: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
) {
    assert(max > min) { "min can't be larger than max ($min > $max)" }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        RepeatingIconButton(onClick = { onChange(value - step) }) {
            Icon(Icons.Filled.RemoveCircle, null)
        }
        var valueString by remember { mutableStateOf("$value") }
        LaunchedEffect(value) {
            if (valueString.isBlank() && value == 0) return@LaunchedEffect
            valueString = value.toString()
        }
        OutlinedTextField(
            label = label,
            value = valueString,
            onValueChange = { newValue ->
                if (newValue.isBlank()) {
                    valueString = newValue
                    onChange(0)
                }
                runCatching {
                    val intValue = if (newValue.trimStart() == "-") -0 else newValue.toInt()
                    onChange(intValue)
                    valueString = newValue
                }
            },
            isError = value > max || value < min,
            supportingText = {
                if (value > max) Text(stringResource(AYMR.strings.numeric_chooser_value_too_big))
                if (value < min) Text(stringResource(AYMR.strings.numeric_chooser_value_too_small))
            },
            suffix = suffix,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        RepeatingIconButton(onClick = { onChange(value + step) }) {
            Icon(Icons.Filled.AddCircle, null)
        }
    }
}

@Composable
fun OutlinedNumericChooser(
    value: Float,
    onChange: (Float) -> Unit,
    max: Float,
    step: Float,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    suffix: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
) {
    assert(max > min) { "min can't be larger than max ($min > $max)" }
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        RepeatingIconButton(onClick = { onChange(value - step) }) {
            Icon(Icons.Filled.RemoveCircle, null)
        }
        var valueString by remember { mutableStateOf("$value") }
        LaunchedEffect(value) {
            if (valueString.isBlank() && value == 0f) return@LaunchedEffect
            valueString = value.toString().dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
        }
        OutlinedTextField(
            value = valueString,
            label = label,
            onValueChange = { newValue ->
                if (newValue.isBlank()) {
                    valueString = newValue
                    onChange(0f)
                }
                runCatching {
                    if (newValue.startsWith('.')) return@runCatching
                    val floatValue = if (newValue.trimStart() == "-") -0f else newValue.toFloat()
                    onChange(floatValue)
                    valueString = newValue
                }
            },
            isError = value > max || value < min,
            supportingText = {
                if (value > max) Text(stringResource(AYMR.strings.numeric_chooser_value_too_big))
                if (value < min) Text(stringResource(AYMR.strings.numeric_chooser_value_too_small))
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            suffix = suffix,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        RepeatingIconButton(onClick = { onChange(value + step) }) {
            Icon(Icons.Filled.AddCircle, null)
        }
    }
}
