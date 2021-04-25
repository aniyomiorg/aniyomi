package eu.kanade.tachiyomi.ui.watcher.viewer.pager

import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.ui.watcher.model.EpisodeTransition
import eu.kanade.tachiyomi.ui.watcher.model.InsertPage
import eu.kanade.tachiyomi.ui.watcher.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.ui.watcher.viewer.hasMissingEpisodes
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import timber.log.Timber

/**
 * Pager adapter used by this [viewer] to where [ViewerEpisodes] updates are posted.
 */
class PagerViewerAdapter(private val viewer: PagerViewer) : ViewPagerAdapter() {

    /**
     * List of currently set items.
     */
    var items: MutableList<Any> = mutableListOf()
        private set

    var nextTransition: EpisodeTransition.Next? = null
        private set

    var currentEpisode: WatcherEpisode? = null

    /**
     * Updates this adapter with the given [episodes]. It handles setting a few pages of the
     * next/previous episode to allow seamless transitions and inverting the pages if the viewer
     * has R2L direction.
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
            val prevPages = episodes.prevEpisode.seconds
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
        }

        // Skip transition page if the episode is loaded & current page is not a transition page
        if (prevHasMissingEpisodes || forceTransition || episodes.prevEpisode?.state !is WatcherEpisode.State.Loaded) {
            newItems.add(EpisodeTransition.Prev(episodes.currEpisode, episodes.prevEpisode))
        }

        // Add current episode.
        val currPages = episodes.currEpisode.seconds
        if (currPages != null) {
            newItems.addAll(currPages)
        }

        currentEpisode = episodes.currEpisode

        // Add next episode transition and pages.
        nextTransition = EpisodeTransition.Next(episodes.currEpisode, episodes.nextEpisode)
            .also {
                if (nextHasMissingEpisodes || forceTransition ||
                    episodes.nextEpisode?.state !is WatcherEpisode.State.Loaded
                ) {
                    newItems.add(it)
                }
            }

        if (episodes.nextEpisode != null) {
            // Add at most two pages, because this episode will be selected before the user can
            // swap more pages.
            val nextPages = episodes.nextEpisode.seconds
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        // Resets double-page splits, else insert pages get misplaced
        items.filterIsInstance<InsertPage>().also { items.removeAll(it) }

        if (viewer is R2LPagerViewer) {
            newItems.reverse()
        }

        items = newItems
        notifyDataSetChanged()
    }

    /**
     * Returns the amount of items of the adapter.
     */
    override fun getCount(): Int {
        return items.size
    }

    /**
     * Creates a new view for the item at the given [position].
     */
    override fun createView(container: ViewGroup, position: Int): View {
        return when (val item = items[position]) {
            is WatcherPage -> PagerPageHolder(viewer, item)
            is EpisodeTransition -> PagerTransitionHolder(viewer, item)
            else -> throw NotImplementedError("Holder for ${item.javaClass} not implemented")
        }
    }

    /**
     * Returns the current position of the given [view] on the adapter.
     */
    override fun getItemPosition(view: Any): Int {
        if (view is PositionableView) {
            val position = items.indexOf(view.item)
            if (position != -1) {
                return position
            } else {
                Timber.d("Position for ${view.item} not found")
            }
        }
        return POSITION_NONE
    }

    fun onPageSplit(current: Any?, newPage: InsertPage, clazz: Class<out PagerViewer>) {
        if (current !is WatcherPage) return

        val currentIndex = items.indexOf(current)

        val placeAtIndex = when {
            clazz.isAssignableFrom(L2RPagerViewer::class.java) -> currentIndex + 1
            clazz.isAssignableFrom(VerticalPagerViewer::class.java) -> currentIndex + 1
            clazz.isAssignableFrom(R2LPagerViewer::class.java) -> currentIndex
            else -> currentIndex
        }

        // It will enter a endless cycle of insert pages
        if (clazz.isAssignableFrom(R2LPagerViewer::class.java) && items[placeAtIndex - 1] is InsertPage) {
            return
        }

        // Same here it will enter a endless cycle of insert pages
        if (items[placeAtIndex] is InsertPage) {
            return
        }

        items.add(placeAtIndex, newPage)

        notifyDataSetChanged()
    }

    fun cleanupPageSplit() {
        val insertPages = items.filterIsInstance(InsertPage::class.java)
        items.removeAll(insertPages)
        notifyDataSetChanged()
    }
}
