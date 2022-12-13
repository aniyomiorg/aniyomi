package eu.kanade.presentation.animebrowse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.presentation.browse.components.BrowseSourceLoadingItem
import eu.kanade.presentation.components.Badge
import eu.kanade.presentation.components.CommonMangaItemDefaults
import eu.kanade.presentation.components.MangaCompactGridItem
import eu.kanade.presentation.util.plus
import eu.kanade.tachiyomi.R

@Composable
fun BrowseAnimeSourceCompactGrid(
    animeList: LazyPagingItems<Anime>,
    getAnimeState: @Composable ((Anime) -> State<Anime>),
    columns: GridCells,
    contentPadding: PaddingValues,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
    ) {
        if (animeList.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(animeList.itemCount) { index ->
            val initialAnime = animeList[index] ?: return@items
            val anime by getAnimeState(initialAnime)
            BrowseAnimeSourceCompactGridItem(
                anime = anime,
                onClick = { onAnimeClick(anime) },
                onLongClick = { onAnimeLongClick(anime) },
            )
        }

        if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseAnimeSourceCompactGridItem(
    anime: Anime,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    MangaCompactGridItem(
        title = anime.title,
        coverData = AnimeCover(
            animeId = anime.id,
            sourceId = anime.source,
            isAnimeFavorite = anime.favorite,
            url = anime.thumbnailUrl,
            lastModified = anime.coverLastModified,
        ),
        coverAlpha = if (anime.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = {
            if (anime.favorite) {
                Badge(text = stringResource(R.string.in_library))
            }
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
