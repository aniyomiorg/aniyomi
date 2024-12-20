package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.player.controls.LocalPlayerButtonsClickEvent

@Composable
fun AutoPlaySwitch(
    isChecked: Boolean,
    onToggleAutoPlay: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickEvent = LocalPlayerButtonsClickEvent.current

    val alignment = remember { Animatable(if (isChecked) 1f else -1f) }

    LaunchedEffect(isChecked) {
        alignment.animateTo(
            targetValue = if (isChecked) 1f else -1f,
            animationSpec = tween(durationMillis = 250),
        )
    }

    val (thumbIcon, thumbColor, trackColor) = if (isChecked) {
        Triple(
            Icons.Filled.PlayCircle,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondaryContainer,

        )
    } else {
        Triple(
            Icons.Filled.PauseCircle,
            Color.White,
            Color.LightGray,
        )
    }

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
        ) {
            clickEvent()
            onToggleAutoPlay(!isChecked)
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 5 / 6f)
                .fillMaxHeight(fraction = 2 / 3f)
                .background(color = trackColor, shape = ShapeDefaults.ExtraLarge),
        )

        Icon(
            imageVector = thumbIcon,
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .background(color = Color.Unspecified, shape = CircleShape)
                .indication(interactionSource, ripple(bounded = false, radius = iconSize * 2 / 3))
                .align(BiasAlignment(horizontalBias = alignment.value, verticalBias = 0f)),
            tint = thumbColor,
        )
    }
}
