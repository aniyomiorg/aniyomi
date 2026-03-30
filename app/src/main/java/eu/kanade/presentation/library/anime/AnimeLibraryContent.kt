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
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.presentation.core.components.material.PullRefresh
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnimeLibraryContent(
    categories: List<Category>,
    selection: List<LibraryAnime>,
    contentPadding: PaddingValues,
    currentPage: () -> Int,
    onAnimeClicked: (Long) -> Unit,
    onContinueWatchingClicked: ((LibraryAnime) -> Unit)?,
    onToggleSelection: (LibraryAnime) -> Unit,
    onToggleRangeSelection: (LibraryAnime) -> Unit,
    onRefresh: (Category?) -> Boolean,
    onCurrentCategoryChanged: (Long?) -> Unit,
    onEnterCategory: (Long) -> Unit,
    columns: Int,
    getLibraryForPage: (Int) -> List<AnimeLibraryItem>,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                IconButton(onClick = { navigationStack = navigationStack.dropLast(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
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
                val firstAnime = itemsForCat.firstOrNull()?.libraryAnime
                val cover = firstAnime?.let { la ->
                    val a = la.anime
                    AnimeCover(
                        animeId = a.id,
                        sourceId = a.source,
                        isAnimeFavorite = a.favorite,
                        url = a.thumbnailUrl,
                        lastModified = a.coverLastModified,
                    )
                }
                AnimeCategoryGridItem.Group(cat, cover)
            }
            val entryItems = entries.map { AnimeCategoryGridItem.Entry(it) }
            groups + entryItems
        }

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
                columns = columns.coerceAtLeast(1),
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding(),
                    start = 12.dp,
                    end = 12.dp,
                ),
                selection = selection,
                onGroupClick = { clickedCategory ->
                    navigationStack = navigationStack + clickedCategory
                    onEnterCategory(clickedCategory.id)
                },
                onClickAnime = onClickAnimeInternal,
                onLongClickAnime = onToggleRangeSelection,
                onClickContinueWatching = onContinueWatchingClicked,
            )
        }
    }
}
