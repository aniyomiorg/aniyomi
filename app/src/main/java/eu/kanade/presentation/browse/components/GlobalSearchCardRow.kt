package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.InLibraryBadge
import eu.kanade.presentation.library.components.AnimeComfortableGridItem
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchCardRow(
    titles: List<Anime>,
    getAnime: @Composable (Anime) -> State<Anime>,
    onClick: (Anime) -> Unit,
    onLongClick: (Anime) -> Unit,
) {
    if (titles.isEmpty()) {
        EmptyResultItem()
        return
    }

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(titles) {
            val title by getAnime(it)
            AnimeItem(
                title = title.title,
                cover = title.asAnimeCover(),
                isFavorite = title.favorite,
                onClick = { onClick(title) },
                onLongClick = { onLongClick(title) },
            )
        }
    }
}

@Composable
internal fun AnimeItem(
    title: String,
    cover: AnimeCover,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(96.dp)) {
        AnimeComfortableGridItem(
            title = title,
            titleMaxLines = 3,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadge(enabled = isFavorite)
            },
            coverAlpha = if (isFavorite) CommonAnimeItemDefaults.BrowseFavoriteCoverAlpha else 1f,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@Composable
internal fun EmptyResultItem() {
    Text(
        text = stringResource(MR.strings.no_results_found),
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
    )
}
