package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import eu.kanade.domain.anime.model.Anime
import eu.kanade.presentation.anime.components.BaseAnimeListItem
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.ScrollbarLazyColumn
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.migration.anime.MigrateAnimeState
import eu.kanade.tachiyomi.ui.browse.migration.anime.MigrationAnimePresenter

@Composable
fun MigrateAnimeScreen(
    nestedScrollInterop: NestedScrollConnection,
    presenter: MigrationAnimePresenter,
    onClickItem: (Anime) -> Unit,
    onClickCover: (Anime) -> Unit,
) {
    val state by presenter.state.collectAsState()

    when (state) {
        MigrateAnimeState.Loading -> LoadingScreen()
        is MigrateAnimeState.Error -> Text(text = (state as MigrateAnimeState.Error).error.message!!)
        is MigrateAnimeState.Success -> {
            MigrateAnimeContent(
                nestedScrollInterop = nestedScrollInterop,
                list = (state as MigrateAnimeState.Success).list,
                onClickItem = onClickItem,
                onClickCover = onClickCover,
            )
        }
    }
}

@Composable
fun MigrateAnimeContent(
    nestedScrollInterop: NestedScrollConnection,
    list: List<Anime>,
    onClickItem: (Anime) -> Unit,
    onClickCover: (Anime) -> Unit,
) {
    if (list.isEmpty()) {
        EmptyScreen(textResource = R.string.empty_screen)
        return
    }
    ScrollbarLazyColumn(
        modifier = Modifier.nestedScroll(nestedScrollInterop),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
    ) {
        items(list) { anime ->
            MigrateAnimeItem(
                anime = anime,
                onClickItem = onClickItem,
                onClickCover = onClickCover,
            )
        }
    }
}

@Composable
fun MigrateAnimeItem(
    modifier: Modifier = Modifier,
    anime: Anime,
    onClickItem: (Anime) -> Unit,
    onClickCover: (Anime) -> Unit,
) {
    BaseAnimeListItem(
        modifier = modifier,
        anime = anime,
        onClickItem = { onClickItem(anime) },
        onClickCover = { onClickCover(anime) },
    )
}
