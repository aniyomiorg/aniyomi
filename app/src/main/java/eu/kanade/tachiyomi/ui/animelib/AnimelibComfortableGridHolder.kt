package eu.kanade.tachiyomi.ui.animelib

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.util.view.loadAutoPause

/**
 * Class used to hold the displayed data of a anime in the animelib, like the cover or the title.
 * All the elements from the layout file "item_source_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new animelib holder.
 */
class AnimelibComfortableGridHolder(
    private val view: View,
    adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
) : AnimelibHolder<SourceComfortableGridItemBinding>(view, adapter) {

    override val binding = SourceComfortableGridItemBinding.bind(view)

    /**
     * Method called from [AnimelibCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given anime.
     *
     * @param item the anime item to bind.
     */
    override fun onSetValues(item: AnimelibItem) {
        // Update the title of the anime.
        binding.title.text = item.anime.title

        // For rounded corners
        binding.badges.leftBadges.clipToOutline = true
        binding.badges.rightBadges.clipToOutline = true

        // Update the unread count and its visibility.
        with(binding.badges.unreadText) {
            isVisible = item.unreadCount > 0
            text = item.unreadCount.toString()
        }
        // Update the download count and its visibility.
        with(binding.badges.downloadText) {
            isVisible = item.downloadCount > 0
            text = item.downloadCount.toString()
        }
        // Update the source language and its visibility
        with(binding.badges.languageText) {
            isVisible = item.sourceLanguage.isNotEmpty()
            text = item.sourceLanguage
        }
        // set local visibility if its local anime
        binding.badges.localText.isVisible = item.isLocal

        // For rounded corners
        binding.card.clipToOutline = true

        // Update the cover.
        binding.thumbnail.dispose()
        binding.thumbnail.loadAutoPause(item.anime)
    }
}
