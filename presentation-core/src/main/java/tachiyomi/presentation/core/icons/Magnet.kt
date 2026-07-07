package tachiyomi.presentation.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// License: MIT. Made by Neuicons: https://github.com/neuicons/neu
val CustomIcons.Magnet: ImageVector
    get() {
        if (_magnet != null) return _magnet!!

        _magnet = ImageVector.Builder(
            name = "Magnet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                moveTo(21.7f, 12.818f)
                arcToRelative(1.022f, 1.022f, 0f, false, true, 0f, 1.445f)
                lineTo(20.154f, 15.81f)
                lineToRelative(-3.589f, -3.589f)
                lineToRelative(1.547f, -1.548f)
                arcToRelative(1.022f, 1.022f, 0f, false, true, 1.444f, 0f)
                close()
                moveTo(9.737f, 2.3f)
                lineTo(8.19f, 3.846f)
                lineToRelative(3.59f, 3.589f)
                lineToRelative(1.546f, -1.547f)
                arcToRelative(1.021f, 1.021f, 0f, false, false, 0f, -1.444f)
                lineTo(11.181f, 2.3f)
                arcTo(1.021f, 1.021f, 0f, false, false, 9.737f, 2.3f)
                close()
                moveTo(4.478f, 19.522f)
                arcToRelative(8.458f, 8.458f, 0f, false, false, 11.963f, 0f)
                lineToRelative(2.269f, -2.268f)
                lineToRelative(-3.589f, -3.589f)
                lineToRelative(-2.269f, 2.268f)
                arcToRelative(3.384f, 3.384f, 0f, false, true, -4.785f, -4.785f)
                lineToRelative(2.269f, -2.269f)
                lineTo(6.747f, 5.29f)
                lineTo(4.478f, 7.559f)
                arcTo(8.458f, 8.458f, 0f, false, false, 4.478f, 19.522f)
                close()
            }
        }.build()

        return _magnet!!
    }

@Suppress("ObjectPropertyName")
private var _magnet: ImageVector? = null
