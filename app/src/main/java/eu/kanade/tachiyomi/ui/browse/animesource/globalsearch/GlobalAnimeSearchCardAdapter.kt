package eu.kanade.tachiyomi.ui.browse.animesource.globalsearch

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.domain.anime.model.Anime

/**
 * Adapter that holds the anime items from search results.
 *
 * @param controller instance of [GlobalSearchController].
 */
class GlobalAnimeSearchCardAdapter(controller: GlobalAnimeSearchController) :
    FlexibleAdapter<GlobalAnimeSearchCardItem>(null, controller, true) {

    /**
     * Listen for browse item clicks.
     */
    val animeClickListener: OnAnimeClickListener = controller

    /**
     * Listener which should be called when user clicks browse.
     * Note: Should only be handled by [GlobalSearchController]
     */
    interface OnAnimeClickListener {
        fun onAnimeClick(anime: Anime)
        fun onAnimeLongClick(anime: Anime)
    }
}
