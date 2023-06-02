package tachiyomi.presentation.widget.entries.anime

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import coil.executeBlocking
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.MainScope
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.updates.anime.interactor.GetAnimeUpdates
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations
import tachiyomi.presentation.widget.R
import tachiyomi.presentation.widget.components.anime.CoverHeight
import tachiyomi.presentation.widget.components.anime.CoverWidth
import tachiyomi.presentation.widget.components.anime.LockedAnimeWidget
import tachiyomi.presentation.widget.components.anime.UpdatesAnimeWidget
import tachiyomi.presentation.widget.util.appWidgetBackgroundRadius
import tachiyomi.presentation.widget.util.calculateRowAndColumnCount
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Calendar
import java.util.Date

class AnimeUpdatesGridGlanceWidget : GlanceAppWidget() {

    private val app: Application by injectLazy()
    private val preferences: SecurityPreferences by injectLazy()

    private val coroutineScope = MainScope()

    private var data: List<Pair<Long, Bitmap?>>? = null

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        // If app lock enabled, don't do anything
        if (preferences.useAuthenticator().get()) {
            LockedAnimeWidget()
            return
        }
        UpdatesAnimeWidget(data)
    }

    fun loadData(list: List<AnimeUpdatesWithRelations>? = null) {
        coroutineScope.launchIO {
            // Don't show anything when lock is active
            if (preferences.useAuthenticator().get()) {
                updateAll(app)
                return@launchIO
            }

            val manager = GlanceAppWidgetManager(app)
            val ids = manager.getGlanceIds(this@AnimeUpdatesGridGlanceWidget::class.java)
            if (ids.isEmpty()) return@launchIO

            val processList = list
                ?: Injekt.get<GetAnimeUpdates>().await(
                    seen = false,
                    after = DateLimit.timeInMillis,
                )
            val (rowCount, columnCount) = ids
                .flatMap { manager.getAppWidgetSizes(it) }
                .maxBy { it.height.value * it.width.value }
                .calculateRowAndColumnCount()

            data = prepareList(processList, rowCount * columnCount)
            ids.forEach { update(app, it) }
        }
    }

    private fun prepareList(processList: List<AnimeUpdatesWithRelations>, take: Int): List<Pair<Long, Bitmap?>> {
        // Resize to cover size
        val widthPx = CoverWidth.value.toInt().dpToPx
        val heightPx = CoverHeight.value.toInt().dpToPx
        val roundPx = app.resources.getDimension(R.dimen.appwidget_inner_radius)
        return processList
            .distinctBy { it.animeId }
            .take(take)
            .map { animeupdatesView ->
                val request = ImageRequest.Builder(app)
                    .data(
                        AnimeCover(
                            animeId = animeupdatesView.animeId,
                            sourceId = animeupdatesView.sourceId,
                            isAnimeFavorite = true,
                            url = animeupdatesView.coverData.url,
                            lastModified = animeupdatesView.coverData.lastModified,
                        ),
                    )
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .precision(Precision.EXACT)
                    .size(widthPx, heightPx)
                    .scale(Scale.FILL)
                    .let {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            it.transformations(RoundedCornersTransformation(roundPx))
                        } else {
                            it // Handled by system
                        }
                    }
                    .build()
                Pair(animeupdatesView.animeId, app.imageLoader.executeBlocking(request).drawable?.toBitmap())
            }
    }

    companion object {
        val DateLimit: Calendar
            get() = Calendar.getInstance().apply {
                time = Date()
                add(Calendar.MONTH, -3)
            }
    }
}

val ContainerModifier = GlanceModifier
    .fillMaxSize()
    .background(ImageProvider(R.drawable.appwidget_background))
    .appWidgetBackground()
    .appWidgetBackgroundRadius()
