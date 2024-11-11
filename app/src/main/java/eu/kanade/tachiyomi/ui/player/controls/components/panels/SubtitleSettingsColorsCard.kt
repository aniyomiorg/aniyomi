package eu.kanade.tachiyomi.ui.player.controls.components.panels

import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

class SubtitleSettingsColorsCard {
}

fun Int.copyAsArgb(
    alpha: Int = this.alpha,
    red: Int = this.red,
    green: Int = this.green,
    blue: Int = this.blue,
) = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

@OptIn(ExperimentalStdlibApi::class)
fun Int.toColorHexString() = "#" + this.toHexString().uppercase()
