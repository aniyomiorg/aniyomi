package eu.kanade.tachiyomi.ui.animelib

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.fredporciuncula.flow.preferences.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.databinding.SourceCompactGridItemBinding
import eu.kanade.tachiyomi.ui.library.setting.DisplayModeSetting
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimelibItem(
    val anime: AnimelibAnime,
    private val shouldSetFromCategory: Preference<Boolean>,
    private val defaultLibraryDisplayMode: Preference<DisplayModeSetting>,
) :
    AbstractFlexibleItem<AnimelibHolder<*>>(), IFilterable<String> {

    private val sourceManager: AnimeSourceManager = Injekt.get()

    var displayMode: Long = -1
    var downloadCount = -1
    var unreadCount = -1
    var isLocal = false
    var sourceLanguage = ""

    private fun getDisplayMode(): DisplayModeSetting {
        return if (shouldSetFromCategory.get() && anime.category != 0) {
            DisplayModeSetting.fromFlag(displayMode)
        } else {
            defaultLibraryDisplayMode.get()
        }
    }

    override fun getLayoutRes(): Int {
        return when (getDisplayMode()) {
            DisplayModeSetting.COMPACT_GRID, DisplayModeSetting.COVER_ONLY_GRID -> R.layout.source_compact_grid_item
            DisplayModeSetting.COMFORTABLE_GRID -> R.layout.source_comfortable_grid_item
            DisplayModeSetting.LIST -> R.layout.source_list_item
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): AnimelibHolder<*> {
        return when (getDisplayMode()) {
            DisplayModeSetting.COMPACT_GRID -> {
                AnimelibCompactGridHolder(SourceCompactGridItemBinding.bind(view), adapter, coverOnly = false)
            }
            DisplayModeSetting.COVER_ONLY_GRID -> {
                AnimelibCompactGridHolder(SourceCompactGridItemBinding.bind(view), adapter, coverOnly = true)
            }
            DisplayModeSetting.COMFORTABLE_GRID -> {
                AnimelibComfortableGridHolder(SourceComfortableGridItemBinding.bind(view), adapter)
            }
            DisplayModeSetting.LIST -> {
                AnimelibListHolder(view, adapter)
            }
        }
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: AnimelibHolder<*>,
        position: Int,
        payloads: List<Any?>?,
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
        val sourceName by lazy { sourceManager.getOrStub(anime.source).name }
        val genres by lazy { anime.getGenres() }
        return anime.title.contains(constraint, true) ||
            (anime.author?.contains(constraint, true) ?: false) ||
            (anime.artist?.contains(constraint, true) ?: false) ||
            (anime.description?.contains(constraint, true) ?: false) ||
            if (constraint.contains(",")) {
                constraint.split(",").all { containsSourceOrGenre(it.trim(), sourceName, genres) }
            } else {
                containsSourceOrGenre(constraint, sourceName, genres)
            }
    }

    /**
     * Filters an anime by checking whether the query is the anime's source OR part of
     * the genres of the anime
     * Checking for genre is done only if the query isn't part of the source name.
     *
     * @param query the query to check
     * @param sourceName name of the anime's source
     * @param genres list containing anime's genres
     */
    private fun containsSourceOrGenre(query: String, sourceName: String, genres: List<String>?): Boolean {
        val minus = query.startsWith("-")
        val tag = if (minus) { query.substringAfter("-") } else query
        return when (sourceName.contains(tag, true)) {
            false -> containsGenre(query, genres)
            else -> !minus
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
