package eu.kanade.presentation.animebrowse.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.presentation.browse.InLibraryBadge
import eu.kanade.presentation.components.CommonMangaItemDefaults
import eu.kanade.presentation.components.MangaComfortableGridItem

@Composable
fun GlobalAnimeSearchCard(
    title: String,
    cover: AnimeCover,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(128.dp)) {
        MangaComfortableGridItem(
            title = title,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadge(enabled = isFavorite)
            },
            coverAlpha = if (isFavorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}
