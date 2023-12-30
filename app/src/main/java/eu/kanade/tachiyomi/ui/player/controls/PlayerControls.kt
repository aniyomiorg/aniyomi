package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.commonClickable
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.viewer.SeekState

@Composable
fun PlayerControls(
    activity: PlayerActivity,
) {
    val state by activity.viewModel.state.collectAsState()
    if(state.seekState == SeekState.LOCKED) {
        LockedPlayerControls { activity.viewModel.updateSeekState(SeekState.NONE) }
    } else {
        TopPlayerControls(activity)
        MiddlePlayerControls(activity)
        BottomPlayerControls(activity)
    }
}

@Composable
fun LockedPlayerControls(
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

val iconSize = 20.dp
val buttonSize = iconSize + 30.dp
val textButtonWidth = 80.dp

@Composable
fun PlayerIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    multiplier: Int = 1,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val iconSize = iconSize * multiplier
    val buttonSize = iconSize + 30.dp
    IconButton(onClick = onClick, modifier = modifier.size(buttonSize), enabled = enabled){
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
