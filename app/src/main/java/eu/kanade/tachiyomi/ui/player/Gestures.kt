package eu.kanade.tachiyomi.ui.player

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy
import kotlin.math.abs

class Gestures(
    private val activity: PlayerActivity,
    private val width: Float,
    private val height: Float
) : GestureDetector.SimpleOnGestureListener(), View.OnTouchListener {
    private var scrollState = STATE_UP

    private val trigger = width.coerceAtMost(height) / 30

    private val preferences: PreferencesHelper by injectLazy()

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
        val interval = preferences.skipLengthPreference()
        when {
            e.x < width * 0.4F -> activity.doubleTapSeek(-interval, e)
            e.x > width * 0.6F -> activity.doubleTapSeek(interval, e)
            else -> return false
        }
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (e1.y < height * 0.05F || e1.y > height * 0.95F) return false
        val dx = e1.x - e2.x
        val dy = e1.y - e2.y
        when (scrollState) {
            STATE_UP -> {
                if (abs(dx) >= trigger) {
                    scrollState = STATE_HORIZONTAL
                    activity.initSeek()
                } else if (abs(dy) > trigger) {
                    scrollState = when {
                        e1.x > width * 0.6F -> STATE_VERTICAL_R
                        e1.x < width * 0.4F -> STATE_VERTICAL_L
                        else -> STATE_UP
                    }
                }
            }
            STATE_VERTICAL_L -> {
                activity.verticalScrollLeft(1.5F * distanceY / height)
            }
            STATE_VERTICAL_R -> {
                activity.verticalScrollRight(1.5F * distanceY / height)
            }
            STATE_HORIZONTAL -> {
                activity.horizontalScroll(150F * -dx / width)
            }
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (scrollState != STATE_UP) {
                scrollState = STATE_UP
            }
        }
        return false
    }
}

private const val STATE_UP = 0
private const val STATE_HORIZONTAL = 1
private const val STATE_VERTICAL_L = 2
private const val STATE_VERTICAL_R = 3
