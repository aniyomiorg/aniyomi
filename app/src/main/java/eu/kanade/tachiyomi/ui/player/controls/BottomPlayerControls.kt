package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun BottomPlayerControls(
    activity: PlayerActivity,
) {
    val viewModel = activity.viewModel
    val state by viewModel.state.collectAsState()

    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = Modifier.padding(all = 10.dp)
    ) {
        Column {
            PlayerRow {
                PlayerRow {
                    PlayerIcon(Icons.Outlined.Lock) { activity.playerControls.lockControls(true) }
                    PlayerIcon(Icons.Outlined.ScreenRotation) { activity.rotatePlayer() }
                    PlayerTextButton(
                        text = stringResource(
                            id = R.string.ui_speed,
                            activity.playerPreferences.playerSpeed().collectAsState().value,
                        ),
                        onClick = activity::cycleSpeed,
                        onLongClick = activity.viewModel::showSpeedPicker,
                    )
                }

                PlayerRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PlayerTextButton(
                        text = state.skipIntroText,
                        onClick = activity::skipIntro,
                        onLongClick = activity.viewModel::showSkipIntroLength,
                    )

                    PlayerIcon(Icons.Outlined.Fullscreen) { activity.playerControls.cycleViewMode() }

                    if (!activity.playerPreferences.pipOnExit().get() && activity.pip.supportedAndEnabled)
                        PlayerIcon(Icons.Outlined.PictureInPictureAlt) { activity.pip.start() }
                }
            }
        }
    }
}
