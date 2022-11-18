package eu.kanade.presentation.animelib.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.Badge
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animelib.AnimelibItem

@Composable
fun DownloadsBadge(
    enabled: Boolean,
    item: AnimelibItem,
) {
    if (enabled && item.downloadCount > 0) {
        Badge(
            text = "${item.downloadCount}",
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Composable
fun UnseenBadge(
    enabled: Boolean,
    item: AnimelibItem,
) {
    if (enabled && item.unseenCount > 0) {
        Badge(text = "${item.unseenCount}")
    }
}

@Composable
fun LanguageBadge(
    showLanguage: Boolean,
    showLocal: Boolean,
    item: AnimelibItem,
) {
    if (showLocal && item.isLocal) {
        Badge(
            text = stringResource(R.string.local_source_badge),
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary,
        )
    } else if (showLanguage && item.sourceLanguage.isNotEmpty()) {
        Badge(
            text = item.sourceLanguage.uppercase(),
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary,
        )
    }
}
