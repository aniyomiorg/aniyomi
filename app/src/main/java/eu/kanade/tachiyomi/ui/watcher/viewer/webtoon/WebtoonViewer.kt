package eu.kanade.tachiyomi.ui.watcher.viewer.webtoon

import android.graphics.PointF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.WebtoonLayoutManager
import eu.kanade.tachiyomi.ui.watcher.WatcherActivity
import eu.kanade.tachiyomi.ui.watcher.model.EpisodeTransition
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.ui.watcher.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.watcher.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.watcher.viewer.ViewerNavigation.NavigationRegion
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of a [BaseViewer] to display pages with a [RecyclerView].
 */
class WebtoonViewer(val activity: WatcherActivity, val isContinuous: Boolean = true) : BaseViewer {

    private val scope = MainScope()

    /**
     * Recycler view used by this viewer.
     */
    val recycler = WebtoonRecyclerView(activity)

    /**
     * Frame containing the recycler view.
     */
    private val frame = WebtoonFrame(activity)

    /**
     * Layout manager of the recycler view.
     */
    private val layoutManager = WebtoonLayoutManager(activity)

    /**
     * Adapter of the recycler view.
     */
    private val adapter = WebtoonAdapter(this)

    /**
     * Distance to scroll when the user taps on one side of the recycler view.
     */
    private var scrollDistance = activity.resources.displayMetrics.heightPixels * 3 / 4

    /**
     * Currently active item. It can be a episode page or a episode transition.
     */
    private var currentPage: Any? = null

    /**
     * Configuration used by this viewer, like allow taps, or crop image borders.
     */
    val config = WebtoonConfig(scope)

    /**
     * Subscriptions to keep while this viewer is used.
     */
    val subscriptions = CompositeSubscription()

