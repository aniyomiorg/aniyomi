package eu.kanade.tachiyomi.ui.download.anime

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload

class DownloadItem(
    val download: AnimeDownload,
    header: DownloadHeaderItem,
) : AbstractSectionableItem<AnimeDownloadHolder, DownloadHeaderItem>(header) {

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
    ): AnimeDownloadHolder {
        return AnimeDownloadHolder(view, adapter as DownloadAdapter)
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
        holder: AnimeDownloadHolder,
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
        if (other is DownloadItem) {
            return download.episode.id == other.download.episode.id
        }
        return false
    }

    override fun hashCode(): Int {
        return download.episode.id!!.toInt()
    }
}
