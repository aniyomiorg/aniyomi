package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.fredporciuncula.flow.preferences.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.domain.anime.model.Anime
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.databinding.SourceCompactGridItemBinding
import eu.kanade.tachiyomi.ui.library.setting.DisplayModeSetting

class AnimeSourceItem(val anime: Anime, private val displayMode: Preference<DisplayModeSetting>) :
    AbstractFlexibleItem<AnimeSourceHolder<*>>() {

    override fun getLayoutRes(): Int {
        return when (displayMode.get()) {
            DisplayModeSetting.COMPACT_GRID, DisplayModeSetting.COVER_ONLY_GRID -> R.layout.source_compact_grid_item
            DisplayModeSetting.COMFORTABLE_GRID -> R.layout.source_comfortable_grid_item
            DisplayModeSetting.LIST -> R.layout.source_list_item
        }
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): AnimeSourceHolder<*> {
        return when (displayMode.get()) {
            DisplayModeSetting.COMPACT_GRID, DisplayModeSetting.COVER_ONLY_GRID -> {
                AnimeSourceCompactGridHolder(SourceCompactGridItemBinding.bind(view), adapter)
            }
            DisplayModeSetting.COMFORTABLE_GRID -> {
                AnimeSourceComfortableGridHolder(SourceComfortableGridItemBinding.bind(view), adapter)
            }
            DisplayModeSetting.LIST -> {
                AnimeSourceListHolder(view, adapter)
            }
        }
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: AnimeSourceHolder<*>,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.onSetValues(anime)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is AnimeSourceItem) {
            return anime.id!! == other.anime.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return anime.id!!.hashCode()
    }
}
