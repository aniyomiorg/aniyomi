package eu.kanade.tachiyomi.ui.download.manga

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractExpandableHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R

data class MangaDownloadHeaderItem(
    val id: Long,
    val name: String,
    val size: Int,
) : AbstractExpandableHeaderItem<MangaDownloadHeaderHolder, MangaDownloadItem>() {

    override fun getLayoutRes(): Int {
        return R.layout.download_header
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): MangaDownloadHeaderHolder {
        return MangaDownloadHeaderHolder(view, adapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: MangaDownloadHeaderHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MangaDownloadHeaderItem

        if (id != other.id) return false
        if (name != other.name) return false
        if (size != other.size) return false
        if (subItemsCount != other.subItemsCount) return false
        if (subItems !== other.subItems) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + size
        result = 31 * result + subItems.hashCode()
        return result
    }

    init {
        isHidden = false
        isExpanded = true
        isSelectable = false
    }
}
