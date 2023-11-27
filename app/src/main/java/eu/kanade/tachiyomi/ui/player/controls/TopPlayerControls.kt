package eu.kanade.tachiyomi.ui.player.controls

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun TopPlayerControls(
    activity: PlayerActivity,
) {
    val viewModel = activity.viewModel
    val state by viewModel.state.collectAsState()

    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    Box(
        contentAlignment = Alignment.TopStart,
        modifier = Modifier.padding(all = 10.dp)
    ) {
        SecondaryTopControlsLayout(isPortrait) {

            PlayerRow {
                @Suppress("DEPRECATION")
                PlayerIcon(Icons.Outlined.ArrowBack) { activity.onBackPressed() }

                PlayerRow(modifier = Modifier.clickable { viewModel.showEpisodeList() }) {
                    Column(Modifier.padding(horizontal = 10.dp)) {
                        Text(
                            text = state.anime?.title ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = state.episode?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0.5f),
                        )
                    }

                    val expanded = state.dialog == PlayerViewModel.Dialog.EpisodeList
                    val rotation by animateFloatAsState(if (expanded) 90f else 0f)
                    PlayerIcon(
                        icon = Icons.Outlined.ChevronRight,
                        modifier = Modifier.rotate(rotation),
                    ) { viewModel.showEpisodeList() }
                }
            }

            PlayerRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isPortrait) Arrangement.SpaceEvenly else Arrangement.End,
            ) {
                AutoplaySwitch(
                    checked = activity.playerPreferences.autoplayEnabled().collectAsState(),
                    onClick = activity.playerControls::toggleAutoplay,
                )

                if (state.videoChapters.isNotEmpty()) {
                    PlayerIcon(Icons.Outlined.AutoStories) {
                        viewModel.showVideoChapters()
                    }
                }

                PlayerIcon(Icons.Outlined.VideoSettings) { viewModel.showStreamsCatalog() }
                PlayerIcon(Icons.Outlined.MoreVert) { viewModel.showPlayerSettings() }

            }
        }
    }
}

@Composable
private fun SecondaryTopControlsLayout(
    isPortrait: Boolean,
    content: @Composable () -> Unit
) {

    if (isPortrait) {
        Column { content() }
    } else {
        PlayerRow { content() }
    }
}

@Composable
private fun AutoplaySwitch(
    checked: State<Boolean>,
    onClick: (Boolean) -> Unit,
) {
    Switch(
        checked = checked.value,
        onCheckedChange = onClick,
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.small),
        thumbContent = if (checked.value) {
            {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        }
    )
}
