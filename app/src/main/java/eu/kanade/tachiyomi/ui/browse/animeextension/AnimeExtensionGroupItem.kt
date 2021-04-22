package eu.kanade.tachiyomi.ui.browse.animeextension

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R

/**
 * Item that contains the group header.
 *
 * @param name The header name.
 * @param size The number of items in the group.
 */
data class AnimeExtensionGroupItem(val name: String, val size: Int, val showSize: Boolean = false) : AbstractHeaderItem<AnimeExtensionGroupHolder>() {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.section_header_item
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): AnimeExtensionGroupHolder {
        return AnimeExtensionGroupHolder(view, adapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: AnimeExtensionGroupHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is AnimeExtensionGroupItem) {
            return name == other.name
        }
        return false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
