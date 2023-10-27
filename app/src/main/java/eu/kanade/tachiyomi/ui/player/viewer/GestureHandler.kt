package eu.kanade.tachiyomi.ui.player.viewer

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import uy.kohesive.injekt.injectLazy
import kotlin.math.abs

class GestureHandler(
    private val activity: PlayerActivity,
    private val width: Float,
    private val height: Float,
) : GestureDetector.SimpleOnGestureListener(), View.OnTouchListener {
    private var scrollState = STATE_UP

    private val trigger = width.coerceAtMost(height) / 25

    private val preferences: PlayerPreferences by injectLazy()

    val interval = preferences.skipLengthPreference().get()

    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    private val playerControls = activity.playerControls

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (SeekState.mode == SeekState.LOCKED || SeekState.mode != SeekState.DOUBLE_TAP || activity.player.timePos == null || activity.player.duration == null) return false
        when {
            e.x < width * 0.4F && interval != 0 -> if (activity.player.timePos!! > 0) activity.doubleTapSeek(-interval, e) else return false
            e.x > width * 0.6F && interval != 0 -> if (activity.player.timePos!! < activity.player.duration!!) activity.doubleTapSeek(interval, e) else return false
            else -> return false
        }
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (SeekState.mode != SeekState.DOUBLE_TAP) playerControls.toggleControls(isTapped = true)
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (SeekState.mode == SeekState.LOCKED) { playerControls.toggleControls(); return false }
        if (activity.player.timePos == null || activity.player.duration == null) return false
        when {
            e.x < width * 0.4F && interval != 0 -> if (activity.player.timePos!! > 0) activity.doubleTapSeek(-interval, e) else return false
            e.x > width * 0.6F && interval != 0 -> if (activity.player.timePos!! < activity.player.duration!!) activity.doubleTapSeek(interval, e) else return false
            else -> activity.doubleTapPlayPause()
        }
        return true
    }

    private var scrollDiff: Float? = null

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        if (SeekState.mode == SeekState.LOCKED) { playerControls.toggleControls(); return false }
        if (e1.y < height * 0.05F || e1.y > height * 0.95F) return false
        val dx = e1.x - e2.x
        val dy = e1.y - e2.y
        when (scrollState) {
            STATE_UP -> {
                if (abs(dx) >= trigger) {
                    if (e1.x < width * 0.05F || e1.x > width * 0.95F) return false
                    scrollState = STATE_HORIZONTAL
                    activity.initSeek()
                } else if (abs(dy) > trigger) {
                    scrollState = when {
                        e1.x > width * 0.6F -> STATE_VERTICAL_RIGHT
                        e1.x < width * 0.4F -> STATE_VERTICAL_LEFT
                        else -> STATE_UP
                    }
                }
            }
            STATE_VERTICAL_LEFT -> {
                val diff = 1.5F * distanceY / height
                if (preferences.gestureVolumeBrightness().get()) activity.verticalScrollLeft(diff)
            }
            STATE_VERTICAL_RIGHT -> {
                val diff = 1.5F * distanceY / height
                if (preferences.gestureVolumeBrightness().get()) activity.verticalScrollRight(diff)
            }
            STATE_HORIZONTAL -> {
                val diff = 150F * -dx / width
                scrollDiff = diff
                if (preferences.gestureHorizontalSeek().get()) activity.horizontalScroll(diff)
            }
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (scrollState == STATE_HORIZONTAL) {
                scrollDiff?.let { if (preferences.gestureHorizontalSeek().get()) activity.horizontalScroll(it, final = true) }
                scrollDiff = null
                playerControls.resetControlsFade()
            }
            if (scrollState != STATE_UP) {
                scrollState = STATE_UP
            }
        }
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        if (SeekState.mode == SeekState.LOCKED) { playerControls.toggleControls(); return }
        activity.viewModel.showScreenshotOptions()
    }
}

private const val STATE_UP = 0
private const val STATE_HORIZONTAL = 1
private const val STATE_VERTICAL_LEFT = 2
private const val STATE_VERTICAL_RIGHT = 3
