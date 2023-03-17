package eu.kanade.presentation.library.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.entries.anime.model.AnimeCover
import eu.kanade.domain.library.anime.LibraryAnime
import eu.kanade.presentation.animelib.components.DownloadsBadge
import eu.kanade.presentation.animelib.components.GlobalSearchItem
import eu.kanade.presentation.animelib.components.LanguageBadge
import eu.kanade.presentation.animelib.components.UnviewedBadge
import eu.kanade.presentation.components.EntryListItem
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.util.plus
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryItem

@Composable
fun AnimeLibraryList(
    items: List<AnimeLibraryItem>,
    contentPadding: PaddingValues,
    selection: List<LibraryAnime>,
    onClick: (LibraryAnime) -> Unit,
    onLongClick: (LibraryAnime) -> Unit,
    onClickContinueWatching: ((LibraryAnime) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    FastScrollLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (!searchQuery.isNullOrEmpty()) {
                GlobalSearchItem(
                    modifier = Modifier.fillMaxWidth(),
                    searchQuery = searchQuery,
                    onClick = onGlobalSearchClicked,
                )
            }
        }

        items(
            items = items,
            contentType = { "anime_library_list_item" },
        ) { libraryItem ->
            val anime = libraryItem.libraryAnime.anime
            EntryListItem(
                isSelected = selection.fastAny { it.id == libraryItem.libraryAnime.id },
                title = anime.title,
                coverData = AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    url = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                ),
                badge = {
                    DownloadsBadge(count = libraryItem.downloadCount.toInt())
                    UnviewedBadge(count = libraryItem.unseenCount.toInt())
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
