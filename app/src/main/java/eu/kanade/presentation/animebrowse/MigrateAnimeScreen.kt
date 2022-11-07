package eu.kanade.presentation.animebrowse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.anime.model.Anime
import eu.kanade.presentation.anime.components.BaseAnimeListItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.migration.anime.MigrateAnimePresenter
import eu.kanade.tachiyomi.ui.browse.migration.anime.MigrateAnimePresenter.Event
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MigrateAnimeScreen(
    navigateUp: () -> Unit,
    title: String?,
    presenter: MigrateAnimePresenter,
    onClickItem: (Anime) -> Unit,
    onClickCover: (Anime) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = title,
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        when {
            presenter.isLoading -> LoadingScreen()
            presenter.isEmpty -> EmptyScreen(
                textResource = R.string.empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                MigrateAnimeContent(
                    contentPadding = contentPadding,
                    state = presenter,
                    onClickItem = onClickItem,
                    onClickCover = onClickCover,
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        presenter.events.collectLatest { event ->
            when (event) {
                Event.FailedFetchingFavorites -> {
                    context.toast(R.string.internal_error)
                }
            }
        }
    }
}

@Composable
private fun MigrateAnimeContent(
    contentPadding: PaddingValues,
    state: MigrateAnimeState,
    onClickItem: (Anime) -> Unit,
    onClickCover: (Anime) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(state.items) { anime ->
            MigrateAnimeItem(
                anime = anime,
                onClickItem = onClickItem,
                onClickCover = onClickCover,
            )
        }
    }
}

@Composable
private fun MigrateAnimeItem(
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
