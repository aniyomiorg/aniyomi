package eu.kanade.presentation.entries.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun MissingItemCountListItem(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            )
            .secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = pluralStringResource(AYMR.plurals.missing_items, count = count, count),
            style = MaterialTheme.typography.labelMedium,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    TachiyomiPreviewTheme {
        Surface {
            MissingItemCountListItem(count = 42)
        }
    }
}
