package eu.kanade.tachiyomi.ui.browse.animesource.globalsearch

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.domain.anime.model.Anime
import eu.kanade.tachiyomi.R

class GlobalAnimeSearchCardItem(val anime: Anime) : AbstractFlexibleItem<GlobalAnimeSearchCardHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.global_search_controller_card_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): GlobalAnimeSearchCardHolder {
        return GlobalAnimeSearchCardHolder(view, adapter as GlobalAnimeSearchCardAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: GlobalAnimeSearchCardHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(anime)
    }

    override fun equals(other: Any?): Boolean {
        if (other is GlobalAnimeSearchCardItem) {
            return anime.id == other.anime.id
        }
        return false
    }

    override fun hashCode(): Int {
        return anime.id?.toInt() ?: 0
    }
}
