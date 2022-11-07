package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.navigationrail.NavigationRailView
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TachiyomiNavigationRailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : NavigationRailView(context, attrs) {

    override fun inflateMenu(resId: Int) {
        val libraryPreferences: LibraryPreferences = Injekt.get()
        when (libraryPreferences.bottomNavStyle().get()) {
            1 -> super.inflateMenu(R.menu.main_nav_history)
            2 -> super.inflateMenu(R.menu.main_nav_no_manga)
            else -> super.inflateMenu(resId)
        }
    }
}
