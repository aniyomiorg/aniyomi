package eu.kanade.presentation.library.manga

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
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryItem
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.library.manga.LibraryManga

sealed interface MangaCategoryGridItem {
    data class Group(val category: Category, val coverManga: MangaCover?) : MangaCategoryGridItem
    data class Entry(val libraryItem: MangaLibraryItem) : MangaCategoryGridItem
}

@Composable
fun MangaCategoryGridScreen(
    items: List<MangaCategoryGridItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: List<LibraryManga>,
    onGroupClick: (Category) -> Unit,
    onClickManga: (LibraryManga) -> Unit,
    onLongClickManga: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
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
                is MangaCategoryGridItem.Group -> {
                    Column(
                        modifier = Modifier.clickable { onGroupClick(item.category) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (item.coverManga != null) {
                            EntryCompactGridItem(
                                isSelected = false,
                                title = item.category.name,
                                coverData = item.coverManga,
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
                is MangaCategoryGridItem.Entry -> {
                    val manga = item.libraryItem.libraryManga.manga
                    EntryCompactGridItem(
                        isSelected = selection.any { it.id == item.libraryItem.libraryManga.id },
                        title = manga.title,
                        coverData = MangaCover(
                            mangaId = manga.id,
                            sourceId = manga.source,
                            isMangaFavorite = manga.favorite,
                            url = manga.thumbnailUrl,
                            lastModified = manga.coverLastModified,
                        ),
                        coverBadgeStart = {},
                        coverBadgeEnd = {},
                        onLongClick = { onLongClickManga(item.libraryItem.libraryManga) },
                        onClick = { onClickManga(item.libraryItem.libraryManga) },
                        onClickContinueViewing = if (onClickContinueReading != null &&
                            item.libraryItem.unreadCount > 0
                        ) {
                            { onClickContinueReading(item.libraryItem.libraryManga) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
