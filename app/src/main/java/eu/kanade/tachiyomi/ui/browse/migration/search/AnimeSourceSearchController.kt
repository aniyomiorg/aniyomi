package eu.kanade.tachiyomi.ui.browse.migration.search

import android.os.Bundle
import android.view.View
import eu.kanade.domain.anime.model.Anime
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.animesource.browse.AnimeSourceItem
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController

class AnimeSourceSearchController(
    bundle: Bundle,
) : BrowseAnimeSourceController(bundle) {

    constructor(anime: Anime? = null, source: AnimeCatalogueSource, searchQuery: String? = null) : this(
        Bundle().apply {
            putLong(SOURCE_ID_KEY, source.id)
            putSerializable(ANIME_KEY, anime)
            if (searchQuery != null) {
                putString(SEARCH_QUERY_KEY, searchQuery)
            }
        },
    )
    private var oldAnime: Anime? = args.getSerializable(ANIME_KEY) as Anime?
    private var newAnime: Anime? = null

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) as? AnimeSourceItem ?: return false
        newAnime = item.anime
        val searchController = router.backstack.findLast { it.controller.javaClass == AnimeSearchController::class.java }?.controller as AnimeSearchController?
        val dialog =
            AnimeSearchController.MigrationDialog(oldAnime, newAnime, this)
        dialog.targetController = searchController
        dialog.showDialog(router)
        return true
    }

    override fun onItemLongClick(position: Int) {
        view?.let { super.onItemClick(it, position) }
    }
}

private const val ANIME_KEY = "oldAnime"
