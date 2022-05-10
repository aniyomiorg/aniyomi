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
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesController
import eu.kanade.tachiyomi.ui.recent.updates.UpdatesController
import uy.kohesive.injekt.injectLazy

class UpdatesTabsController() :
    RxController<PagerControllerBinding>(),
    RootController,
    TabbedController {

    private var adapter: UpdatesTabsAdapter? = null

    private val preferences: PreferencesHelper by injectLazy()

    override fun getTitle(): String {
        return resources!!.getString(R.string.label_recent_updates)
    }

    override fun createBinding(inflater: LayoutInflater) = PagerControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = UpdatesTabsAdapter()
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

    private inner class UpdatesTabsAdapter : RouterPagerAdapter(this@UpdatesTabsController) {

        private val tabTitles =
            if (preferences.switchAnimeManga().get()) listOf(R.string.label_updates, R.string.label_animeupdates,).map { resources!!.getString(it) }
            else listOf(R.string.label_animeupdates, R.string.label_updates,).map { resources!!.getString(it) }

        override fun getCount(): Int {
            return tabTitles.size
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val controller: Controller = when (position) {
                    UPDATES_CONTROLLER_FIRST -> if (preferences.switchAnimeManga().get()) UpdatesController() else AnimeUpdatesController()
                    UPDATES_CONTROLLER_SECOND -> if (preferences.switchAnimeManga().get()) AnimeUpdatesController() else UpdatesController()
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
        const val UPDATES_CONTROLLER_FIRST = 0
        const val UPDATES_CONTROLLER_SECOND = 1
    }
}
