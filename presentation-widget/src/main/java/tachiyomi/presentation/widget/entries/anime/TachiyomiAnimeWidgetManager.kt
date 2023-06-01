package tachiyomi.presentation.widget.entries.anime

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TachiyomiAnimeWidgetManager(
    private val database: AnimeDatabaseHandler = Injekt.get(),
) {

    fun Context.init(scope: LifecycleCoroutineScope) {
        database.subscribeToList {
            animeupdatesViewQueries.getUpdatesBySeenStatus(
                seen = false,
                after = AnimeUpdatesGridGlanceWidget.DateLimit.timeInMillis,
            )
        }
            .drop(1)
            .distinctUntilChanged()
            .onEach {
                val manager = GlanceAppWidgetManager(this)
                if (manager.getGlanceIds(AnimeUpdatesGridGlanceWidget::class.java).isNotEmpty()) {
                    AnimeUpdatesGridGlanceWidget().loadData(it)
                }
            }
            .launchIn(scope)
    }
}
