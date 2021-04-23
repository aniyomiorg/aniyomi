package eu.kanade.tachiyomi.ui.browse.animesource

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.AnimeCatalogueSource

/**
 * Item that contains source information.
 *
 * @param source Instance of [CatalogueSource] containing source information.
 * @param header The header for this item.
 */
data class AnimeSourceItem(
    val source: AnimeCatalogueSource,
    val header: LangItem? = null,
    val isPinned: Boolean = false
) :
    AbstractSectionableItem<AnimeSourceHolder, LangItem>(header) {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.source_main_controller_card_item
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): AnimeSourceHolder {
        return AnimeSourceHolder(view, adapter as AnimeSourceAdapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: AnimeSourceHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other is AnimeSourceItem) {
            return source.id == other.source.id &&
                getHeader()?.code == other.getHeader()?.code &&
                isPinned == other.isPinned
        }
        return false
    }

    override fun hashCode(): Int {
        var result = source.id.hashCode()
        result = 31 * result + (header?.hashCode() ?: 0)
        result = 31 * result + isPinned.hashCode()
        return result
    }
}
