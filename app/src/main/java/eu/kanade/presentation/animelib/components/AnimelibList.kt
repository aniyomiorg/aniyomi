package eu.kanade.presentation.animelib.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.components.MangaListItem
import eu.kanade.presentation.library.components.DownloadsBadge
import eu.kanade.presentation.library.components.GlobalSearchItem
import eu.kanade.presentation.library.components.LanguageBadge
import eu.kanade.presentation.library.components.UnreadBadge
import eu.kanade.presentation.util.plus
import eu.kanade.tachiyomi.ui.animelib.AnimelibItem

@Composable
fun AnimelibList(
    items: List<AnimelibItem>,
    contentPadding: PaddingValues,
    selection: List<AnimelibAnime>,
    onClick: (AnimelibAnime) -> Unit,
    onLongClick: (AnimelibAnime) -> Unit,
    onClickContinueWatching: ((AnimelibAnime) -> Unit)?,
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
            contentType = { "animelib_list_item" },
        ) { animelibItem ->
            val anime = animelibItem.animelibAnime.anime
            MangaListItem(
                isSelected = selection.fastAny { it.id == animelibItem.animelibAnime.id },
                title = anime.title,
                coverData = AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    url = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                ),
                badge = {
                    DownloadsBadge(count = animelibItem.downloadCount.toInt())
                    UnreadBadge(count = animelibItem.unseenCount.toInt())
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
