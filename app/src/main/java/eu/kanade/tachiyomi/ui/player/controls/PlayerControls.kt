package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.commonClickable
import eu.kanade.tachiyomi.ui.player.PlayerActivity

@Composable
fun PlayerControls(
    activity: PlayerActivity,
) {
    TopPlayerControls(activity)
}

@Composable
fun PlayerIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    IconButton(onClick = onClick, modifier = modifier.size(50.dp)){
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = modifier.size(20.dp)
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
){
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(80.dp)
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
