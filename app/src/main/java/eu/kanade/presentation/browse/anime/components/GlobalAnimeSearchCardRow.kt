package eu.kanade.presentation.browse.anime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import eu.kanade.presentation.browse.GlobalSearchCard
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.presentation.core.components.material.padding

@Composable
fun GlobalAnimeSearchCardRow(
    titles: List<Anime>,
    getAnime: @Composable (Anime) -> State<Anime>,
    onClick: (Anime) -> Unit,
    onLongClick: (Anime) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.tiny),
    ) {
        items(titles) {
            val title by getAnime(it)
            GlobalSearchCard(
                title = title.title,
                cover = title.asAnimeCover(),
                isFavorite = title.favorite,
                onClick = { onClick(title) },
                onLongClick = { onLongClick(title) },
            )
        }
    }
}
