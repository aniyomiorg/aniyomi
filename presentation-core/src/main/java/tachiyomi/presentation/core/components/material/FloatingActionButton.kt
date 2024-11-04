package tachiyomi.presentation.core.components.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * ExtendedFloatingActionButton with custom transition between collapsed/expanded state.
 *
 * @see androidx.compose.material3.ExtendedFloatingActionButton
 */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
    FloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
    ) {
        val minWidth by animateDpAsState(
            targetValue = if (expanded) ExtendedFabMinimumWidth else FabContainerWidth,
            animationSpec = tween(
                durationMillis = 500,
                easing = EasingEmphasizedCubicBezier,
            ),
            label = "minWidth",
        )
        val startPadding by animateDpAsState(
            targetValue = if (expanded) ExtendedFabIconSize / 2 else 0.dp,
            animationSpec = tween(
                durationMillis = if (expanded) 300 else 900,
                easing = EasingEmphasizedCubicBezier,
            ),
            label = "startPadding",
        )

        Row(
            modifier = Modifier
                .sizeIn(minWidth = minWidth)
                .padding(start = startPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon()
            AnimatedVisibility(
                visible = expanded,
                enter = ExtendedFabExpandAnimation,
                exit = ExtendedFabCollapseAnimation,
            ) {
                Box(modifier = Modifier.padding(start = ExtendedFabIconPadding, end = ExtendedFabTextPadding)) {
                    text()
                }
            }
        }
    }
}

private val EasingLinearCubicBezier = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)
private val EasingEmphasizedCubicBezier = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

private val ExtendedFabMinimumWidth = 80.dp
private val ExtendedFabIconSize = 24.0.dp
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp

private val ExtendedFabCollapseAnimation = fadeOut(
    animationSpec = tween(
        durationMillis = 100,
        easing = EasingLinearCubicBezier,
    ),
) + shrinkHorizontally(
    animationSpec = tween(
        durationMillis = 500,
        easing = EasingEmphasizedCubicBezier,
    ),
    shrinkTowards = Alignment.Start,
)

private val ExtendedFabExpandAnimation = fadeIn(
    animationSpec = tween(
        durationMillis = 200,
        delayMillis = 100,
        easing = EasingLinearCubicBezier,
    ),
) + expandHorizontally(
    animationSpec = tween(
        durationMillis = 500,
        easing = EasingEmphasizedCubicBezier,
    ),
    expandFrom = Alignment.Start,
)

private val FabContainerWidth = 56.0.dp
