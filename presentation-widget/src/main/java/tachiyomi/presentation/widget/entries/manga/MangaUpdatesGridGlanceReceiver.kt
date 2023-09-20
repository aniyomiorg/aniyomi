package tachiyomi.presentation.widget.entries.manga

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MangaUpdatesGridGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MangaUpdatesGridGlanceWidget().apply { loadData() }
}
