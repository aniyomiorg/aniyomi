package eu.kanade.tachiyomi.ui.player.cast.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import eu.kanade.tachiyomi.ui.player.CastManager

@Composable
fun CastButton(
    castState: CastManager.CastState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        label = "cast_button_alpha",
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .alpha(alpha)
            .combinedClickable(
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = if (castState == CastManager.CastState.CONNECTED) {
                Icons.Default.CastConnected
            } else {
                Icons.Default.Cast
            },
            contentDescription = null,
            tint = LocalContentColor.current.copy(alpha = alpha),
        )
    }
}
