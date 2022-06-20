package eu.kanade.presentation.anime.components

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
import eu.kanade.domain.anime.model.Anime
import eu.kanade.presentation.components.MangaCover
import eu.kanade.presentation.util.horizontalPadding

@Composable
fun BaseAnimeListItem(
    modifier: Modifier = Modifier,
    anime: Anime,
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
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cover()
        content()
        actions()
    }
}

private val defaultCover: @Composable RowScope.(Anime, () -> Unit) -> Unit = { anime, onClick ->
    MangaCover.Square(
        modifier = Modifier
            .padding(vertical = 8.dp)
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
                .padding(start = horizontalPadding),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
