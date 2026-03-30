package eu.kanade.presentation.library.anime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.presentation.core.components.material.PullRefresh
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnimeLibraryContent(
    categories: List<Category>,
    searchQuery: String?,
    selection: List<LibraryAnime>,
    contentPadding: PaddingValues,
    currentPage: () -> Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onAnimeClicked: (Long) -> Unit,
    onContinueWatchingClicked: ((LibraryAnime) -> Unit)?,
    onToggleSelection: (LibraryAnime) -> Unit,
    onToggleRangeSelection: (LibraryAnime) -> Unit,
    onRefresh: (Category?) -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    onCurrentCategoryChanged: (Long?) -> Unit,
    onEnterCategory: ((Long) -> Unit)? = null,
    getNumberOfAnimeForCategory: (Category) -> Int?,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getAnimeLibraryForPage: (Int) -> List<AnimeLibraryItem>,
) {
    Column(
        modifier = Modifier.padding(
            top = contentPadding.calculateTopPadding(),
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
        ),
    ) {
        var navigationStack by remember { mutableStateOf(listOf<Category?>(null)) }
        val currentCategory = navigationStack.last()

        LaunchedEffect(currentCategory) {
            onCurrentCategoryChanged(currentCategory?.id)
        }

        val scope = rememberCoroutineScope()
        var isRefreshing by remember(currentCategory) { mutableStateOf(false) }

        if (navigationStack.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navigationStack = navigationStack.dropLast(1) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(text = currentCategory?.name ?: "")
            }
        }

        val notSelectionMode = selection.isEmpty()
        val onClickAnimeInternal = { anime: LibraryAnime ->
            if (notSelectionMode) {
                onAnimeClicked(anime.anime.id)
            } else {
                onToggleSelection(anime)
            }
        }

        val childCategories = categories.filter { it.parentId == currentCategory?.id }

        val entries = if (currentCategory != null) {
            val pageIndex = categories.indexOf(currentCategory)
            if (pageIndex != -1) getAnimeLibraryForPage(pageIndex) else emptyList()
        } else {
            val defaultCategory = categories.find { it.id == 0L }
            if (defaultCategory != null && defaultCategory.parentId == null) {
                val pageIndex = categories.indexOf(defaultCategory)
                if (pageIndex != -1) getAnimeLibraryForPage(pageIndex) else emptyList()
            } else {
                emptyList()
            }
        }

        val gridItems = remember(childCategories, entries) {
            val groups = childCategories.map { cat ->
                val pageIndex = categories.indexOf(cat)
                val itemsForCat = if (pageIndex != -1) getAnimeLibraryForPage(pageIndex) else emptyList()
                val firstAnime = itemsForCat.firstOrNull()?.libraryAnime?.anime
                val cover = firstAnime?.let {
                    tachiyomi.domain.entries.anime.model.AnimeCover(
                        animeId = it.id,
                        sourceId = it.source,
                        isAnimeFavorite = it.favorite,
                        url = it.thumbnailUrl,
                        lastModified = it.coverLastModified,
                    )
                }
                CategoryGridItem.Group(cat, cover)
            }
            val entryItems = entries.map { CategoryGridItem.Entry(it) }
            groups + entryItems
        }

        val columns by remember { getColumnsForOrientation(true) } // Simplified assuming compact grid

        PullRefresh(
            refreshing = isRefreshing,
            onRefresh = {
                val started = onRefresh(currentCategory)
                if (!started) return@PullRefresh
                scope.launch {
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
            enabled = notSelectionMode,
        ) {
            AnimeCategoryGridScreen(
                items = gridItems,
                columns = columns.takeIf { it > 0 } ?: 2,
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                selection = selection,
                onGroupClick = { clickedCategory ->
                    navigationStack = navigationStack + clickedCategory
                    onEnterCategory?.invoke(clickedCategory.id)
                },
                onClickAnime = onClickAnimeInternal,
                onLongClickAnime = onToggleRangeSelection,
                onClickContinueWatching = onContinueWatchingClicked,
            )
        }
    }
}
