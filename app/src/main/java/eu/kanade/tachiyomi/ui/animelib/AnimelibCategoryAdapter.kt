package eu.kanade.tachiyomi.ui.animelib

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.domain.anime.model.Anime

/**
 * Adapter storing a list of anime in a certain category.
 *
 * @param view the fragment containing this adapter.
 */
class AnimelibCategoryAdapter(view: AnimelibCategoryView) :
    FlexibleAdapter<AnimelibItem>(null, view, true) {

    /**
     * The list of anime in this category.
     */
    private var animes: List<AnimelibItem> = emptyList()

    /**
     * Sets a list of anime in the adapter.
     *
     * @param list the list to set.
     */
    fun setItems(list: List<AnimelibItem>) {
        // A copy of anime always unfiltered.
        animes = list.toList()

        performFilter()
    }

    /**
     * Returns the position in the adapter for the given anime.
     *
     * @param anime the anime to find.
     */
    fun indexOf(anime: Anime): Int {
        return currentItems.indexOfFirst { it.anime.id == anime.id }
    }

    fun performFilter() {
        val s = getFilter(String::class.java) ?: ""
        updateDataSet(animes.filter { it.filter(s) })
    }
}
