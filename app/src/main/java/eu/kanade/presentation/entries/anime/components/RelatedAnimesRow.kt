package eu.kanade.presentation.entries.anime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.AnimeItem
import eu.kanade.presentation.browse.components.EmptyResultItem
import eu.kanade.tachiyomi.ui.entries.anime.RelatedAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.presentation.core.components.material.padding

@Composable
fun RelatedAnimesRow(
    relatedAnimes: List<RelatedAnime>?,
    getAnimeState: @Composable (Anime) -> State<Anime>,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
) {
    when {
        relatedAnimes == null -> {
            GlobalSearchLoadingResultItem()
        }

        relatedAnimes.isNotEmpty() -> {
            RelatedAnimeCardRow(
                relatedAnimes = relatedAnimes,
                getManga = { getAnimeState(it) },
                onMangaClick = onAnimeClick,
                onMangaLongClick = onAnimeLongClick,
            )
        }

        else -> {
            EmptyResultItem()
        }
    }
}

@Composable
fun RelatedAnimeCardRow(
    relatedAnimes: List<RelatedAnime>,
    getManga: @Composable (Anime) -> State<Anime>,
    onMangaClick: (Anime) -> Unit,
    onMangaLongClick: (Anime) -> Unit,
) {
    val mangas = relatedAnimes.filterIsInstance<RelatedAnime.Success>().map { it.mangaList }.flatten()
    val loading = relatedAnimes.filterIsInstance<RelatedAnime.Loading>().firstOrNull()

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(mangas, key = { "related-row-${it.url.hashCode()}" }) {
            val manga by getManga(it)
            AnimeItem(
                title = manga.title,
                cover = manga.asAnimeCover(),
                isFavorite = manga.favorite,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
                isSelected = false,
            )
        }
        if (loading != null) {
            item {
                RelatedAnimeLoadingItem()
            }
        }
    }
}

@Composable
fun RelatedAnimeLoadingItem() {
    Box(
        modifier = Modifier
            .width(96.dp)
            .aspectRatio(AnimeCover.Book.ratio)
            .padding(vertical = MaterialTheme.padding.medium),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center),
            strokeWidth = 2.dp,
        )
    }
}
