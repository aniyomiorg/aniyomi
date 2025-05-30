package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.anime.components.RelatedAnimesComfortableGrid
import eu.kanade.presentation.browse.anime.components.RelatedAnimesCompactGrid
import eu.kanade.presentation.browse.anime.components.RelatedAnimesList
import eu.kanade.tachiyomi.ui.entries.anime.RelatedAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun RelatedAnimesContent(
    relatedAnimes: List<RelatedAnime>?,
    getMangaState: @Composable (Anime) -> State<Anime>,
    columns: GridCells,
    entries: Int = 0,
    topBarHeight: Int = 0,
    displayMode: LibraryDisplayMode,
    contentPadding: PaddingValues,
    onMangaClick: (Anime) -> Unit,
    onMangaLongClick: (Anime) -> Unit,
    onKeywordClick: (String) -> Unit,
    onKeywordLongClick: (String) -> Unit,
    selection: List<Anime>,
) {
    if (relatedAnimes == null) {
        LoadingScreen(
            modifier = Modifier.padding(contentPadding),
        )
        return
    }

    if (relatedAnimes.isEmpty()) {
        EmptyScreen(
            modifier = Modifier.padding(contentPadding),
            message = stringResource(MR.strings.no_results_found),
        )
        return
    }

    when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> {
            RelatedAnimesComfortableGrid(
                relatedAnimes = relatedAnimes,
                getManga = getMangaState,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
                onKeywordClick = onKeywordClick,
                onKeywordLongClick = onKeywordLongClick,
                selection = selection!!,
            )
        }
        LibraryDisplayMode.ComfortableGridPanorama -> {
            RelatedAnimesComfortableGrid(
                relatedAnimes = relatedAnimes,
                getManga = getMangaState,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
                onKeywordClick = onKeywordClick,
                onKeywordLongClick = onKeywordLongClick,
                selection = selection!!,
                usePanoramaCover = true,
            )
        }
        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
            RelatedAnimesCompactGrid(
                relatedAnimes = relatedAnimes,
                getManga = getMangaState,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
                onKeywordClick = onKeywordClick,
                onKeywordLongClick = onKeywordLongClick,
                selection = selection!!,
            )
        }
        LibraryDisplayMode.List -> {
            RelatedAnimesList(
                relatedAnimes = relatedAnimes,
                entries = entries,
                topBarHeight = topBarHeight,
                getManga = getMangaState,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
                onKeywordClick = onKeywordClick,
                onKeywordLongClick = onKeywordLongClick,
                selection = selection,
            )
        }
    }
}

@Composable
fun RelatedAnimeTitle(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
) {
    Row(
        modifier = modifier
            .padding(top = MaterialTheme.padding.small)
            .fillMaxWidth()
            .let {
                if (onLongClick == null) {
                    it.clickable(onClick = onClick)
                } else {
                    it.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                }
            },
        horizontalArrangement = if (showArrow) Arrangement.SpaceBetween else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.padding(
                top = MaterialTheme.padding.small,
                bottom = MaterialTheme.padding.extraSmall,
            ),
        ) {
            Text(
                text = title.capitalize(Locale.current),
                style = MaterialTheme.typography.titleMedium,
            )
            if (subtitle != null) {
                Text(text = subtitle)
            }
        }
        if (showArrow) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
        }
    }
}

fun LazyGridScope.header(
    key: Any? = null,
    content: @Composable LazyGridItemScope.() -> Unit,
) {
    item(key = key, span = { GridItemSpan(this.maxLineSpan) }, content = content)
}

@Composable
fun RelatedAnimesLoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(24.dp),
            strokeWidth = 2.dp,
        )
    }
}
