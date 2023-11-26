package tachiyomi.presentation.widget.entries.manga

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MangaUpdatesGridCoverScreenGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = MangaUpdatesGridCoverScreenGlanceWidget()
}
