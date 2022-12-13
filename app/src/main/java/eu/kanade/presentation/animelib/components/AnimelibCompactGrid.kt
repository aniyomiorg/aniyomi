package eu.kanade.presentation.animelib.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.presentation.components.MangaCompactGridItem
import eu.kanade.presentation.library.components.LazyLibraryGrid
import eu.kanade.presentation.library.components.globalSearchItem
import eu.kanade.tachiyomi.ui.animelib.AnimelibItem

@Composable
fun AnimelibCompactGrid(
    items: List<AnimelibItem>,
    showTitle: Boolean,
    showDownloadBadges: Boolean,
    showUnreadBadges: Boolean,
    showLocalBadges: Boolean,
    showLanguageBadges: Boolean,
    showContinueWatchingButton: Boolean,
    columns: Int,
    contentPadding: PaddingValues,
    selection: List<AnimelibAnime>,
    onClick: (AnimelibAnime) -> Unit,
    onLongClick: (AnimelibAnime) -> Unit,
    onClickContinueWatching: (AnimelibAnime) -> Unit,
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
            contentType = { "animelib_compact_grid_item" },
        ) { animelibItem ->
            val anime = animelibItem.animelibAnime.anime
            MangaCompactGridItem(
                isSelected = selection.fastAny { it.id == animelibItem.animelibAnime.id },
                title = anime.title.takeIf { showTitle },
                coverData = AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    url = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                ),
                coverBadgeStart = {
                    DownloadsBadge(
                        enabled = showDownloadBadges,
                        item = animelibItem,
                    )
                    UnseenBadge(
                        enabled = showUnreadBadges,
                        item = animelibItem,
                    )
                },
                coverBadgeEnd = {
                    LanguageBadge(
                        showLanguage = showLanguageBadges,
                        showLocal = showLocalBadges,
                        item = animelibItem,
                    )
                },
                showContinueReadingButton = showContinueWatchingButton,
                onLongClick = { onLongClick(animelibItem.animelibAnime) },
                onClick = { onClick(animelibItem.animelibAnime) },
                onClickContinueReading = { onClickContinueWatching(animelibItem.animelibAnime) },
            )
        }
    }
}
