package eu.kanade.presentation.entries.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entries.components.ItemCover
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.presentation.core.components.material.padding

@Composable
fun BaseMangaListItem(
    manga: Manga,
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onClickCover: () -> Unit = onClickItem,
    cover: @Composable RowScope.() -> Unit = { defaultCover(manga, onClickCover) },
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit = { defaultContent(manga) },
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClickItem)
            .height(76.dp)
            .padding(horizontal = MaterialTheme.padding.medium, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cover()
        content()
        actions()
    }
}

private val defaultCover: @Composable RowScope.(Manga, () -> Unit) -> Unit = { manga, onClick ->
    ItemCover.Book(
        modifier = Modifier
            .fillMaxHeight(),
        data = manga,
        onClick = onClick,
    )
}

private val defaultContent: @Composable RowScope.(Manga) -> Unit = {
    Box(modifier = Modifier.weight(1f)) {
        Text(
            text = it.title,
            modifier = Modifier
                .padding(start = MaterialTheme.padding.medium),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
