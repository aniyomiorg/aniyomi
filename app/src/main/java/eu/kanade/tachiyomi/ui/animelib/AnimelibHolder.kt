package eu.kanade.tachiyomi.ui.animelib

import android.view.View
import androidx.viewbinding.ViewBinding
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder

/**
 * Generic class used to hold the displayed data of a anime in the animelib.
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to the single tap and long tap events.
 */

abstract class AnimelibHolder<VB : ViewBinding>(
    view: View,
    adapter: FlexibleAdapter<*>
) : FlexibleViewHolder(view, adapter) {

    abstract val binding: VB

    /**
     * Method called from [AnimelibCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given anime.
     *
     * @param item the anime item to bind.
     */
    abstract fun onSetValues(item: AnimelibItem)
}
