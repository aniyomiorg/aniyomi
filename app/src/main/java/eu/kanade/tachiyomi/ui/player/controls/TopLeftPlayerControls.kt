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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import tachiyomi.presentation.core.components.material.padding

@Composable
fun TopLeftPlayerControls(
    animeTitle: String,
    mediaTitle: String,
    onTitleClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.mediumSmall),
    ) {
        ControlsButton(
            icon = Icons.AutoMirrored.Default.ArrowBack,
            onClick = onBackClick,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(-MaterialTheme.padding.extraSmall),
            modifier = Modifier
                .clickable(onClick = onTitleClick),
        ) {
            Text(
                animeTitle,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                mediaTitle,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Preview
@Composable
fun TopLeftPlayerControlsPreview() {
    TopLeftPlayerControls(
        animeTitle = "Bleach",
        mediaTitle = "Episode 1 - A Shinigami is born",
        onTitleClick = {},
        onBackClick = {},
    )
}
