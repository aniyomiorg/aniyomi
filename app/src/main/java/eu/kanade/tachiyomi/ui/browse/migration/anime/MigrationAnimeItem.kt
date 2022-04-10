package eu.kanade.tachiyomi.ui.browse.migration.anime

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime

class MigrationAnimeItem(val anime: Anime) : AbstractFlexibleItem<MigrationAnimeHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.source_list_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): MigrationAnimeHolder {
        return MigrationAnimeHolder(view, adapter as MigrationAnimeAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: MigrationAnimeHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other is MigrationAnimeItem) {
            return anime.id == other.anime.id
        }
        return false
    }

    override fun hashCode(): Int {
        return anime.id!!.hashCode()
    }
}
