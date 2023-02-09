package eu.kanade.presentation.animelib.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.presentation.components.MangaComfortableGridItem
import eu.kanade.presentation.library.components.DownloadsBadge
import eu.kanade.presentation.library.components.LanguageBadge
import eu.kanade.presentation.library.components.LazyLibraryGrid
import eu.kanade.presentation.library.components.UnreadBadge
import eu.kanade.presentation.library.components.globalSearchItem
import eu.kanade.tachiyomi.ui.animelib.AnimelibItem

@Composable
fun AnimelibComfortableGrid(
    items: List<AnimelibItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: List<AnimelibAnime>,
    onClick: (AnimelibAnime) -> Unit,
    onLongClick: (AnimelibAnime) -> Unit,
    onClickContinueWatching: ((AnimelibAnime) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    LazyLibraryGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = contentPadding,
    ) {
        globalSearchItem(searchQuery, onGlobalSearchClicked)

        items(
            items = items,
            contentType = { "animelib_comfortable_grid_item" },
        ) { animelibItem ->
            val anime = animelibItem.animelibAnime.anime
            MangaComfortableGridItem(
                isSelected = selection.fastAny { it.id == animelibItem.animelibAnime.id },
                title = anime.title,
                coverData = AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    url = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                ),
                coverBadgeStart = {
                    DownloadsBadge(count = animelibItem.downloadCount.toInt())
                    UnreadBadge(count = animelibItem.unseenCount.toInt())
                },
                coverBadgeEnd = {
                    LanguageBadge(
                        isLocal = animelibItem.isLocal,
                        sourceLanguage = animelibItem.sourceLanguage,
                    )
                },
                onLongClick = { onLongClick(animelibItem.animelibAnime) },
                onClick = { onClick(animelibItem.animelibAnime) },
                onClickContinueReading = if (onClickContinueWatching != null) {
                    { onClickContinueWatching(animelibItem.animelibAnime) }
                } else {
                    null
                },
            )
        }
    }
}