    init {
        recycler.isVisible = false // Don't let the recycler layout yet
        recycler.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        recycler.itemAnimator = null
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
        recycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    onScrolled()

                    if (dy < 0) {
                        val firstIndex = layoutManager.findFirstVisibleItemPosition()
                        val firstItem = adapter.items.getOrNull(firstIndex)
                        if (firstItem is EpisodeTransition.Prev && firstItem.to != null) {
                            activity.requestPreloadEpisode(firstItem.to)
                        }
                    }
                }
            }
        )
        recycler.tapListener = f@{ event ->
            if (!config.tappingEnabled) {
                activity.toggleMenu()
                return@f
            }

            val pos = PointF(event.rawX / recycler.width, event.rawY / recycler.height)
            if (!config.tappingEnabled) activity.toggleMenu()
            else {
                val navigator = config.navigator
                when (navigator.getAction(pos)) {
                    NavigationRegion.MENU -> activity.toggleMenu()
                    NavigationRegion.NEXT, NavigationRegion.RIGHT -> scrollDown()
                    NavigationRegion.PREV, NavigationRegion.LEFT -> scrollUp()
                }
            }
        }
        recycler.longTapListener = f@{ event ->
            if (activity.menuVisible || config.longTapEnabled) {
                val child = recycler.findChildViewUnder(event.x, event.y)
                if (child != null) {
                    val position = recycler.getChildAdapterPosition(child)
                    val item = adapter.items.getOrNull(position)
                    if (item is WatcherPage) {
                        activity.onPageLongTap(item)
                        return@f true
                    }
                }
            }
            false
        }

        config.imagePropertyChangedListener = {
            refreshAdapter()
        }

        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayOnStart || config.forceNavigationOverlay
            activity.binding.navigationOverlay.setNavigation(config.navigator, showOnStart)
        }

        frame.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        frame.addView(recycler)
    }

    private fun checkAllowPreload(page: WatcherPage?): Boolean {
        // Page is transition page - preload allowed
        page ?: return true

        // Initial opening - preload allowed
        currentPage ?: return true

        val nextItem = adapter.items.getOrNull(adapter.items.size - 1)
        val nextEpisode = (nextItem as? EpisodeTransition.Next)?.to ?: (nextItem as? WatcherPage)?.episode

        // Allow preload for
        // 1. Going between pages of same episode
        // 2. Next episode page
        return when (page.episode) {
            (currentPage as? WatcherPage)?.episode -> true
            nextEpisode -> true
            else -> false
        }
    }

    /**
     * Returns the view this viewer uses.
     */
    override fun getView(): View {
        return frame
    }

    /**
     * Destroys this viewer. Called when leaving the watcher or swapping viewers.
     */
    override fun destroy() {
        super.destroy()
        scope.cancel()
        subscriptions.unsubscribe()
    }

    /**
     * Called from the RecyclerView listener when a [page] is marked as active. It notifies the
     * activity of the change and requests the preload of the next episode if this is the last page.
     */
    private fun onPageSelected(page: WatcherPage, allowPreload: Boolean) {
        val pages = page.episode.pages ?: return
        Timber.d("onPageSelected: ${page.number}/${pages.size}")
        activity.onPageSelected(page)

        // Preload next episode once we're within the last 5 pages of the current episode
        val inPreloadRange = pages.size - page.number < 5
        if (inPreloadRange && allowPreload && page.episode == adapter.currentEpisode) {
            Timber.d("Request preload next episode because we're at page ${page.number} of ${pages.size}")
            val nextItem = adapter.items.getOrNull(adapter.items.size - 1)
            val transitionEpisode = (nextItem as? EpisodeTransition.Next)?.to ?: (nextItem as?WatcherPage)?.episode
            if (transitionEpisode != null) {
                Timber.d("Requesting to preload episode ${transitionEpisode.episode.episode_number}")
                activity.requestPreloadEpisode(transitionEpisode)
            }
        }
    }

    /**
     * Called from the RecyclerView listener when a [transition] is marked as active. It request the
     * preload of the destination episode of the transition.
     */
    private fun onTransitionSelected(transition: EpisodeTransition) {
        Timber.d("onTransitionSelected: $transition")
        val toEpisode = transition.to
        if (toEpisode != null) {
            Timber.d("Request preload destination episode because we're on the transition")
            activity.requestPreloadEpisode(toEpisode)
        } else if (transition is EpisodeTransition.Next) {
            // No more episodes, show menu because the user is probably going to close the watcher
            activity.showMenu()
        }
    }

    /**
     * Tells this viewer to set the given [episodes] as active.
     */
    override fun setEpisodes(episodes: ViewerEpisodes) {
        Timber.d("setEpisodes")
        val forceTransition = config.alwaysShowEpisodeTransition || currentPage is EpisodeTransition
        adapter.setEpisodes(episodes, forceTransition)

        if (recycler.isGone) {
            Timber.d("Recycler first layout")
            val pages = episodes.currEpisode.pages ?: return
            moveToPage(pages[min(episodes.currEpisode.requestedPage, pages.lastIndex)])
            recycler.isVisible = true
        }
    }

    /**
     * Tells this viewer to move to the given [page].
     */
    override fun moveToPage(page: WatcherPage) {
        Timber.d("moveToPage")
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            recycler.scrollToPosition(position)
            if (layoutManager.findLastEndVisibleItemPosition() == -1) {
                onScrolled(position)
            }
        } else {
            Timber.d("Page $page not found in adapter")
        }
    }

    fun onScrolled(pos: Int? = null) {
        val position = pos ?: layoutManager.findLastEndVisibleItemPosition()
        val item = adapter.items.getOrNull(position)
        val allowPreload = checkAllowPreload(item as? WatcherPage)
        if (item != null && currentPage != item) {
            currentPage = item
            when (item) {
                is WatcherPage -> onPageSelected(item, allowPreload)
                is EpisodeTransition -> onTransitionSelected(item)
            }
        }
    }

    /**
     * Scrolls up by [scrollDistance].
     */
    private fun scrollUp() {
        if (config.usePageTransitions) {
            recycler.smoothScrollBy(0, -scrollDistance)
        } else {
            recycler.scrollBy(0, -scrollDistance)
        }
    }

    /**
     * Scrolls down by [scrollDistance].
     */
    private fun scrollDown() {
        if (config.usePageTransitions) {
            recycler.smoothScrollBy(0, scrollDistance)
        } else {
            recycler.scrollBy(0, scrollDistance)
        }
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) scrollDown() else scrollUp()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) scrollUp() else scrollDown()
                }
            }
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) scrollUp()

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) scrollDown()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        return false
    }

    /**
     * Notifies adapter of changes around the current page to trigger a relayout in the recycler.
     * Used when an image configuration is changed.
     */
    private fun refreshAdapter() {
        val position = layoutManager.findLastEndVisibleItemPosition()
        adapter.notifyItemRangeChanged(
            max(0, position - 3),
            min(position + 3, adapter.itemCount - 1)
        )
    }
}
