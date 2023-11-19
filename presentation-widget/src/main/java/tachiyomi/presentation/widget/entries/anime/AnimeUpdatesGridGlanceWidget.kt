package tachiyomi.presentation.widget.entries.anime

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
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
import tachiyomi.core.util.lang.withIOContext
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
import java.util.Calendar
import java.util.Date

class AnimeUpdatesGridGlanceWidget(
    private val context: Context = Injekt.get<Application>(),
    private val getUpdates: GetAnimeUpdates = Injekt.get(),
    private val preferences: SecurityPreferences = Injekt.get(),
) : GlanceAppWidget() {

    private var data: List<Pair<Long, Bitmap?>>? = null

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val locked = preferences.useAuthenticator().get()
        if (!locked) loadData()

        provideContent {
            // If app lock enabled, don't do anything
            if (locked) {
                LockedAnimeWidget()
                return@provideContent
            }
            UpdatesAnimeWidget(data)
        }
    }

    private suspend fun loadData() {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(this@AnimeUpdatesGridGlanceWidget::class.java)
        if (ids.isEmpty()) return

        withIOContext {
            val updates = getUpdates.await(
                seen = false,
                after = DateLimit.timeInMillis,
            )
            val (rowCount, columnCount) = ids
                .flatMap { manager.getAppWidgetSizes(it) }
                .maxBy { it.height.value * it.width.value }
                .calculateRowAndColumnCount()

            data = prepareList(updates, rowCount * columnCount)
        }
    }

    private fun prepareList(processList: List<AnimeUpdatesWithRelations>, take: Int): List<Pair<Long, Bitmap?>> {
        // Resize to cover size
        val widthPx = CoverWidth.value.toInt().dpToPx
        val heightPx = CoverHeight.value.toInt().dpToPx
        val roundPx = context.resources.getDimension(R.dimen.appwidget_inner_radius)
        return processList
            .distinctBy { it.animeId }
            .take(take)
            .map { animeupdatesView ->
                val request = ImageRequest.Builder(context)
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
                Pair(
                    animeupdatesView.animeId,
                    context.imageLoader.executeBlocking(request).drawable?.toBitmap(),
                )
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
