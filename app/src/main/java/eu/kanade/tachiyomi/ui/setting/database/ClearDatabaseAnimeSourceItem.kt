package eu.kanade.tachiyomi.ui.setting.database

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource

data class ClearDatabaseAnimeSourceItem(val source: AnimeSource, private val animeCount: Int) : AbstractFlexibleItem<ClearDatabaseSourceItemHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.clear_database_source_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ClearDatabaseSourceItemHolder {
        return ClearDatabaseSourceItemHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: ClearDatabaseSourceItemHolder?, position: Int, payloads: MutableList<Any>?) {
        if (payloads.isNullOrEmpty()) {
            holder?.bind(source, animeCount)
        } else {
            holder?.updateCheckbox()
        }
    }
}
