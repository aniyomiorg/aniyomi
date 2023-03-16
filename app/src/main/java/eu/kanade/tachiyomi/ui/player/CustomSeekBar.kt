package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import com.google.android.material.R
import eu.kanade.tachiyomi.util.Stamp
import eu.kanade.tachiyomi.util.system.getResourceColor

/*
* Seek bar with Lines on it on specific time
* Code based on https://github.com/qgustavor/mpv-android/blob/master/app/src/main/java/is/xyz/mpv/SeekBarWithTicks.kt
* Licensed under MIT - https://github.com/qgustavor/mpv-android/blob/master/LICENSE
*/

class CustomSeekBar : AppCompatSeekBar {
    private var stamps: List<Stamp>? = null
    private val paint = Paint()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        paint.color = context.getResourceColor(R.attr.colorTertiaryContainer)
        paint.alpha = 255
        // Use the thumb height for consistency
        paint.strokeWidth = (thumb?.intrinsicHeight ?: thumb?.intrinsicWidth)?.div(8F) ?: 2.2F
    }

    fun setStamps(newStamps: List<Stamp>?) {
        stamps = newStamps
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.let { canv ->
            val scale = (width - paddingLeft - paddingRight) / max.toFloat()
            val baseX = paddingLeft.toFloat()
            val baseY = height / 2f

            stamps?.forEach { stamp ->
                val startX = (baseX + scale * stamp.interval.startTime).toFloat()
                val endX = (baseX + scale * stamp.interval.endTime).toFloat()
                canv.drawLine(
                    startX,
                    baseY,
                    endX,
                    baseY,
                    paint,
                )
            }

            val mThumb = thumb
            if (mThumb != null) {
                val mThumbOffset = thumbOffset
                val saveCount = canvas.save()
                val mPaddingLeft = paddingLeft
                val mPaddingTop = paddingTop
                // Translate the padding. For the x, we need to allow the thumb to
                // draw in its extra space
                canvas.translate((mPaddingLeft - mThumbOffset).toFloat(), mPaddingTop.toFloat())
                mThumb.draw(canvas)
                canvas.restoreToCount(saveCount)
            }
        }
    }
}
