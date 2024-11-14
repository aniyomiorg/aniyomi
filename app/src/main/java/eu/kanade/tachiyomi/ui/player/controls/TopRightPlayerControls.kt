package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.Decoder
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton

@Composable
fun TopRightPlayerControls(
    // decoder
    decoder: Decoder,
    onDecoderClick: () -> Unit,
    onDecoderLongClick: () -> Unit,

    // subtitles
    onSubtitlesClick: () -> Unit,
    onSubtitlesLongClick: () -> Unit,

    // audio
    onAudioClick: () -> Unit,
    onAudioLongClick: () -> Unit,

    // more
    onMoreClick: () -> Unit,
    onMoreLongClick: () -> Unit,

    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlsButton(
            decoder.title,
            onClick = onDecoderClick,
            onLongClick = onDecoderLongClick,
        )
        ControlsButton(
            Icons.Default.Subtitles,
            onClick = onSubtitlesClick,
            onLongClick = onSubtitlesLongClick,
        )
        ControlsButton(
            Icons.Default.Audiotrack,
            onClick = onAudioClick,
            onLongClick = onAudioLongClick,
        )
        ControlsButton(
            Icons.Default.MoreVert,
            onClick = onMoreClick,
            onLongClick = onMoreLongClick,
        )
    }
}
