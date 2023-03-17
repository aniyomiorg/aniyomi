package eu.kanade.tachiyomi.ui.download.manga

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload

class MangaDownloadItem(
    val download: MangaDownload,
    header: MangaDownloadHeaderItem,
) : AbstractSectionableItem<MangaDownloadHolder, MangaDownloadHeaderItem>(header) {

    override fun getLayoutRes(): Int {
        return R.layout.download_item
    }

    /**
     * Returns a new view holder for this item.
     *
     * @param view The view of this item.
     * @param adapter The adapter of this item.
     */
    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): MangaDownloadHolder {
        return MangaDownloadHolder(view, adapter as MangaDownloadAdapter)
    }

    /**
     * Binds the given view holder with this item.
     *
     * @param adapter The adapter of this item.
     * @param holder The holder to bind.
     * @param position The position of this item in the adapter.
     * @param payloads List of partial changes.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: MangaDownloadHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        holder.bind(download)
    }

    /**
     * Returns true if this item is draggable.
     */
    override fun isDraggable(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is MangaDownloadItem) {
            return download.chapter.id == other.download.chapter.id
        }
        return false
    }

    override fun hashCode(): Int {
        return download.chapter.id.toInt()
    }
}
