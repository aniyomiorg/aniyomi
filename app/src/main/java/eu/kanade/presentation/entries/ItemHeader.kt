package eu.kanade.presentation.entries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R

@Composable
fun ItemHeader(
    enabled: Boolean,
    itemCount: Int?,
    onClick: () -> Unit,
    isManga: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (itemCount == null) {
                val count = if (isManga) R.string.chapters else R.string.episodes
                stringResource(count)
            } else {
                val pluralCount = if (isManga) R.plurals.manga_num_chapters else R.plurals.anime_num_episodes
                pluralStringResource(id = pluralCount, count = itemCount, itemCount)
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
