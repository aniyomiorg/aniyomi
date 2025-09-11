package eu.kanade.presentation.entries.components

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
import eu.kanade.tachiyomi.animesource.model.FetchType
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ItemHeader(
    enabled: Boolean,
    itemCount: Int?,
    missingItemsCount: Int,
    onClick: () -> Unit,
    isManga: Boolean,
    modifier: Modifier = Modifier,
    fetchType: FetchType = FetchType.Episodes,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        Text(
            text = if (itemCount == null) {
                val count = if (isManga) MR.strings.chapters else AYMR.strings.episodes
                stringResource(count)
            } else {
                val pluralCount = if (isManga) {
                    MR.plurals.manga_num_chapters
                } else {
                    when (fetchType) {
                        FetchType.Seasons -> AYMR.plurals.anime_num_seasons
                        FetchType.Episodes -> AYMR.plurals.anime_num_episodes
                    }
                }
                pluralStringResource(pluralCount, count = itemCount, itemCount)
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
        text = pluralStringResource(AYMR.plurals.missing_items, count = count, count),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error.copy(alpha = SECONDARY_ALPHA),
    )
}
