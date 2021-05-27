package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tfcporciuncula.flow.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.preference.PreferenceValues.DisplayMode
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.databinding.SourceCompactGridItemBinding
import eu.kanade.tachiyomi.widget.AutofitRecyclerView

class AnimeSourceItem(val anime: Anime, private val displayMode: Preference<DisplayMode>) :
    AbstractFlexibleItem<AnimeSourceHolder<*>>() {

    override fun getLayoutRes(): Int {
        return when (displayMode.get()) {
            DisplayMode.COMPACT_GRID -> R.layout.source_compact_grid_item
            DisplayMode.COMFORTABLE_GRID -> R.layout.source_comfortable_grid_item
            DisplayMode.LIST -> R.layout.source_list_item
        }
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): AnimeSourceHolder<*> {
        return when (displayMode.get()) {
            DisplayMode.COMPACT_GRID -> {
                val binding = SourceCompactGridItemBinding.bind(view)
                val parent = adapter.recyclerView as AutofitRecyclerView
                val coverHeight = parent.itemWidth / 3 * 4
                view.apply {
                    binding.card.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        coverHeight
                    )
                    binding.gradient.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        coverHeight / 2,
                        Gravity.BOTTOM
                    )
                }
                AnimeSourceGridHolder(view, adapter)
            }
            DisplayMode.COMFORTABLE_GRID -> {
                val binding = SourceComfortableGridItemBinding.bind(view)
                val parent = adapter.recyclerView as AutofitRecyclerView
                val coverHeight = parent.itemWidth / 3 * 4
                view.apply {
                    binding.card.layoutParams = ConstraintLayout.LayoutParams(
                        MATCH_PARENT,
                        coverHeight
                    )
                }
                AnimeSourceComfortableGridHolder(view, adapter)
            }
            DisplayMode.LIST -> {
                AnimeSourceListHolder(view, adapter)
            }
        }
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: AnimeSourceHolder<*>,
        position: Int,
        payloads: List<Any?>?
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
