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

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.player.controls.LocalPlayerButtonsClickEvent
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding

@Composable
fun ControlsButton(
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    title: String? = null,
    color: Color = Color.White,
    horizontalSpacing: Dp = MaterialTheme.padding.medium,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickEvent = LocalPlayerButtonsClickEvent.current
    val iconColor = if (enabled) color else color.copy(alpha = DISABLED_ALPHA)

    Box(
        modifier = modifier
            .combinedClickable(
                enabled = enabled,
                onClick = {
                    clickEvent()
                    onClick()
                },
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .clip(CircleShape)
            .indication(
                interactionSource,
                ripple(),
            )
            .padding(
                vertical = MaterialTheme.padding.medium,
                horizontal = horizontalSpacing,
            ),
    ) {
        Icon(
            icon,
            title,
            tint = iconColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun ControlsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    color: Color = Color.White,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val clickEvent = LocalPlayerButtonsClickEvent.current
    Box(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    clickEvent()
                    onClick()
                },
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null,

            )
            .clip(CircleShape)
            .indication(
                interactionSource,
                ripple(),
            )
            .padding(MaterialTheme.padding.medium),
    ) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun FilledControlsButton(
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickEvent = LocalPlayerButtonsClickEvent.current

    Box(
        modifier = modifier.padding(end = MaterialTheme.padding.small),
    ) {
        Button(onClick = {}) {
            Text(text = text)
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .combinedClickable(
                    onClick = {
                        clickEvent()
                        onClick()
                    },
                    onLongClick = onLongClick,
                    interactionSource = interactionSource,
                    indication = null,
                ),
        )
    }
}

@Preview
@Composable
private fun PreviewControlsButton() {
    ControlsButton(
        Icons.Default.CatchingPokemon,
        onClick = {},
    )
}
