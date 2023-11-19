package tachiyomi.presentation.widget.entries.anime

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import tachiyomi.presentation.widget.R

class AnimeUpdatesGridCoverScreenGlanceWidget : BaseAnimeUpdatesGridGlanceWidget() {
    override val foreground = ColorProvider(Color.White)
    override val background = ImageProvider(R.drawable.appwidget_coverscreen_background)
    override val topPadding = 0.dp
    override val bottomPadding = 24.dp
}
