package tachiyomi.presentation.widget.entries.manga

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TachiyomiMangaWidgetManager(
    private val database: MangaDatabaseHandler = Injekt.get(),
) {

    fun Context.init(scope: LifecycleCoroutineScope) {
        database.subscribeToList {
            updatesViewQueries.getUpdatesByReadStatus(
                read = false,
                after = MangaUpdatesGridGlanceWidget.DateLimit.timeInMillis,
            )
        }
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
