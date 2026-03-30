package eu.kanade.presentation.library.anime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.library.components.EntryCompactGridItem
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryItem
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.library.anime.LibraryAnime

sealed interface CategoryGridItem {
    data class Group(val category: Category, val coverAnime: AnimeCover?) : CategoryGridItem
    data class Entry(val libraryItem: AnimeLibraryItem) : CategoryGridItem
}

@Composable
fun AnimeCategoryGridScreen(
    items: List<CategoryGridItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: List<LibraryAnime>,
    onGroupClick: (Category) -> Unit,
    onClickAnime: (LibraryAnime) -> Unit,
    onLongClickAnime: (LibraryAnime) -> Unit,
    onClickContinueWatching: ((LibraryAnime) -> Unit)?,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = items,
            key = { it.hashCode() },
        ) { item ->
            when (item) {
                is CategoryGridItem.Group -> {
                    Column(
                        modifier = Modifier.clickable { onGroupClick(item.category) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (item.coverAnime != null) {
                            EntryCompactGridItem(
                                isSelected = false,
                                title = item.category.name,
                                coverData = item.coverAnime,
                                coverBadgeStart = {},
                                coverBadgeEnd = {},
                                onLongClick = { },
                                onClick = { onGroupClick(item.category) },
                                onClickContinueViewing = null,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Text(
                                text = item.category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                            )
                        }
                    }
                }
                is CategoryGridItem.Entry -> {
                    val anime = item.libraryItem.libraryAnime.anime
                    EntryCompactGridItem(
                        isSelected = selection.any { it.id == item.libraryItem.libraryAnime.id },
                        title = anime.title,
                        coverData = AnimeCover(
                            animeId = anime.id,
                            sourceId = anime.source,
                            isAnimeFavorite = anime.favorite,
                            url = anime.thumbnailUrl,
                            lastModified = anime.coverLastModified,
                        ),
                        coverBadgeStart = {},
                        coverBadgeEnd = {},
                        onLongClick = { onLongClickAnime(item.libraryItem.libraryAnime) },
                        onClick = { onClickAnime(item.libraryItem.libraryAnime) },
                        onClickContinueViewing = if (onClickContinueWatching != null &&
                            item.libraryItem.unseenCount > 0
                        ) {
                            { onClickContinueWatching(item.libraryItem.libraryAnime) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
