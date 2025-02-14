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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.player.CastManager
import eu.kanade.tachiyomi.ui.player.cast.components.CastButton
import eu.kanade.tachiyomi.ui.player.controls.components.AutoPlaySwitch
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import tachiyomi.presentation.core.components.material.padding

@Composable
fun TopRightPlayerControls(
    // auto-play
    autoPlayEnabled: Boolean,
    onToggleAutoPlay: (Boolean) -> Unit,

    // subtitles
    onSubtitlesClick: () -> Unit,
    onSubtitlesLongClick: () -> Unit,

    // audio
    onAudioClick: () -> Unit,
    onAudioLongClick: () -> Unit,

    // video
    onQualityClick: () -> Unit,
    isEpisodeOnline: Boolean?,

    // more
    onMoreClick: () -> Unit,
    onMoreLongClick: () -> Unit,

    // cast
    castState: CastManager.CastState,
    onCastClick: () -> Unit,
    isCastEnabled: () -> Boolean,

    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
    ) {
        AutoPlaySwitch(
            isChecked = autoPlayEnabled,
            onToggleAutoPlay = onToggleAutoPlay,
            modifier = Modifier
                .padding(vertical = MaterialTheme.padding.medium, horizontal = MaterialTheme.padding.mediumSmall)
                .size(width = 48.dp, height = 24.dp),
        )
        if (isCastEnabled()) {
            CastButton(
                castState = castState,
                onClick = onCastClick,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.mediumSmall),
            )
        }
        ControlsButton(
            icon = Icons.Default.Subtitles,
            onClick = onSubtitlesClick,
            onLongClick = onSubtitlesLongClick,
            horizontalSpacing = MaterialTheme.padding.mediumSmall,
        )
        ControlsButton(
            icon = Icons.Default.Audiotrack,
            onClick = onAudioClick,
            onLongClick = onAudioLongClick,
            horizontalSpacing = MaterialTheme.padding.mediumSmall,
        )
        if (isEpisodeOnline == true) {
            ControlsButton(
                icon = Icons.Default.HighQuality,
                onClick = onQualityClick,
                onLongClick = onQualityClick,
                horizontalSpacing = MaterialTheme.padding.mediumSmall,
            )
        }
        ControlsButton(
            icon = Icons.Default.MoreVert,
            onClick = onMoreClick,
            onLongClick = onMoreLongClick,
            horizontalSpacing = MaterialTheme.padding.mediumSmall,
        )
    }
}
