package eu.kanade.presentation.anime.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import eu.kanade.domain.anime.model.Anime
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.ChapterHeader
import kotlin.math.roundToInt

@Composable
fun AnimeTopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    author: String?,
    artist: String?,
    description: String?,
    tagsProvider: () -> List<String>?,
    coverDataProvider: () -> Anime,
    sourceName: String,
    isStubSource: Boolean,
    favorite: Boolean,
    status: Long,
    trackingCount: Int,
    episodeCount: Int?,
    episodeFiltered: Boolean,
    incognitoMode: Boolean,
    downloadedOnlyMode: Boolean,
    fromSource: Boolean,
    onBackClicked: () -> Unit,
    onCoverClick: () -> Unit,
    onTagClicked: (String) -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onFilterButtonClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    doGlobalSearch: (query: String, global: Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onSmallAppBarHeightChanged: (Int) -> Unit,
) {
    val scrollPercentageProvider = { scrollBehavior?.scrollFraction?.coerceIn(0f, 1f) ?: 0f }
    val inverseScrollPercentageProvider = { 1f - scrollPercentageProvider() }

    Layout(
        modifier = modifier,
        content = {
            val (smallHeightPx, onSmallHeightPxChanged) = remember { mutableStateOf(0) }
            Column(modifier = Modifier.layoutId("animeInfo")) {
                AnimeInfoHeader(
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    appBarPadding = with(LocalDensity.current) { smallHeightPx.toDp() },
                    title = title,
                    author = author,
                    artist = artist,
                    description = description,
                    tagsProvider = tagsProvider,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    coverDataProvider = coverDataProvider,
                    favorite = favorite,
                    status = status,
                    trackingCount = trackingCount,
                    fromSource = fromSource,
                    onAddToLibraryClicked = onAddToLibraryClicked,
                    onWebViewClicked = onWebViewClicked,
                    onTrackingClicked = onTrackingClicked,
                    onTagClicked = onTagClicked,
                    onEditCategory = onEditCategoryClicked,
                    onCoverClick = onCoverClick,
                    doSearch = doGlobalSearch,
                )
                ChapterHeader(
                    chapterCount = episodeCount,
                    isChapterFiltered = episodeFiltered,
                    onFilterButtonClicked = onFilterButtonClicked,
                )
            }

            AnimeSmallAppBar(
                modifier = Modifier
                    .layoutId("topBar")
                    .onSizeChanged {
                        onSmallHeightPxChanged(it.height)
                        onSmallAppBarHeightChanged(it.height)
                    },
                title = title,
                titleAlphaProvider = { if (actionModeCounter == 0) scrollPercentageProvider() else 1f },
                incognitoMode = incognitoMode,
                downloadedOnlyMode = downloadedOnlyMode,
                onBackClicked = onBackClicked,
                onShareClicked = onShareClicked,
                onDownloadClicked = onDownloadClicked,
                onEditCategoryClicked = onEditCategoryClicked,
                onMigrateClicked = onMigrateClicked,
                actionModeCounter = actionModeCounter,
                onSelectAll = onSelectAll,
                onInvertSelection = onInvertSelection,
            )
        },
    ) { measurables, constraints ->
        val animeInfoPlaceable = measurables
            .first { it.layoutId == "animeInfo" }
            .measure(constraints.copy(maxHeight = Constraints.Infinity))
        val topBarPlaceable = measurables
            .first { it.layoutId == "topBar" }
            .measure(constraints)
        val animeInfoHeight = animeInfoPlaceable.height
        val topBarHeight = topBarPlaceable.height
        val animeInfoSansTopBarHeightPx = animeInfoHeight - topBarHeight
        val layoutHeight = topBarHeight +
            (animeInfoSansTopBarHeightPx * inverseScrollPercentageProvider()).roundToInt()

        layout(constraints.maxWidth, layoutHeight) {
            val animeInfoY = (-animeInfoSansTopBarHeightPx * scrollPercentageProvider()).roundToInt()
            animeInfoPlaceable.place(0, animeInfoY)
            topBarPlaceable.place(0, 0)

            // Update offset limit
            val offsetLimit = -animeInfoSansTopBarHeightPx.toFloat()
            if (scrollBehavior?.state?.offsetLimit != offsetLimit) {
                scrollBehavior?.state?.offsetLimit = offsetLimit
            }
        }
    }
}
