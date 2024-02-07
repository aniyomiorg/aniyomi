package eu.kanade.tachiyomi.ui.player.controls

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.commonClickable
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.viewer.SeekState
import kotlinx.coroutines.delay

@Composable
fun PlayerControls(
    activity: PlayerActivity,
    modifier: Modifier = Modifier,
    timerState: TimerState = rememberTimerState(),
) {
    val state by activity.viewModel.state.collectAsState()

    val onPlayerPressed = { timerState.controls = if (timerState.controls > 0L) 0L else 3000L }
    val playerModifier = modifier.pointerInput(Unit) { detectTapGestures(onPress = { onPlayerPressed() }) }

    Surface(modifier = playerModifier, color = Color.Transparent) {
        if (state.seekState == SeekState.LOCKED) {
            LockedPlayerControls { activity.viewModel.updateSeekState(SeekState.NONE) }
        } else {
            if (timerState.controls > 0L) UnlockedPlayerControls(activity)
        }

        Text(timerState.controls.toString())
    }

    LaunchedEffect(key1 = timerState.controls, key2 = state.timeData.paused) {
        if(timerState.controls > 0L && !state.timeData.paused) {
            delay(1000L)
            timerState.controls -= 1000L
        }
    }
}

@Composable
private fun LockedPlayerControls(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier.padding(all = 10.dp)
    ) {
        PlayerIcon(icon = Icons.Outlined.LockOpen, onClick = onClick)
    }
}

@SuppressLint("ComposeModifierReused")
@Composable
private fun UnlockedPlayerControls(
    activity: PlayerActivity,
    modifier: Modifier = Modifier,
) {
    Surface(color = Color(color = 0x70000000)) {
        TopPlayerControls(activity)
        MiddlePlayerControls(activity)
        BottomPlayerControls(activity)
    }
}

val iconSize = 20.dp
val buttonSize = iconSize + 30.dp
val textButtonWidth = 80.dp

@Composable
fun PlayerIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    multiplier: Int = 1,
    enabled: Boolean = true,
    timerState: TimerState = rememberTimerState(),
    onClick: () -> Unit = {},
) {
    val iconSize = iconSize * multiplier
    val buttonSize = iconSize + 30.dp
    val onPlayerPressed = { timerState.controls = if (timerState.controls > 0L) 0L else 3500L }

        IconButton(onClick = { onClick(); onPlayerPressed() }, modifier = modifier.size(buttonSize), enabled = enabled){
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun PlayerRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) = Row(
    modifier = modifier,
    horizontalArrangement = horizontalArrangement,
    verticalAlignment = verticalAlignment,
    content = content
)

@Composable
fun PlayerTextButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
){
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = textButtonWidth, height = buttonSize)
            .commonClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

class TimerState (
    var controls: Long = 3500L,
)

@Composable
fun rememberTimerState(
    controls: Long = 3500L,
): TimerState {
    return remember {
        TimerState(
            controls = controls,
        )
    }
}
