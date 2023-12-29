package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import eu.kanade.tachiyomi.ui.player.viewer.InvertedPlayback
import eu.kanade.tachiyomi.ui.player.viewer.SeekState
import eu.kanade.tachiyomi.ui.player.viewer.components.Seekbar
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun BottomPlayerControls(
    activity: PlayerActivity,
    modifier: Modifier = Modifier,
) {
    val viewModel = activity.viewModel
    val state by viewModel.state.collectAsState()
    val preferences = activity.playerPreferences

    val includePip = !preferences.pipOnExit().get() && activity.pip.supportedAndEnabled

    fun onPositionChange(value: Float, wasSeeking: Boolean) {
        if (!wasSeeking) {
            SeekState.mode = SeekState.SEEKBAR
            activity.initSeek()
        }

        MPVLib.command(arrayOf("seek", value.toInt().toString(), "absolute+keyframes"))

        val duration = activity.player.duration ?: 0
        if (duration == 0 || activity.initialSeek < 0) {
            return
        }

        val difference = value.toInt() - activity.initialSeek

        activity.playerControls.showSeekText(value.toInt(), difference)
    }

    fun onPositionChangeFinished(value: Float) {
        if (SeekState.mode == SeekState.SEEKBAR) {
            if (preferences.playerSmoothSeek().get()) {
                activity.player.timePos = value.toInt()
            } else {
                MPVLib.command(
                    arrayOf("seek", value.toInt().toString(), "absolute+keyframes"),
                )
            }
            SeekState.mode = SeekState.NONE
        } else {
            MPVLib.command(arrayOf("seek", value.toInt().toString(), "absolute+keyframes"))
        }
    }


    BoxWithConstraints(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier.padding(all = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.Bottom) {
            PlayerRow(modifier = Modifier.fillMaxWidth()) {

                // Bottom - Left Controls
                PlayerRow {
                    PlayerIcon(Icons.Outlined.Lock) { activity.playerControls.lockControls(true) }
                    PlayerIcon(Icons.Outlined.ScreenRotation) { activity.rotatePlayer() }
                    PlayerTextButton(
                        text = stringResource(
                            id = R.string.ui_speed,
                            preferences.playerSpeed().collectAsState().value,
                        ),
                        onClick = activity::cycleSpeed,
                        onLongClick = activity.viewModel::showSpeedPicker,
                    )
                }

                // Bottom - Right Controls
                PlayerRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PlayerTextButton(
                        text = state.skipIntroText,
                        onClick = activity::skipIntro,
                        onLongClick = activity.viewModel::showSkipIntroLength,
                    )

                    PlayerIcon(Icons.Outlined.Fullscreen) { activity.playerControls.cycleViewMode() }

                    if (includePip) PlayerIcon(Icons.Outlined.PictureInPictureAlt) { activity.pip.start() }
                }
            }

            PlayerRow(modifier = Modifier.fillMaxWidth()) {
                fun getTimeText(time: Long) = Utils.prettyTime(time.toInt(), true).replace("+", "")
                val invertedPlayback = preferences.invertedPlayback().collectAsState().value

                val position = when (invertedPlayback) {
                    InvertedPlayback.NONE, InvertedPlayback.DURATION -> state.timeData.position
                    InvertedPlayback.POSITION -> state.timeData.position - state.timeData.duration
                }
                val onPositionCLicked = {
                    preferences.invertedPlayback().set(
                        when (invertedPlayback) {
                            InvertedPlayback.NONE, InvertedPlayback.DURATION -> InvertedPlayback.POSITION
                            InvertedPlayback.POSITION -> InvertedPlayback.NONE
                        },
                    )
                }

                val duration = when (invertedPlayback) {
                    InvertedPlayback.NONE, InvertedPlayback.POSITION -> state.timeData.duration
                    InvertedPlayback.DURATION -> state.timeData.position - state.timeData.duration
                }
                val onDurationCLicked = {
                    preferences.invertedPlayback().set(
                        when (invertedPlayback) {
                            InvertedPlayback.NONE, InvertedPlayback.POSITION -> InvertedPlayback.DURATION
                            InvertedPlayback.DURATION -> InvertedPlayback.NONE
                        }
                    )
                }

                PlayerTextButton(text = getTimeText(position), onClick = onPositionCLicked)

                Seekbar(
                    position = state.timeData.position.toFloat(),
                    duration = state.timeData.duration.toFloat(),
                    readAhead = state.timeData.readAhead.toFloat(),
                    chapters = state.videoChapters,
                    onPositionChange = ::onPositionChange,
                    onPositionChangeFinished = ::onPositionChangeFinished,
                    modifier = Modifier.widthIn(max = this@BoxWithConstraints.maxWidth - (textButtonWidth * 2))
                )

                PlayerTextButton(text = getTimeText(duration), onClick = onDurationCLicked)
            }
        }
    }
}
