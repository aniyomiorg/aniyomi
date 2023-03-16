package eu.kanade.presentation.library.manga

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.entries.manga.model.MangaCover
import eu.kanade.domain.library.manga.LibraryManga
import eu.kanade.presentation.animelib.components.DownloadsBadge
import eu.kanade.presentation.animelib.components.GlobalSearchItem
import eu.kanade.presentation.animelib.components.LanguageBadge
import eu.kanade.presentation.animelib.components.UnviewedBadge
import eu.kanade.presentation.components.EntryListItem
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.util.plus
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryItem

@Composable
fun MangaLibraryList(
    items: List<MangaLibraryItem>,
    contentPadding: PaddingValues,
    selection: List<LibraryManga>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
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
            contentType = { "manga_library_list_item" },
        ) { libraryItem ->
            val manga = libraryItem.libraryManga.manga
            EntryListItem(
                isSelected = selection.fastAny { it.id == libraryItem.libraryManga.id },
                title = manga.title,
                coverData = MangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified,
                ),
                badge = {
                    DownloadsBadge(count = libraryItem.downloadCount.toInt())
                    UnviewedBadge(count = libraryItem.unreadCount.toInt())
                    LanguageBadge(
                        isLocal = libraryItem.isLocal,
                        sourceLanguage = libraryItem.sourceLanguage,
                    )
                },
                onLongClick = { onLongClick(libraryItem.libraryManga) },
                onClick = { onClick(libraryItem.libraryManga) },
                onClickContinueViewing = if (onClickContinueReading != null) {
                    { onClickContinueReading(libraryItem.libraryManga) }
                } else {
                    null
                },
            )
        }
    }
}
