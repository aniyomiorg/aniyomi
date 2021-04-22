package eu.kanade.tachiyomi.ui.watcher

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

/**
 * Seekbar to show current episode progress.
 */
class WatcherSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatSeekBar(context, attrs) {

    /**
     * Whether the seekbar should draw from right to left.
     */
    var isRTL = false

    /**
     * Draws the seekbar, translating the canvas if using a right to left watcher.
     */
    override fun draw(canvas: Canvas) {
        if (isRTL) {
            val px = width / 2f
            val py = height / 2f

            canvas.scale(-1f, 1f, px, py)
        }
        super.draw(canvas)
    }

    /**
     * Handles touch events, translating coordinates if using a right to left watcher.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isRTL) {
            event.setLocation(width - event.x, event.y)
        }
        return super.onTouchEvent(event)
    }
}
