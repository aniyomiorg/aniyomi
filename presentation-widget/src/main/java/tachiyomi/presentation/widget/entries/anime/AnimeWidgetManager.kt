package tachiyomi.presentation.widget.entries.anime

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.LifecycleCoroutineScope
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.updates.anime.interactor.GetAnimeUpdates

class AnimeWidgetManager(
    private val getUpdates: GetAnimeUpdates,
    private val securityPreferences: SecurityPreferences,
) {

    fun Context.init(scope: LifecycleCoroutineScope) {
        combine(
            getUpdates.subscribe(seen = false, after = BaseAnimeUpdatesGridGlanceWidget.DateLimit.toEpochMilli()),
            securityPreferences.useAuthenticator().changes(),
            transform = { a, _ -> a },
        )
            .drop(1)
            .distinctUntilChanged()
            .onEach {
                try {
                    AnimeUpdatesGridGlanceWidget().updateAll(this)
                    AnimeUpdatesGridCoverScreenGlanceWidget().updateAll(this)
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to update widget" }
                }
            }
            .launchIn(scope)
    }
}
