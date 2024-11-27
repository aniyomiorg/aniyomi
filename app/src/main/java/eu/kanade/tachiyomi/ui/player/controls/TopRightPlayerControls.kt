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

import android.content.res.Resources
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
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
            modifier = Modifier.padding(
                horizontal = MaterialTheme.padding.mediumSmall,
            ),
        )
        ControlsButton(
            Icons.Default.Subtitles,
            onClick = onSubtitlesClick,
            onLongClick = onSubtitlesLongClick,
            horizontalSpacing = MaterialTheme.padding.mediumSmall,
        )
        ControlsButton(
            Icons.Default.Audiotrack,
            onClick = onAudioClick,
            onLongClick = onAudioLongClick,
            horizontalSpacing = MaterialTheme.padding.mediumSmall,
        )
        if (isEpisodeOnline == true) {
            ControlsButton(
                Icons.Default.HighQuality,
                onClick = onQualityClick,
                onLongClick = onQualityClick,
                horizontalSpacing = MaterialTheme.padding.mediumSmall,
            )
        }
        ControlsButton(
            Icons.Default.MoreVert,
            onClick = onMoreClick,
            onLongClick = onMoreLongClick,
            horizontalSpacing = MaterialTheme.padding.mediumSmall,
        )
    }
}

@Composable
fun AutoPlaySwitch(
    isChecked: Boolean,
    onToggleAutoPlay: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            com.google.android.material.switchmaterial.SwitchMaterial(context).apply {
                layoutParams = ViewGroup.LayoutParams(50.dp.toPx().toInt(), 50.dp.toPx().toInt())
                this.isChecked = isChecked
                setOnCheckedChangeListener { _, isChecked ->
                    onToggleAutoPlay(isChecked)
                }
            }
        },
        update = { switch ->
            switch.isChecked = isChecked
            switch.thumbDrawable = if (isChecked) {
                ContextCompat.getDrawable(switch.context, R.drawable.ic_play_circle_filled_24)
            } else {
                ContextCompat.getDrawable(switch.context, R.drawable.ic_pause_circle_filled_24)
            }
        },
        modifier = modifier,
    )
}

fun Dp.toPx(): Float {
    return this.value * Resources.getSystem().displayMetrics.density
}
