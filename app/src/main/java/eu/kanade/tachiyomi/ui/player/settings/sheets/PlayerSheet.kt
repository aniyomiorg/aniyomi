package eu.kanade.tachiyomi.ui.player.settings.sheets

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.ScrimAnimationSpec
import tachiyomi.presentation.core.components.SheetAnimationSpec
import tachiyomi.presentation.core.components.preUpPostDownNestedScrollConnection
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerSheet(
    @StringRes titleRes: Int,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val swipeState = rememberSwipeableState(
        initialValue = 1,
        animationSpec = SheetAnimationSpec,
    )
    val internalOnDismissRequest: () -> Unit = { if (swipeState.currentValue == 0) scope.launch { swipeState.animateTo(1) } }
    BoxWithConstraints(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = internalOnDismissRequest,
            )
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val fullHeight = constraints.maxHeight.toFloat()
        val anchors = mapOf(0f to 0, fullHeight to 1)
        val scrimAlpha by animateFloatAsState(
            targetValue = if (swipeState.targetValue == 1) 0f else 1f,
            animationSpec = ScrimAnimationSpec,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(scrimAlpha)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        )
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .nestedScroll(
                    remember(anchors) {
                        swipeState.preUpPostDownNestedScrollConnection(
                            enabled = true,
                            anchor = anchors,
                        )
                    },
                )
                .offset {
                    IntOffset(
                        0,
                        swipeState.offset.value.roundToInt(),
                    )
                }
                .swipeable(
                    enabled = true,
                    state = swipeState,
                    anchors = anchors,
                    orientation = Orientation.Vertical,
                    resistance = null,
                ),
            shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
            tonalElevation = 1.dp,
            content = {
                BackHandler(enabled = swipeState.targetValue == 0, onBack = internalOnDismissRequest)
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    content()
                }
            },
        )

        LaunchedEffect(swipeState) {
            scope.launch { swipeState.animateTo(0) }
            snapshotFlow { swipeState.currentValue }
                .drop(1)
                .filter { it == 1 }
                .collectLatest {
                    delay(ScrimAnimationSpec.durationMillis.milliseconds)
                    onDismissRequest()
                }
        }
    }
}
