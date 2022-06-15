package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.presentation.util.horizontalPadding
import eu.kanade.tachiyomi.util.system.LocaleHelper

@Composable
fun BaseAnimeSourceItem(
    modifier: Modifier = Modifier,
    source: AnimeSource,
    showLanguageInContent: Boolean = true,
    onClickItem: () -> Unit = {},
    onLongClickItem: () -> Unit = {},
    icon: @Composable RowScope.(AnimeSource) -> Unit = defaultIcon,
    action: @Composable RowScope.(AnimeSource) -> Unit = {},
    content: @Composable RowScope.(AnimeSource, Boolean) -> Unit = defaultContent,
) {
    BaseBrowseItem(
        modifier = modifier,
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        icon = { icon.invoke(this, source) },
        action = { action.invoke(this, source) },
        content = { content.invoke(this, source, showLanguageInContent) },
    )
}

private val defaultIcon: @Composable RowScope.(AnimeSource) -> Unit = { source ->
    AnimeSourceIcon(source = source)
}

private val defaultContent: @Composable RowScope.(AnimeSource, Boolean) -> Unit = { source, showLanguageInContent ->
    Column(
        modifier = Modifier
            .padding(horizontal = horizontalPadding)
            .weight(1f),
    ) {
        Text(
            text = source.name.ifBlank { source.id.toString() },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (showLanguageInContent) {
            Text(
                text = LocaleHelper.getDisplayName(source.lang),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
