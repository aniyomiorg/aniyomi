package eu.kanade.tachiyomi.ui.player

import android.view.GestureDetector
import android.view.MotionEvent

class Gestures(
    private val activity: PlayerActivity,
    private val width: Float,
    private val height: Float
) : GestureDetector.SimpleOnGestureListener() {
    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (e.y < height * 0.05F || e.y > height * 0.95F) return false
        when {
            e.x < width * 0.4F -> return false
            e.x > width * 0.6F -> return false
            else -> activity.toggleControls()
        }
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (e.y < height * 0.05F || e.y > height * 0.95F) return false
        when {
            e.x < width * 0.4F -> activity.toggleControls()
            e.x > width * 0.6F -> activity.toggleControls()
            else -> return false
        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (e.y < height * 0.05F || e.y > height * 0.95F) return false
        when {
            e.x < width * 0.4F -> activity.doubleTapSeek(-10, e)
            e.x > width * 0.6F -> activity.doubleTapSeek(10, e)
            else -> return false
        }
        return true
    }
}
