package eu.kanade.tachiyomi.ui.browse.anime.source.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.SuggestionChip
import eu.kanade.presentation.components.SuggestionChipDefaults
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.source.anime.model.EXHSavedSearch
import tachiyomi.presentation.core.components.SettingsItemsPaddings

@Composable
fun SavedSearchItem(
    savedSearches: ImmutableList<EXHSavedSearch>,
    onSavedSearch: (EXHSavedSearch) -> Unit,
    onSavedSearchPress: (EXHSavedSearch) -> Unit,
    // KMK -->
    onSavedSearchPressDesc: String,
    // KMK <--
) {
    if (savedSearches.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
    ) {
        Text(
            // KMK -->
            text = onSavedSearchPressDesc,
            // KMK <--
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(
            modifier = Modifier.padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            savedSearches.forEach {
                SuggestionChip(
                    onClick = { onSavedSearch(it) },
                    onLongClick = { onSavedSearchPress(it) },
                    label = {
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
    }
}
