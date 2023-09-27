package eu.kanade.presentation.browse.manga.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import eu.kanade.presentation.browse.InLibraryBadge
import eu.kanade.presentation.library.CommonEntryItemDefaults
import eu.kanade.presentation.library.EntryListItem
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseMangaSourceList(
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (mangaList.loadState.prepend is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }

        items(mangaList) { mangaflow ->
            mangaflow ?: return@items
            val manga by mangaflow.collectAsState()
            BrowseMangaSourceListItem(
                manga = manga,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
            )
        }

        item {
            if (mangaList.loadState.refresh is LoadState.Loading || mangaList.loadState.append is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseMangaSourceListItem(
    manga: Manga,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    EntryListItem(
        title = manga.title,
        coverData = MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            url = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        ),
        coverAlpha = if (manga.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = manga.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
