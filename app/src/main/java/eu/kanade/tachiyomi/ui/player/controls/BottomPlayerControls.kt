package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.viewer.AspectState
import eu.kanade.tachiyomi.ui.player.viewer.InvertedPlayback
import eu.kanade.tachiyomi.ui.player.viewer.SeekState
import eu.kanade.tachiyomi.ui.player.viewer.components.Seekbar
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVView
import `is`.xyz.mpv.Utils
import tachiyomi.presentation.core.components.material.padding
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



    fun setViewMode() {
        viewModel.updatePlayerInformation(AspectState.mode.stringRes)
        var aspect = "-1"
        var pan = "1.0"
        when (AspectState.mode) {
            AspectState.CROP -> {
                pan = "1.0"
            }
            AspectState.FIT -> {
                pan = "0.0"
            }
            AspectState.STRETCH -> {
                aspect = "${activity.deviceWidth}/${activity.deviceHeight}"
                pan = "0.0"
            }
            AspectState.CUSTOM -> {
                aspect = MPVLib.getPropertyString("video-aspect-override")
            }
        }

        MPVLib.setPropertyString("video-aspect-override", aspect)
        MPVLib.setPropertyString("panscan", pan)
        preferences.aspectState().set(AspectState.mode)
    }

    fun cycleViewMode() {
        AspectState.mode = when (AspectState.mode) {
            AspectState.FIT -> AspectState.CROP
            AspectState.CROP -> AspectState.STRETCH
            else -> AspectState.FIT
        }
        setViewMode()
    }

    BoxWithConstraints(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier.padding(all = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.Bottom) {
            PlayerRow(modifier = Modifier.fillMaxWidth()) {

                // Bottom - Left Controls
                PlayerRow {
                    PlayerIcon(icon = Icons.Outlined.Lock) { viewModel.updateSeekState(SeekState.LOCKED) }
                    PlayerIcon(icon = Icons.Outlined.ScreenRotation) { activity.rotatePlayer() }
                    PlayerTextButton(
                        text = stringResource(
                            id = R.string.ui_speed,
                            preferences.playerSpeed().collectAsState().value,
                        ),
                        onClick = activity::cycleSpeed,
                        onLongClick = activity.viewModel::showSpeedPicker,
                    )

                    if (state.videoChapters.isNotEmpty()) {
                        val currentChapter = state.videoChapters.last { it.time <= state.timeData.position }
                        ChapterButton(chapter = currentChapter)
                    }

                }

                // Bottom - Right Controls
                PlayerRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PlayerTextButton(
                        text = state.skipIntroText,
                        onClick = activity::skipIntro,
                        onLongClick = activity.viewModel::showSkipIntroLength,
                    )

                    PlayerIcon(icon = Icons.Outlined.Fullscreen) { cycleViewMode() }

                    if (includePip) PlayerIcon(icon = Icons.Outlined.PictureInPictureAlt) { activity.pip.start() }
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

@Composable
private fun ChapterButton(
    chapter: MPVView.Chapter,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(25))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6F))
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
    ) {
        AnimatedContent(
            targetState = chapter,
            transitionSpec = {
                if (targetState.time > initialState.time) {
                    (slideInVertically { height -> height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> height } + fadeOut())
                }.using(
                    SizeTransform(clip = false),
                )
            },
            label = "Chapter",
        ) { currentChapter ->
            Row {
                Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = MaterialTheme.padding.small)
                        .size(16.dp),
                )
                Text(
                    text = Utils.prettyTime(currentChapter.time.toInt()),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                currentChapter.title?.let {
                    Text(
                        text = " • ",
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    Text(
                        text = it,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
