package eu.kanade.presentation.animesource

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.presentation.animesource.components.BaseAnimeSourceItem
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.ItemBadges
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.theme.header
import eu.kanade.presentation.util.horizontalPadding
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.migration.animesources.MigrateAnimeSourceState
import eu.kanade.tachiyomi.ui.browse.migration.animesources.MigrationAnimeSourcesPresenter

@Composable
fun MigrateAnimeSourceScreen(
    nestedScrollInterop: NestedScrollConnection,
    presenter: MigrationAnimeSourcesPresenter,
    onClickItem: (AnimeSource) -> Unit,
    onLongClickItem: (AnimeSource) -> Unit,
) {
    val state by presenter.state.collectAsState()
    when (state) {
        is MigrateAnimeSourceState.Loading -> LoadingScreen()
        is MigrateAnimeSourceState.Error -> Text(text = (state as MigrateAnimeSourceState.Error).error.message!!)
        is MigrateAnimeSourceState.Success ->
            MigrateAnimeSourceList(
                nestedScrollInterop = nestedScrollInterop,
                list = (state as MigrateAnimeSourceState.Success).sources,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
            )
    }
}

@Composable
fun MigrateAnimeSourceList(
    nestedScrollInterop: NestedScrollConnection,
    list: List<Pair<AnimeSource, Long>>,
    onClickItem: (AnimeSource) -> Unit,
    onLongClickItem: (AnimeSource) -> Unit,
) {
    if (list.isEmpty()) {
        EmptyScreen(textResource = R.string.information_empty_library)
        return
    }

    LazyColumn(
        modifier = Modifier.nestedScroll(nestedScrollInterop),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(id = R.string.migration_selection_prompt),
                modifier = Modifier
                    .animateItemPlacement()
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                style = MaterialTheme.typography.header
            )
        }

        items(
            items = list,
            key = { (source, _) ->
                source.id
            }
        ) { (source, count) ->
            MigrateAnimeSourceItem(
                modifier = Modifier.animateItemPlacement(),
                source = source,
                count = count,
                onClickItem = { onClickItem(source) },
                onLongClickItem = { onLongClickItem(source) }
            )
        }
    }
}

@Composable
fun MigrateAnimeSourceItem(
    modifier: Modifier = Modifier,
    source: AnimeSource,
    count: Long,
    onClickItem: () -> Unit,
    onLongClickItem: () -> Unit,
) {
    BaseAnimeSourceItem(
        modifier = modifier,
        source = source,
        showLanguageInContent = source.lang != "",
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        action = { ItemBadges(primaryText = "$count") },
    )
}
