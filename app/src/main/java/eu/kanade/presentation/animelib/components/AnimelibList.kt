package eu.kanade.presentation.animelib.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.components.MangaListItem
import eu.kanade.presentation.util.plus
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animelib.AnimelibItem

@Composable
fun AnimelibList(
    items: List<AnimelibItem>,
    showDownloadBadges: Boolean,
    showUnreadBadges: Boolean,
    showLocalBadges: Boolean,
    showLanguageBadges: Boolean,
    showContinueWatchingButton: Boolean,
    contentPadding: PaddingValues,
    selection: List<AnimelibAnime>,
    onClick: (AnimelibAnime) -> Unit,
    onLongClick: (AnimelibAnime) -> Unit,
    onClickContinueWatching: (AnimelibAnime) -> Unit,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    FastScrollLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (searchQuery.isNullOrEmpty().not()) {
                TextButton(onClick = onGlobalSearchClicked) {
                    Text(
                        text = stringResource(R.string.action_global_search_query, searchQuery!!),
                        modifier = Modifier.zIndex(99f),
                    )
                }
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
                    DownloadsBadge(
                        enabled = showDownloadBadges,
                        item = animelibItem,
                    )
                    UnseenBadge(
                        enabled = showUnreadBadges,
                        item = animelibItem,
                    )
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
