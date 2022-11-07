package eu.kanade.tachiyomi.ui.browse.migration.search

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.bundleOf
import eu.kanade.domain.anime.model.Anime
import eu.kanade.presentation.animebrowse.AnimeSourceSearchScreen
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.getSerializableCompat

class AnimeSourceSearchController(
    bundle: Bundle,
) : BrowseAnimeSourceController(bundle) {

    constructor(anime: Anime? = null, source: AnimeCatalogueSource, searchQuery: String? = null) : this(
        bundleOf(
            SOURCE_ID_KEY to source.id,
            ANIME_KEY to anime,
            SEARCH_QUERY_KEY to searchQuery,
        ),
    )
    private var oldAnime: Anime? = args.getSerializableCompat(ANIME_KEY)
    private var newAnime: Anime? = null

    @Composable
    override fun ComposeContent() {
        AnimeSourceSearchScreen(
            presenter = presenter,
            navigateUp = { router.popCurrentController() },
            onFabClick = { filterSheet?.show() },
            onAnimeClick = { it ->
                newAnime = it
                val searchController = router.backstack.findLast { it.controller.javaClass == AnimeSearchController::class.java }?.controller as AnimeSearchController?
                val dialog = AnimeSearchController.MigrationDialog(oldAnime, newAnime, this)
                dialog.targetController = searchController
                dialog.showDialog(router)
            },
            onWebViewClick = f@{
                val source = presenter.source as? AnimeHttpSource ?: return@f
                activity?.let { context ->
                    val intent = WebViewActivity.newIntent(context, source.baseUrl, source.id, source.name)
                    context.startActivity(intent)
                }
            },
        )

        LaunchedEffect(presenter.filters) {
            initFilterSheet()
        }
    }
}

private const val ANIME_KEY = "oldAnime"
