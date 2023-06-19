package eu.kanade.tachiyomi.widget.sheet

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.displayCompat

abstract class PlayerBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    abstract fun createView(inflater: LayoutInflater): View

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = createView(layoutInflater)
        setContentView(rootView)

        // Enforce max width for tablets
        val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
        if (width > 0) {
            behavior.maxWidth = width
        }

        // Set peek height to 50% display height
        context.displayCompat?.let {
            val metrics = DisplayMetrics()
            it.getRealMetrics(metrics)
            behavior.peekHeight = metrics.heightPixels / 2
        }

        val bottomSheet = rootView.parent as ViewGroup
        bottomSheet.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LOW_PROFILE
        val window = window ?: return
        WindowInsetsControllerCompat(window, bottomSheet).hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }
}
