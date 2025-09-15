/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.presentation.player.components

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val sheetAnimationSpec = tween<Float>(350)

@Composable
fun PlayerSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 1.dp,
    dismissEvent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val maxWidth = if (LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE) {
        720.dp
    } else {
        420.dp
    }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .95f

    var backgroundAlpha by remember { mutableFloatStateOf(0f) }
    val alpha by animateFloatAsState(
        backgroundAlpha,
        animationSpec = sheetAnimationSpec,
        label = "alpha",
    )

    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = 1,
            snapAnimationSpec = sheetAnimationSpec,
            decayAnimationSpec = decayAnimationSpec,
            positionalThreshold = { with(density) { 56.dp.toPx() } },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
        )
    }

    LaunchedEffect(dismissEvent) {
        if (dismissEvent) {
            backgroundAlpha = 0f
            anchoredDraggableState.animateTo(1)
            onDismissRequest()
        }
    }

    val internalOnDismissRequest = {
        if (anchoredDraggableState.currentValue == 0) {
            scope.launch {
                backgroundAlpha = 0f
                anchoredDraggableState.animateTo(1)
            }
        }
    }
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = internalOnDismissRequest,
            )
            .fillMaxSize()
            .background(Color.Black.copy(alpha))
            .onSizeChanged {
                val anchors = DraggableAnchors {
                    0 at 0f
                    1 at it.height.toFloat()
                }
                anchoredDraggableState.updateAnchors(anchors)
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .nestedScroll(
                    remember(anchoredDraggableState) {
                        anchoredDraggableState.preUpPostDownNestedScrollConnection()
                    },
                )
                .then(modifier)
                .offset {
                    IntOffset(
                        0,
                        anchoredDraggableState.offset
                            .takeIf { it.isFinite() }
                            ?.roundToInt()
                            ?: 0,
                    )
                }
                .anchoredDraggable(
                    state = anchoredDraggableState,
                    orientation = Orientation.Vertical,
                )
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
            shape = MaterialTheme.shapes.extraLarge.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
            tonalElevation = tonalElevation,
            content = {
                BackHandler(
                    enabled = anchoredDraggableState.targetValue == 0,
                    onBack = internalOnDismissRequest,
                )
                content()
            },
        )

        LaunchedEffect(true) {
            backgroundAlpha = 0.5f
        }

        LaunchedEffect(anchoredDraggableState) {
            scope.launch { anchoredDraggableState.animateTo(0) }
            snapshotFlow { anchoredDraggableState.currentValue }
                .drop(1)
                .filter { it == 1 }
                .collectLatest { latestOnDismissRequest() }
        }
    }
}

private fun <T> AnchoredDraggableState<T>.preUpPostDownNestedScrollConnection() = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.toFloat()
        return if (delta < 0 && source == NestedScrollSource.UserInput) {
            dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        return if (source == NestedScrollSource.UserInput) {
            dispatchRawDelta(available.toFloat()).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.toFloat()
        return if (toFling < 0 && offset > anchors.minAnchor()) {
            settle(toFling)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val toFling = available.toFloat()
        return if (toFling > 0) {
            settle(toFling)
            available
        } else {
            Velocity.Zero
        }
    }

    private fun Float.toOffset(): Offset = Offset(0f, this)

    @JvmName("velocityToFloat")
    private fun Velocity.toFloat() = y

    private fun Offset.toFloat(): Float = y
}
