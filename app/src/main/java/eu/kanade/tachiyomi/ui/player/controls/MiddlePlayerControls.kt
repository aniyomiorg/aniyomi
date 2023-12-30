package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MiddlePlayerControls(
    activity: PlayerActivity,
    modifier: Modifier = Modifier,
) {
    val viewModel = activity.viewModel
    val state by viewModel.state.collectAsState()

    val playPauseIcon = if(state.timeData.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause

    fun switchEpisode(previous: Boolean) {
        return activity.changeEpisode(viewModel.getAdjacentEpisodeId(previous = previous))
    }

    val plCount = viewModel.currentPlaylist.size
    val plPos = viewModel.getCurrentEpisodeIndex()

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
            PlayerRow(horizontalArrangement = Arrangement.spacedBy(iconSize * 2)) {
                PlayerIcon(
                    icon = Icons.Filled.SkipPrevious,
                    multiplier = 2,
                    enabled = plPos != 0
                ) { switchEpisode(previous = true) }

                PlayerIcon(icon = playPauseIcon, multiplier = 3) { activity.playerControls.playPause() }

                PlayerIcon(
                    icon = Icons.Filled.SkipNext,
                    multiplier = 2,
                    enabled = plPos != plCount - 1
                ) { switchEpisode(previous = false) }
            }

            Text(
                text = stringResource(resource = state.playerInformation),
                modifier = Modifier.padding(top = iconSize * 8),
            )
    }
}
