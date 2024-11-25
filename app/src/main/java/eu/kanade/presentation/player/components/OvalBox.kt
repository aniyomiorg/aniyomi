package eu.kanade.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

object RightSideOvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            moveTo(size.width, size.height)
            lineTo(size.width, 0f)
            lineTo(size.width / 10, 0f)
            cubicTo(
                size.width / 10,
                0f,
                -30f,
                size.height / 2,
                size.width / 10,
                size.height,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

object LeftSideOvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height)
            lineTo(size.width - size.width / 10, size.height)
            cubicTo(
                size.width - size.width / 10,
                size.height,
                size.width,
                size.height / 2,
                size.width - size.width / 10,
                0f,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

@Preview
@Composable
private fun PreviewRightSideOvalBox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RightSideOvalShape)
            .background(Color.Red),
    ) {}
}

@Preview
@Composable
private fun PreviewLeftSideOvalBox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(LeftSideOvalShape)
            .background(Color.Red),
    ) {}
}
