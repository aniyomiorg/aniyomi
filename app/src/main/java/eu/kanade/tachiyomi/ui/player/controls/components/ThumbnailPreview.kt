package eu.kanade.tachiyomi.ui.player.controls.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ThumbnailPreview(
    visible: Boolean,
    image: ImageBitmap?,
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val (previewWidth, previewHeight) =
        remember(image) {
            if (image != null) {
                val width = image.width.toFloat()
                val height = image.height.toFloat()
                val aspectRatio = width / height

                if (aspectRatio < 16f / 9f) {
                    124.dp * aspectRatio to 124.dp
                } else {
                    220.dp to 124.dp
                }
            } else {
                220.dp to 124.dp
            }
        }

    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val startInset = safeDrawingPadding.calculateStartPadding(layoutDirection)
    val endInset = safeDrawingPadding.calculateEndPadding(layoutDirection)

    AnimatedVisibility(
        visible = visible && image != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        if (image != null) {
            BoxWithConstraints {
                val screenWidth = maxWidth
                val timerWidth = 96.dp // Width + padding.extraSmall
                val seekbarWidth = screenWidth - timerWidth - timerWidth

                val progress = if (durationMs > 0f) {
                    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                val targetX = startInset + timerWidth + (seekbarWidth * progress)

                val minX = startInset + 8.dp
                val maxX = screenWidth - previewWidth - endInset - 8.dp

                val constrainedX = when {
                    targetX - previewWidth / 2 < minX -> minX
                    targetX + previewWidth / 2 > screenWidth - endInset - 8.dp -> maxX
                    else -> targetX - previewWidth / 2
                }

                Card(
                    modifier = Modifier
                        .offset(x = constrainedX)
                        .size(previewWidth, previewHeight),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}
