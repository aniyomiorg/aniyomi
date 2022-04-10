package eu.kanade.tachiyomi.ui.recent.animehistory

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.AnimeEpisodeHistory
import eu.kanade.tachiyomi.ui.recent.DateSectionItem

class AnimeHistoryItem(val aeh: AnimeEpisodeHistory, header: DateSectionItem) :
    AbstractSectionableItem<AnimeHistoryHolder, DateSectionItem>(header) {

    override fun getLayoutRes(): Int {
        return R.layout.history_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): AnimeHistoryHolder {
        return AnimeHistoryHolder(view, adapter as AnimeHistoryAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: AnimeHistoryHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(aeh)
    }

    override fun equals(other: Any?): Boolean {
        if (other is AnimeHistoryItem) {
            return aeh.anime.id == other.aeh.anime.id
        }
        return false
    }

    override fun hashCode(): Int {
        return aeh.anime.id!!.hashCode()
    }
}
