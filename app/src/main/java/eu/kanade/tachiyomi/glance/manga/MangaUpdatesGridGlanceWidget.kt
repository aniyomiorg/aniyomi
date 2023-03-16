package eu.kanade.tachiyomi.glance.manga

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.executeBlocking
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import eu.kanade.data.handlers.manga.MangaDatabaseHandler
import eu.kanade.domain.entries.manga.model.MangaCover
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.glance.*
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.Constants
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.MainScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import view.UpdatesView
import java.util.*

class MangaUpdatesGridGlanceWidget : GlanceAppWidget() {
    private val app: Application by injectLazy()
    private val preferences: SecurityPreferences by injectLazy()

    private val coroutineScope = MainScope()

    var data: List<Pair<Long, Bitmap?>>? = null

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        // App lock enabled, don't do anything
        if (preferences.useAuthenticator().get()) {
            WidgetNotAvailable()
        } else {
            UpdatesWidget()
        }
    }

    @Composable
    private fun WidgetNotAvailable() {
        val intent = Intent(LocalContext.current, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        Box(
            modifier = GlanceModifier
                .clickable(actionStartActivity(intent))
                .then(ContainerModifier)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.appwidget_unavailable_locked),
                style = TextStyle(
                    color = ColorProvider(R.color.appwidget_on_secondary_container),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }

    @Composable
    private fun UpdatesWidget() {
        val (rowCount, columnCount) = LocalSize.current.calculateRowAndColumnCount()
        Column(
            modifier = ContainerModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val inData = data
            if (inData == null) {
                CircularProgressIndicator()
            } else if (inData.isEmpty()) {
                Text(text = stringResource(R.string.information_no_recent))
            } else {
                (0 until rowCount).forEach { i ->
                    val coverRow = (0 until columnCount).mapNotNull { j ->
                        inData.getOrNull(j + (i * columnCount))
                    }
                    if (coverRow.isNotEmpty()) {
                        Row(
                            modifier = GlanceModifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            coverRow.forEach { (mangaId, cover) ->
                                Box(
                                    modifier = GlanceModifier
                                        .padding(horizontal = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val intent = Intent(
                                        LocalContext.current,
                                        MainActivity::class.java,
                                    ).apply {
                                        action = MainActivity.SHORTCUT_MANGA
                                        putExtra(Constants.MANGA_EXTRA, mangaId)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                                        // https://issuetracker.google.com/issues/238793260
                                        addCategory(mangaId.toString())
                                    }
                                    Cover(
                                        modifier = GlanceModifier.clickable(
                                            actionStartActivity(
                                                intent,
                                            ),
                                        ),
                                        cover = cover,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Cover(
        modifier: GlanceModifier = GlanceModifier,
        cover: Bitmap?,
    ) {
        Box(
            modifier = modifier
                .size(width = CoverWidth, height = CoverHeight)
                .appWidgetInnerRadius(),
        ) {
            if (cover != null) {
                Image(
                    provider = ImageProvider(cover),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetInnerRadius(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Enjoy placeholder
                Image(
                    provider = ImageProvider(R.drawable.appwidget_cover_error),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }

    fun loadData(list: List<UpdatesView>? = null) {
        coroutineScope.launchIO {
            // Don't show anything when lock is active
            if (preferences.useAuthenticator().get()) {
                updateAll(app)
                return@launchIO
            }

            val manager = GlanceAppWidgetManager(app)
            val ids = manager.getGlanceIds(this@MangaUpdatesGridGlanceWidget::class.java)
            if (ids.isEmpty()) return@launchIO

            val processList = list
                ?: Injekt.get<MangaDatabaseHandler>()
                    .awaitList { updatesViewQueries.updates(after = DateLimit.timeInMillis) }
            val (rowCount, columnCount) = ids
                .flatMap { manager.getAppWidgetSizes(it) }
                .maxBy { it.height.value * it.width.value }
                .calculateRowAndColumnCount()

            data = prepareList(processList, rowCount * columnCount)
            ids.forEach { update(app, it) }
        }
    }

    private fun prepareList(processList: List<UpdatesView>, take: Int): List<Pair<Long, Bitmap?>> {
        // Resize to cover size
        val widthPx = CoverWidth.value.toInt().dpToPx
        val heightPx = CoverHeight.value.toInt().dpToPx
        val roundPx = app.resources.getDimension(R.dimen.appwidget_inner_radius)
        return processList
            .distinctBy { it.mangaId }
            .take(take)
            .map { updatesView ->
                val request = ImageRequest.Builder(app)
                    .data(
                        MangaCover(
                            mangaId = updatesView.mangaId,
                            sourceId = updatesView.source,
                            isMangaFavorite = updatesView.favorite,
                            url = updatesView.thumbnailUrl,
                            lastModified = updatesView.coverLastModified,
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
                Pair(updatesView.mangaId, app.imageLoader.executeBlocking(request).drawable?.toBitmap())
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