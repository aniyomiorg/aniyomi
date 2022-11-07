package eu.kanade.tachiyomi.ui.browse.animesource.globalsearch

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.domain.anime.model.Anime
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.databinding.GlobalSearchControllerCardBinding
import eu.kanade.tachiyomi.util.system.LocaleHelper

/**
 * Holder that binds the [GlobalAnimeSearchItem] containing catalogue cards.
 *
 * @param view view of [GlobalAnimeSearchItem]
 * @param adapter instance of [GlobalAnimeSearchAdapter]
 */
class GlobalAnimeSearchHolder(view: View, val adapter: GlobalAnimeSearchAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = GlobalSearchControllerCardBinding.bind(view)

    /**
     * Adapter containing anime from search results.
     */
    private val animeAdapter = GlobalAnimeSearchCardAdapter(adapter.controller)

    private var lastBoundResults: List<GlobalAnimeSearchCardItem>? = null

    init {
        // Set layout horizontal.
        binding.recycler.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        binding.recycler.adapter = animeAdapter

        binding.titleWrapper.setOnClickListener {
            adapter.getItem(bindingAdapterPosition)?.let {
                adapter.titleClickListener.onTitleClick(it.source)
            }
        }
    }

    /**
     * Show the loading of source search result.
     *
     * @param item item of card.
     */
    fun bind(item: GlobalAnimeSearchItem) {
        val source = item.source
        val results = item.results

        val titlePrefix = if (item.highlighted) "▶ " else ""

        binding.title.text = titlePrefix + source.name
        binding.subtitle.isVisible = source !is LocalAnimeSource
        binding.subtitle.text = LocaleHelper.getDisplayName(source.lang)

        when {
            results == null -> {
                binding.progress.isVisible = true
                showResultsHolder()
            }
            results.isEmpty() -> {
                binding.progress.isVisible = false
                showNoResults()
            }
            else -> {
                binding.progress.isVisible = false
                showResultsHolder()
            }
        }
        if (results !== lastBoundResults) {
            animeAdapter.updateDataSet(results)
            lastBoundResults = results
        }
    }

    /**
     * Called from the presenter when an anime is initialized.
     *
     * @param anime the initialized anime.
     */
    fun setImage(anime: Anime) {
        getHolder(anime)?.setImage(anime)
    }

    /**
     * Returns the view holder for the given anime.
     *
     * @param anime the anime to find.
     * @return the holder of the anime or null if it's not bound.
     */
    private fun getHolder(anime: Anime): GlobalAnimeSearchCardHolder? {
        animeAdapter.allBoundViewHolders.forEach { holder ->
            val item = animeAdapter.getItem(holder.bindingAdapterPosition)
            if (item != null && item.anime.id == anime.id) {
                return holder as GlobalAnimeSearchCardHolder
            }
        }

        return null
    }

    private fun showResultsHolder() {
        binding.noResultsFound.isVisible = false
    }

    private fun showNoResults() {
        binding.noResultsFound.isVisible = true
    }
}
