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

package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import eu.kanade.tachiyomi.ui.player.controls.components.FilledControlsButton
import eu.kanade.tachiyomi.ui.player.execute
import eu.kanade.tachiyomi.ui.player.executeLongPress
import tachiyomi.domain.custombuttons.model.CustomButton

@Composable
fun BottomRightPlayerControls(
    customButton: CustomButton?,
    customButtonTitle: String,
    aniskipButton: String?,
    onPressAniSkipButton: () -> Unit,
    isPipAvailable: Boolean,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        if (aniskipButton != null) {
            FilledControlsButton(
                text = aniskipButton,
                onClick = onPressAniSkipButton,
                onLongClick = {},
            )
        } else if (customButton != null) {
            FilledControlsButton(
                text = customButtonTitle,
                onClick = customButton::execute,
                onLongClick = customButton::executeLongPress,
            )
        }

        if (isPipAvailable) {
            ControlsButton(
                Icons.Default.PictureInPictureAlt,
                onClick = onPipClick,
            )
        }

        ControlsButton(
            Icons.Default.AspectRatio,
            onClick = onAspectClick,
        )
    }
}
