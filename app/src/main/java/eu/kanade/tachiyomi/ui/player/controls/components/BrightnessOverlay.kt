package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

@Composable
fun BrightnessOverlay(
    @FloatRange(from = -0.75, to = 1.0) brightness: Float,
    modifier: Modifier = Modifier,
) {
    if (brightness < 0) {
        val brightnessAlpha = remember(brightness) {
            abs(brightness)
        }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = brightnessAlpha
                },
        ) {
            drawRect(Color.Black)
        }
    }
}
