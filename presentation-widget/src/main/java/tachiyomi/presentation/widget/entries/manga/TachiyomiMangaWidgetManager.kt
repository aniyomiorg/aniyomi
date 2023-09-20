package tachiyomi.presentation.widget.entries.manga

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.domain.updates.manga.interactor.GetMangaUpdates

class TachiyomiMangaWidgetManager(
    private val getUpdates: GetMangaUpdates,
) {

    fun Context.init(scope: LifecycleCoroutineScope) {
        getUpdates.subscribe(
            read = false,
            after = MangaUpdatesGridGlanceWidget.DateLimit.timeInMillis,
        )
            .drop(1)
            .distinctUntilChanged()
            .onEach {
                val manager = GlanceAppWidgetManager(this)
                if (manager.getGlanceIds(MangaUpdatesGridGlanceWidget::class.java).isNotEmpty()) {
                    MangaUpdatesGridGlanceWidget().loadData(it)
                }
            }
            .launchIn(scope)
    }
}
