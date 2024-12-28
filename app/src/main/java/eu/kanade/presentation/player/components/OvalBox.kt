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
