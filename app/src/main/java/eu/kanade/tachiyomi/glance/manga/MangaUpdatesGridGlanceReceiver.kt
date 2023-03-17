package eu.kanade.tachiyomi.glance.manga

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MangaUpdatesGridGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MangaUpdatesGridGlanceWidget().apply { loadData() }
}
