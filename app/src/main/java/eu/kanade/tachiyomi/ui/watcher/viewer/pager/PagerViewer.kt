package eu.kanade.tachiyomi.ui.watcher.viewer.pager

import android.graphics.PointF
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.viewpager.widget.ViewPager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.watcher.WatcherActivity
import eu.kanade.tachiyomi.ui.watcher.model.EpisodeTransition
import eu.kanade.tachiyomi.ui.watcher.model.InsertPage
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.ui.watcher.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.watcher.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.watcher.viewer.ViewerNavigation.NavigationRegion
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import timber.log.Timber
import kotlin.math.min

/**
 * Implementation of a [BaseViewer] to display pages with a [ViewPager].
 */
@Suppress("LeakingThis")
abstract class PagerViewer(val activity: WatcherActivity) : BaseViewer {

    private val scope = MainScope()

    /**
     * View pager used by this viewer. It's abstract to implement L2R, R2L and vertical pagers on
     * top of this class.
     */
    val pager = createPager()

    /**
     * Configuration used by the pager, like allow taps, scale mode on images, page transitions...
     */
    val config = PagerConfig(this, scope)

    /**
     * Adapter of the pager.
     */
    private val adapter = PagerViewerAdapter(this)

    /**
     * Currently active item. It can be a episode page or a episode transition.
     */
    private var currentPage: Any? = null

    /**
     * Viewer episodes to set when the pager enters idle mode. Otherwise, if the view was settling
     * or dragging, there'd be a noticeable and annoying jump.
     */
    private var awaitingIdleViewerEpisodes: ViewerEpisodes? = null

    /**
     * Whether the view pager is currently in idle mode. It sets the awaiting episodes if setting
     * this field to true.
     */
    private var isIdle = true
        set(value) {
            field = value
            if (value) {
                awaitingIdleViewerEpisodes?.let {
                    setEpisodesInternal(it)
                    awaitingIdleViewerEpisodes = null
                }
            }
        }

    init {
        pager.isVisible = false // Don't layout the pager yet
        pager.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        pager.offscreenPageLimit = 1
        pager.id = R.id.watcher_pager
        pager.adapter = adapter
        pager.addOnPageChangeListener(
            object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    onPageChange(position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    isIdle = state == ViewPager.SCROLL_STATE_IDLE
                }
            }
        )
        pager.tapListener = f@{ event ->
            if (!config.tappingEnabled) {
                activity.toggleMenu()
                return@f
            }

            val pos = PointF(event.rawX / pager.width, event.rawY / pager.height)
            val navigator = config.navigator
            when (navigator.getAction(pos)) {
                NavigationRegion.MENU -> activity.toggleMenu()
                NavigationRegion.NEXT -> moveToNext()
                NavigationRegion.PREV -> moveToPrevious()
                NavigationRegion.RIGHT -> moveRight()
                NavigationRegion.LEFT -> moveLeft()
            }
        }
        pager.longTapListener = f@{
            if (activity.menuVisible || config.longTapEnabled) {
                val item = adapter.items.getOrNull(pager.currentItem)
                if (item is WatcherPage) {
                    activity.onPageLongTap(item)
                    return@f true
                }
            }
            false
        }

        config.dualPageSplitChangedListener = { enabled ->
            if (!enabled) {
                cleanupPageSplit()
            }
        }

