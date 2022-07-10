package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.view.View
import androidx.viewbinding.ViewBinding
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.domain.anime.model.Anime

/**
 * Generic class used to hold the displayed data of a anime in the catalogue.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 */
abstract class AnimeSourceHolder<VB : ViewBinding>(view: View, adapter: FlexibleAdapter<*>) :
    FlexibleViewHolder(view, adapter) {

    abstract val binding: VB

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given anime.
     *
     * @param anime the anime to bind.
     */
    abstract fun onSetValues(anime: Anime)

    /**
     * Updates the image for this holder. Useful to update the image when the anime is initialized
     * and the url is now known.
     *
     * @param anime the anime to bind.
     */
    abstract fun setImage(anime: Anime)
}
