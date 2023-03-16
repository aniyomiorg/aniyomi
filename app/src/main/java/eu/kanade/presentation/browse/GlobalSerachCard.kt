package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.entries.EntryCover
import eu.kanade.presentation.components.CommonEntryItemDefaults
import eu.kanade.presentation.components.EntryComfortableGridItem

@Composable
fun GlobalSearchCard(
    title: String,
    cover: EntryCover,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(128.dp)) {
        EntryComfortableGridItem(
            title = title,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadge(enabled = isFavorite)
            },
            coverAlpha = if (isFavorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}
