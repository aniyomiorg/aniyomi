package eu.kanade.presentation.entries.anime.components

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
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.presentation.core.components.material.padding

@Composable
fun BaseAnimeListItem(
    anime: Anime,
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onClickCover: () -> Unit = onClickItem,
    cover: @Composable RowScope.() -> Unit = { defaultCover(anime, onClickCover) },
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit = { defaultContent(anime) },
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClickItem)
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cover()
        content()
        actions()
    }
}

private val defaultCover: @Composable RowScope.(Anime, () -> Unit) -> Unit = { anime, onClick ->
    ItemCover.Square(
        modifier = Modifier
            .padding(vertical = MaterialTheme.padding.small)
            .fillMaxHeight(),
        data = anime,
        onClick = onClick,
    )
}

private val defaultContent: @Composable RowScope.(Anime) -> Unit = {
    Box(modifier = Modifier.weight(1f)) {
        Text(
            text = it.title,
            modifier = Modifier
                .padding(start = MaterialTheme.padding.medium),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
