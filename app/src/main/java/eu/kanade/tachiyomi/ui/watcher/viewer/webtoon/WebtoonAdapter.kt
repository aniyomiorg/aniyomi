package eu.kanade.tachiyomi.ui.watcher.viewer.webtoon

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.ui.watcher.model.EpisodeTransition
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.ui.watcher.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.watcher.viewer.hasMissingEpisodes

/**
 * RecyclerView Adapter used by this [viewer] to where [ViewerEpisodes] updates are posted.
 */
class WebtoonAdapter(val viewer: WebtoonViewer) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * List of currently set items.
     */
    var items: List<Any> = emptyList()
        private set

    var currentEpisode: WatcherEpisode? = null

    /**
     * Updates this adapter with the given [episodes]. It handles setting a few pages of the
     * next/previous episode to allow seamless transitions.
     */
    fun setEpisodes(episodes: ViewerEpisodes, forceTransition: Boolean) {
        val newItems = mutableListOf<Any>()

        // Forces episode transition if there is missing episodes
        val prevHasMissingEpisodes = hasMissingEpisodes(episodes.currEpisode, episodes.prevEpisode)
        val nextHasMissingEpisodes = hasMissingEpisodes(episodes.nextEpisode, episodes.currEpisode)

        // Add previous episode pages and transition.
        if (episodes.prevEpisode != null) {
            // We only need to add the last few pages of the previous episode, because it'll be
            // selected as the current episode when one of those pages is selected.
            val prevPages = episodes.prevEpisode.pages
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
        }

        // Skip transition page if the episode is loaded & current page is not a transition page
        if (prevHasMissingEpisodes || forceTransition || episodes.prevEpisode?.state !is WatcherEpisode.State.Loaded) {
            newItems.add(EpisodeTransition.Prev(episodes.currEpisode, episodes.prevEpisode))
        }

        // Add current episode.
        val currPages = episodes.currEpisode.pages
        if (currPages != null) {
            newItems.addAll(currPages)
        }

        currentEpisode = episodes.currEpisode

        // Add next episode transition and pages.
        if (nextHasMissingEpisodes || forceTransition || episodes.nextEpisode?.state !is WatcherEpisode.State.Loaded) {
            newItems.add(EpisodeTransition.Next(episodes.currEpisode, episodes.nextEpisode))
        }

        if (episodes.nextEpisode != null) {
            // Add at most two pages, because this episode will be selected before the user can
            // swap more pages.
            val nextPages = episodes.nextEpisode.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        val result = DiffUtil.calculateDiff(Callback(items, newItems))
        items = newItems
        result.dispatchUpdatesTo(this)
    }

    /**
     * Returns the amount of items of the adapter.
     */
    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Returns the view type for the item at the given [position].
     */
    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is WatcherPage -> PAGE_VIEW
            is EpisodeTransition -> TRANSITION_VIEW
            else -> error("Unknown view type for ${item.javaClass}")
        }
    }

    /**
     * Creates a new view holder for an item with the given [viewType].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            PAGE_VIEW -> {
                val view = FrameLayout(parent.context)
                WebtoonPageHolder(view, viewer)
            }
            TRANSITION_VIEW -> {
                val view = LinearLayout(parent.context)
                WebtoonTransitionHolder(view, viewer)
            }
            else -> error("Unknown view type")
        }
    }

    /**
     * Binds an existing view [holder] with the item at the given [position].
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is WebtoonPageHolder -> holder.bind(item as WatcherPage)
            is WebtoonTransitionHolder -> holder.bind(item as EpisodeTransition)
        }
    }

    /**
     * Recycles an existing view [holder] before adding it to the view pool.
     */
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is WebtoonPageHolder -> holder.recycle()
            is WebtoonTransitionHolder -> holder.recycle()
        }
    }

    /**
     * Diff util callback used to dispatch delta updates instead of full dataset changes.
     */
    private class Callback(
        private val oldItems: List<Any>,
        private val newItems: List<Any>
    ) : DiffUtil.Callback() {

        /**
         * Returns true if these two items are the same.
         */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]

            return oldItem == newItem
        }

        /**
         * Returns true if the contents of the items are the same.
         */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return true
        }

        /**
         * Returns the size of the old list.
         */
        override fun getOldListSize(): Int {
            return oldItems.size
        }

        /**
         * Returns the size of the new list.
         */
        override fun getNewListSize(): Int {
            return newItems.size
        }
    }

    private companion object {
        /**
         * View holder type of a episode page view.
         */
        const val PAGE_VIEW = 0

        /**
         * View holder type of a episode transition view.
         */
        const val TRANSITION_VIEW = 1
    }
}
