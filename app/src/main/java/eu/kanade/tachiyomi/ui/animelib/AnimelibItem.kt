package eu.kanade.tachiyomi.ui.animelib

import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tfcporciuncula.flow.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime
import eu.kanade.tachiyomi.data.preference.PreferenceValues.DisplayMode
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.databinding.SourceCompactGridItemBinding
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimelibItem(
    val anime: AnimelibAnime,
    private val shouldSetFromCategory: Preference<Boolean>,
    private val defaultLibraryDisplayMode: Preference<DisplayMode>
) :
    AbstractFlexibleItem<AnimelibHolder<*>>(), IFilterable<String> {

    private val sourceManager: AnimeSourceManager = Injekt.get()

    var displayMode: Int = -1
    var downloadCount = -1
    var unreadCount = -1
    var isLocal = false

    private fun getDisplayMode(): DisplayMode {
        return if (shouldSetFromCategory.get() && anime.category != 0) {
            if (displayMode != -1) {
                DisplayMode.values()[displayMode]
            } else {
                DisplayMode.COMPACT_GRID
            }
        } else {
            defaultLibraryDisplayMode.get()
        }
    }

    override fun getLayoutRes(): Int {
        return when (getDisplayMode()) {
            DisplayMode.COMPACT_GRID -> R.layout.source_compact_grid_item
            DisplayMode.COMFORTABLE_GRID -> R.layout.source_comfortable_grid_item
            DisplayMode.LIST -> R.layout.source_list_item
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): AnimelibHolder<*> {
        return when (getDisplayMode()) {
            DisplayMode.COMPACT_GRID -> {
                val binding = SourceCompactGridItemBinding.bind(view)
                val parent = adapter.recyclerView as AutofitRecyclerView
                val coverHeight = parent.itemWidth / 3 * 4
                view.apply {
                    binding.card.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight)
                    binding.gradient.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        coverHeight / 2,
                        Gravity.BOTTOM
                    )
                }
                AnimelibCompactGridHolder(view, adapter)
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
                AnimelibComfortableGridHolder(view, adapter)
            }
            DisplayMode.LIST -> {
                AnimelibListHolder(view, adapter)
            }
        }
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: AnimelibHolder<*>,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.onSetValues(this)
    }

    /**
     * Filters a anime depending on a query.
     *
     * @param constraint the query to apply.
     * @return true if the anime should be included, false otherwise.
     */
    override fun filter(constraint: String): Boolean {
        return anime.title.contains(constraint, true) ||
            (anime.author?.contains(constraint, true) ?: false) ||
            (anime.artist?.contains(constraint, true) ?: false) ||
            (anime.description?.contains(constraint, true) ?: false) ||
            sourceManager.getOrStub(anime.source).name.contains(constraint, true) ||
            if (constraint.contains(",")) {
                constraint.split(",").all { containsGenre(it.trim(), anime.getGenres()) }
            } else {
                containsGenre(constraint, anime.getGenres())
            }
    }

    private fun containsGenre(tag: String, genres: List<String>?): Boolean {
        return if (tag.startsWith("-")) {
            genres?.find {
                it.trim().equals(tag.substringAfter("-"), ignoreCase = true)
            } == null
        } else {
            genres?.find {
                it.trim().equals(tag, ignoreCase = true)
            } != null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is AnimelibItem) {
            return anime.id == other.anime.id
        }
        return false
    }

    override fun hashCode(): Int {
        return anime.id!!.hashCode()
    }
}
