package tachiyomi.presentation.widget.entries.manga

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import tachiyomi.presentation.widget.R

class MangaUpdatesGridCoverScreenGlanceWidget : BaseMangaUpdatesGridGlanceWidget() {
    override val foreground = ColorProvider(Color.White)
    override val background = ImageProvider(R.drawable.appwidget_coverscreen_background)
    override val topPadding = 0.dp
    override val bottomPadding = 24.dp
}
