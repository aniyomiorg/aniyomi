package tachiyomi.presentation.widget.entries.manga

import androidx.compose.ui.unit.dp
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import tachiyomi.presentation.widget.R

class MangaUpdatesGridGlanceWidget : BaseMangaUpdatesGridGlanceWidget() {
    override val foreground = ColorProvider(R.color.appwidget_on_secondary_container)
    override val background = ImageProvider(R.drawable.appwidget_background)
    override val topPadding = 0.dp
    override val bottomPadding = 0.dp
}