        config.imagePropertyChangedListener = {
            refreshAdapter()
        }

        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayOnStart || config.forceNavigationOverlay
            activity.binding.navigationOverlay.setNavigation(config.navigator, showOnStart)
        }
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

    /**
     * Creates a new ViewPager.
     */
    abstract fun createPager(): Pager

    /**
     * Returns the view this viewer uses.
     */
    override fun getView(): View {
        return pager
    }

    /**
     * Called when a new page (either a [WatcherPage] or [EpisodeTransition]) is marked as active
     */
    private fun onPageChange(position: Int) {
        val page = adapter.items.getOrNull(position)
        if (page != null && currentPage != page) {
            val allowPreload = checkAllowPreload(page as? WatcherPage)
            currentPage = page
            when (page) {
                is WatcherPage -> onWatcherPageSelected(page, allowPreload)
                is EpisodeTransition -> onTransitionSelected(page)
            }
        }
    }

    private fun checkAllowPreload(page: WatcherPage?): Boolean {
        // Page is transition page - preload allowed
        page ?: return true

        // Initial opening - preload allowed
        currentPage ?: return true

        // Allow preload for
        // 1. Going to next episode from episode transition
        // 2. Going between pages of same episode
        // 3. Next episode page
        return when (page.episode) {
            (currentPage as? EpisodeTransition.Next)?.to -> true
            (currentPage as? WatcherPage)?.episode -> true
            adapter.nextTransition?.to -> true
            else -> false
        }
    }

    /**
     * Called when a [WatcherPage] is marked as active. It notifies the
     * activity of the change and requests the preload of the next episode if this is the last page.
     */
    private fun onWatcherPageSelected(page: WatcherPage, allowPreload: Boolean) {
        val pages = page.episode.pages ?: return
        Timber.d("onWatcherPageSelected: ${page.number}/${pages.size}")
        activity.onPageSelected(page)

        // Preload next episode once we're within the last 5 pages of the current episode
        val inPreloadRange = pages.size - page.number < 5
        if (inPreloadRange && allowPreload && page.episode == adapter.currentEpisode) {
            Timber.d("Request preload next episode because we're at page ${page.number} of ${pages.size}")
            adapter.nextTransition?.to?.let {
                activity.requestPreloadEpisode(it)
            }
        }
    }

    /**
     * Called when a [EpisodeTransition] is marked as active. It request the
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
     * Tells this viewer to set the given [episodes] as active. If the pager is currently idle,
     * it sets the episodes immediately, otherwise they are saved and set when it becomes idle.
     */
    override fun setEpisodes(episodes: ViewerEpisodes) {
        if (isIdle) {
            setEpisodesInternal(episodes)
        } else {
            awaitingIdleViewerEpisodes = episodes
        }
    }

    /**
     * Sets the active [episodes] on this pager.
     */
    private fun setEpisodesInternal(episodes: ViewerEpisodes) {
        Timber.d("setEpisodesInternal")
        val forceTransition = config.alwaysShowEpisodeTransition || adapter.items.getOrNull(pager.currentItem) is EpisodeTransition
        adapter.setEpisodes(episodes, forceTransition)

        // Layout the pager once a episode is being set
        if (pager.isGone) {
            Timber.d("Pager first layout")
            val pages = episodes.currEpisode.pages ?: return
            moveToPage(pages[min(episodes.currEpisode.requestedPage, pages.lastIndex)])
            pager.isVisible = true
        }
    }

    /**
     * Tells this viewer to move to the given [page].
     */
    override fun moveToPage(page: WatcherPage) {
        Timber.d("moveToPage ${page.number}")
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            val currentPosition = pager.currentItem
            pager.setCurrentItem(position, true)
            // manually call onPageChange since ViewPager listener is not triggered in this case
            if (currentPosition == position) {
                onPageChange(position)
            }
        } else {
            Timber.d("Page $page not found in adapter")
        }
    }

    /**
     * Moves to the next page.
     */
    open fun moveToNext() {
        moveRight()
    }

    /**
     * Moves to the previous page.
     */
    open fun moveToPrevious() {
        moveLeft()
    }

    /**
     * Moves to the page at the right.
     */
    protected open fun moveRight() {
        if (pager.currentItem != adapter.count - 1) {
            pager.setCurrentItem(pager.currentItem + 1, config.usePageTransitions)
        }
    }

    /**
     * Moves to the page at the left.
     */
    protected open fun moveLeft() {
        if (pager.currentItem != 0) {
            pager.setCurrentItem(pager.currentItem - 1, config.usePageTransitions)
        }
    }

    /**
     * Moves to the page at the top (or previous).
     */
    protected open fun moveUp() {
        moveToPrevious()
    }

    /**
     * Moves to the page at the bottom (or next).
     */
    protected open fun moveDown() {
        moveToNext()
    }

    /**
     * Resets the adapter in order to recreate all the views. Used when a image configuration is
     * changed.
     */
    private fun refreshAdapter() {
        val currentItem = pager.currentItem
        pager.adapter = adapter
        pager.setCurrentItem(currentItem, false)
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP
        val ctrlPressed = event.metaState.and(KeyEvent.META_CTRL_ON) > 0

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveDown() else moveUp()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveUp() else moveDown()
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isUp) {
                    if (ctrlPressed) moveToNext() else moveRight()
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isUp) {
                    if (ctrlPressed) moveToPrevious() else moveLeft()
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_DPAD_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
            when (event.action) {
                MotionEvent.ACTION_SCROLL -> {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        moveDown()
                    } else {
                        moveUp()
                    }
                    return true
                }
            }
        }
        return false
    }

    fun onPageSplit(currentPage: WatcherPage, newPage: InsertPage) {
        adapter.onPageSplit(currentPage, newPage, this::class.java)
    }

    private fun cleanupPageSplit() {
        adapter.cleanupPageSplit()
    }
}
