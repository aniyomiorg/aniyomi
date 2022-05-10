package eu.kanade.tachiyomi.ui.recent

import android.view.LayoutInflater
import android.view.View
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.viewpager.RouterPagerAdapter
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PagerControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.recent.animehistory.AnimeHistoryController
import eu.kanade.tachiyomi.ui.recent.history.HistoryController
import uy.kohesive.injekt.injectLazy

class HistoryTabsController() :
    RxController<PagerControllerBinding>(),
    RootController,
    TabbedController {

    private var adapter: HistoryTabsAdapter? = null

    private val preferences: PreferencesHelper by injectLazy()

    override fun getTitle(): String {
        return resources!!.getString(R.string.history)
    }

    override fun createBinding(inflater: LayoutInflater) = PagerControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = HistoryTabsAdapter()
        binding.pager.adapter = adapter
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            (activity as? MainActivity)?.binding?.tabs?.apply {
                setupWithViewPager(binding.pager)
            }
        }
    }

    override fun configureTabs(tabs: TabLayout): Boolean {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }
        return true
    }

    private inner class HistoryTabsAdapter : RouterPagerAdapter(this@HistoryTabsController) {

        private val tabTitles =
            if (preferences.switchAnimeManga().get()) listOf(R.string.label_history, R.string.label_animehistory,).map { resources!!.getString(it) }
            else listOf(R.string.label_animehistory, R.string.label_history,).map { resources!!.getString(it) }

        override fun getCount(): Int {
            return tabTitles.size
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val controller: Controller = when (position) {
                    HISTORY_CONTROLLER_FIRST -> if (preferences.switchAnimeManga().get()) HistoryController() else AnimeHistoryController()
                    HISTORY_CONTROLLER_SECOND -> if (preferences.switchAnimeManga().get()) AnimeHistoryController() else HistoryController()
                    else -> error("Wrong position $position")
                }
                router.setRoot(RouterTransaction.with(controller))
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return tabTitles[position]
        }
    }

    companion object {
        const val HISTORY_CONTROLLER_FIRST = 0
        const val HISTORY_CONTROLLER_SECOND = 1
    }
}
