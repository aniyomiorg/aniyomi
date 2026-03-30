package eu.kanade.presentation.library.manga

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
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.presentation.core.components.material.PullRefresh
import kotlin.time.Duration.Companion.seconds

@Composable
fun MangaLibraryContent(
    categories: List<Category>,
    searchQuery: String?,
    selection: List<LibraryManga>,
    contentPadding: PaddingValues,
    currentPage: () -> Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onMangaClicked: (Long) -> Unit,
    onContinueReadingClicked: ((LibraryManga) -> Unit)?,
    onToggleSelection: (LibraryManga) -> Unit,
    onToggleRangeSelection: (LibraryManga) -> Unit,
    onRefresh: (Category?) -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    onCurrentCategoryChanged: (Long?) -> Unit,
    onEnterCategory: ((Long) -> Unit)? = null,
    getNumberOfMangaForCategory: (Category) -> Int?,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getLibraryForPage: (Int) -> List<MangaLibraryItem>,
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
            val categoryId = if (currentCategory != null) {
                currentCategory.id
            } else {
                categories.find { it.id == 0L }?.id
            }
            onCurrentCategoryChanged(categoryId)
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
        val onClickMangaInternal = { manga: LibraryManga ->
            if (notSelectionMode) {
                onMangaClicked(manga.manga.id)
            } else {
                onToggleSelection(manga)
            }
        }

        val childCategories = categories.filter { it.parentId == currentCategory?.id }

        val entries = if (currentCategory != null) {
            val pageIndex = categories.indexOf(currentCategory)
            if (pageIndex != -1) getLibraryForPage(pageIndex) else emptyList()
        } else {
            val defaultCategory = categories.find { it.id == 0L }
            if (defaultCategory != null && defaultCategory.parentId == null) {
                val pageIndex = categories.indexOf(defaultCategory)
                if (pageIndex != -1) getLibraryForPage(pageIndex) else emptyList()
            } else {
                emptyList()
            }
        }

        val gridItems = remember(childCategories, entries) {
            val groups = childCategories.map { cat ->
                val pageIndex = categories.indexOf(cat)
                val itemsForCat = if (pageIndex != -1) getLibraryForPage(pageIndex) else emptyList()
                val firstManga = itemsForCat.firstOrNull()?.libraryManga
                val cover = firstManga?.let { lm ->
                    val it = lm.manga
                    tachiyomi.domain.entries.manga.model.MangaCover(
                        mangaId = it.id,
                        sourceId = it.source,
                        isMangaFavorite = it.favorite,
                        url = it.thumbnailUrl,
                        lastModified = it.coverLastModified,
                    )
                }
                MangaCategoryGridItem.Group(cat, cover)
            }
            val entryItems = entries.map { MangaCategoryGridItem.Entry(it) }
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
            MangaCategoryGridScreen(
                items = gridItems,
                columns = columns.takeIf { it > 0 } ?: 2,
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                selection = selection,
                onGroupClick = { clickedCategory ->
                    navigationStack = navigationStack + clickedCategory
                    onEnterCategory?.invoke(clickedCategory.id)
                },
                onClickManga = onClickMangaInternal,
                onLongClickManga = onToggleRangeSelection,
                onClickContinueReading = onContinueReadingClicked,
            )
        }
    }
}
