package eu.kanade.presentation.animebrowse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.presentation.browse.components.BrowseSourceLoadingItem
import eu.kanade.presentation.components.Badge
import eu.kanade.presentation.components.CommonMangaItemDefaults
import eu.kanade.presentation.components.LazyColumn
import eu.kanade.presentation.components.MangaListItem
import eu.kanade.presentation.util.plus
import eu.kanade.tachiyomi.R

@Composable
fun BrowseAnimeSourceList(
    animeList: LazyPagingItems<Anime>,
    getAnimeState: @Composable ((Anime) -> State<Anime>),
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

        items(animeList) { initialAnime ->
            initialAnime ?: return@items
            val anime by getAnimeState(initialAnime)
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
fun BrowseAnimeSourceListItem(
    anime: Anime,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    MangaListItem(
        title = anime.title,
        coverData = AnimeCover(
            animeId = anime.id,
            sourceId = anime.source,
            isAnimeFavorite = anime.favorite,
            url = anime.thumbnailUrl,
            lastModified = anime.coverLastModified,
        ),
        coverAlpha = if (anime.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            if (anime.favorite) {
                Badge(text = stringResource(R.string.in_library))
            }
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
