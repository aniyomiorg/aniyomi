package eu.kanade.presentation.library.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.entries.anime.model.AnimeCover
import eu.kanade.domain.library.anime.LibraryAnime
import eu.kanade.presentation.animelib.components.DownloadsBadge
import eu.kanade.presentation.animelib.components.LanguageBadge
import eu.kanade.presentation.animelib.components.UnviewedBadge
import eu.kanade.presentation.components.EntryComfortableGridItem
import eu.kanade.presentation.library.LazyLibraryGrid
import eu.kanade.presentation.library.globalSearchItem
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryItem

@Composable
fun AnimeLibraryComfortableGrid(
    items: List<AnimeLibraryItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: List<LibraryAnime>,
    onClick: (LibraryAnime) -> Unit,
    onLongClick: (LibraryAnime) -> Unit,
    onClickContinueWatching: ((LibraryAnime) -> Unit)?,
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
            contentType = { "anime_library_comfortable_grid_item" },
        ) { libraryItem ->
            val anime = libraryItem.libraryAnime.anime
            EntryComfortableGridItem(
                isSelected = selection.fastAny { it.id == libraryItem.libraryAnime.id },
                title = anime.title,
                coverData = AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    url = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                ),
                coverBadgeStart = {
                    DownloadsBadge(count = libraryItem.downloadCount.toInt())
                    UnviewedBadge(count = libraryItem.unseenCount.toInt())
                },
                coverBadgeEnd = {
                    LanguageBadge(
                        isLocal = libraryItem.isLocal,
                        sourceLanguage = libraryItem.sourceLanguage,
                    )
                },
                onLongClick = { onLongClick(libraryItem.libraryAnime) },
                onClick = { onClick(libraryItem.libraryAnime) },
                onClickContinueViewing = if (onClickContinueWatching != null) {
                    { onClickContinueWatching(libraryItem.libraryAnime) }
                } else {
                    null
                },
            )
        }
    }
}
