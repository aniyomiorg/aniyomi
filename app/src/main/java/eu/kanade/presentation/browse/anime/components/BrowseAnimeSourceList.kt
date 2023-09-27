package eu.kanade.presentation.browse.anime.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import eu.kanade.presentation.browse.InLibraryBadge
import eu.kanade.presentation.browse.manga.components.BrowseSourceLoadingItem
import eu.kanade.presentation.library.CommonEntryItemDefaults
import eu.kanade.presentation.library.EntryListItem
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseAnimeSourceList(
    animeList: LazyPagingItems<StateFlow<Anime>>,
    contentPadding: PaddingValues,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (animeList.loadState.prepend is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }

        items(animeList) { animeflow ->
            animeflow ?: return@items
            val anime by animeflow.collectAsState()
            BrowseAnimeSourceListItem(
                anime = anime,
                onClick = { onAnimeClick(anime) },
                onLongClick = { onAnimeLongClick(anime) },
            )
        }

        item {
            if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseAnimeSourceListItem(
    anime: Anime,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    EntryListItem(
        title = anime.title,
        coverData = AnimeCover(
            animeId = anime.id,
            sourceId = anime.source,
            isAnimeFavorite = anime.favorite,
            url = anime.thumbnailUrl,
            lastModified = anime.coverLastModified,
        ),
        coverAlpha = if (anime.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = anime.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
