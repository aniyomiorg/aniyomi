package eu.kanade.presentation.entries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.SecondaryItemAlpha
import tachiyomi.presentation.core.i18n.localize
import tachiyomi.presentation.core.i18n.localizePlural

@Composable
fun ItemHeader(
    enabled: Boolean,
    itemCount: Int?,
    missingItemsCount: Int,
    onClick: () -> Unit,
    isManga: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (itemCount == null) {
                val count = if (isManga) MR.strings.chapters else MR.strings.episodes
                localize(count)
            } else {
                val pluralCount = if (isManga) MR.plurals.manga_num_chapters else MR.plurals.anime_num_episodes
                localizePlural(pluralCount, count = itemCount, itemCount)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        MissingItemsWarning(missingItemsCount)
    }
}

@Composable
private fun MissingItemsWarning(count: Int) {
    if (count == 0) {
        return
    }

    Text(
        text = localizePlural(MR.plurals.missing_items, count = count, count),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error.copy(alpha = SecondaryItemAlpha),
    )
}
