package eu.kanade.tachiyomi.ui.browse.animesource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.animebrowse.AnimeSourcesScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController

@Composable
fun animeSourcesTab(
    router: Router?,
    presenter: AnimeSourcesPresenter,
) = TabContent(
    titleRes = R.string.label_animesources,
    actions = listOf(
        AppBar.Action(
            title = stringResource(R.string.action_global_search),
            icon = Icons.Outlined.TravelExplore,
            onClick = { router?.pushController(GlobalAnimeSearchController()) },
        ),
        AppBar.Action(
            title = stringResource(R.string.action_filter),
            icon = Icons.Outlined.FilterList,
            onClick = { router?.pushController(AnimeSourceFilterController()) },
        ),
    ),
    content = { contentPadding ->
        AnimeSourcesScreen(
            presenter = presenter,
            contentPadding = contentPadding,
            onClickItem = { source, query ->
                presenter.onOpenSource(source)
                router?.pushController(BrowseAnimeSourceController(source, query))
            },
            onClickDisable = { source ->
                presenter.toggleSource(source)
            },
            onClickPin = { source ->
                presenter.togglePin(source)
            },
        )
    },
)
