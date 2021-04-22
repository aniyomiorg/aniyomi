package eu.kanade.tachiyomi.ui.watcher.viewer

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import eu.kanade.tachiyomi.ui.watcher.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage

/**
 * Interface for implementing a viewer.
 */
interface BaseViewer {

    /**
     * Returns the view this viewer uses.
     */
    fun getView(): View

    /**
     * Destroys this viewer. Called when leaving the watcher or swapping viewers.
     */
    fun destroy() {}

    /**
     * Tells this viewer to set the given [episodes] as active.
     */
    fun setEpisodes(episodes: ViewerEpisodes)

    /**
     * Tells this viewer to move to the given [page].
     */
    fun moveToPage(page: WatcherPage)

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    fun handleGenericMotionEvent(event: MotionEvent): Boolean
}
