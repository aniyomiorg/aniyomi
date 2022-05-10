package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.viewpager.RouterPagerAdapter
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PagerControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.widget.listener.SimpleTabSelectedListener
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.ui.download.anime.DownloadController as AnimeDownloadController
import eu.kanade.tachiyomi.ui.download.manga.DownloadController as MangaDownloadController

class DownloadTabsController :
    RxController<PagerControllerBinding>(),
    RootController,
    TabbedController,
    FabController {

    private var adapter: DownloadTabsAdapter? = null

    private val preferences: PreferencesHelper by injectLazy()

    private val animeController = AnimeDownloadController()

    private val mangaController = MangaDownloadController()

    private lateinit var thisFab: ExtendedFloatingActionButton

    private var firstOpen = true

    private val onTabSelectedListener = object : SimpleTabSelectedListener() {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            when (tab?.position) {
                DOWNLOAD_CONTROLLER_FIRST -> {
                    if (preferences.switchAnimeManga().get()) {
                        mangaController.configureFab(thisFab)
                        mangaController.selectTab()
                        if (!firstOpen) mangaController.setInformationView()
                    } else {
                        animeController.configureFab(thisFab)
                        animeController.selectTab()
                        if (!firstOpen) animeController.setInformationView()
                    }
                }
                DOWNLOAD_CONTROLLER_SECOND -> {
                    if (preferences.switchAnimeManga().get()) {
                        animeController.configureFab(thisFab)
                        animeController.selectTab()
                        if (!firstOpen) animeController.setInformationView()
                    } else {
                        mangaController.configureFab(thisFab)
                        mangaController.selectTab()
                        if (!firstOpen) mangaController.setInformationView()
                    }
                }
            }
            firstOpen = false
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            thisFab.isVisible = false
            when (tab?.position) {
                DOWNLOAD_CONTROLLER_FIRST -> {
                    if (preferences.switchAnimeManga().get()) {
                        mangaController.unselectTab()
                        mangaController.cleanupFab(thisFab)
                    } else {
                        animeController.unselectTab()
                        animeController.cleanupFab(thisFab)
                    }
                }
                DOWNLOAD_CONTROLLER_SECOND -> {
                    if (preferences.switchAnimeManga().get()) {
                        animeController.unselectTab()
                        animeController.cleanupFab(thisFab)
                    } else {
                        mangaController.unselectTab()
                        mangaController.cleanupFab(thisFab)
                    }
                }
            }
        }
    }

    override fun getTitle(): String {
        return resources!!.getString(R.string.label_download_queue)
    }

    override fun createBinding(inflater: LayoutInflater) = PagerControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        (activity as? MainActivity)?.showBottomNav(false)
        super.onViewCreated(view)

        adapter = DownloadTabsAdapter()
        binding.pager.adapter = adapter

        if (preferences.switchAnimeManga().get()) {
            mangaController.selectTab()
            animeController.unselectTab()
        } else {
            animeController.selectTab()
            mangaController.unselectTab()
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
        (activity as? MainActivity)?.showBottomNav(true)
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
        tabs.addOnTabSelectedListener(onTabSelectedListener)
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }
        return true
    }

    override fun cleanupTabs(tabs: TabLayout) {
        tabs.removeOnTabSelectedListener(onTabSelectedListener)
    }

    private inner class DownloadTabsAdapter : RouterPagerAdapter(this@DownloadTabsController) {

        private val tabTitles =
            if (preferences.switchAnimeManga().get()) listOf(R.string.label_manga, R.string.label_animelib,).map { resources!!.getString(it) }
            else listOf(R.string.label_animelib, R.string.label_manga,).map { resources!!.getString(it) }

        override fun getCount(): Int {
            return tabTitles.size
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val controller: Controller = when (position) {
                    DOWNLOAD_CONTROLLER_FIRST -> if (preferences.switchAnimeManga().get()) mangaController else animeController
                    DOWNLOAD_CONTROLLER_SECOND -> if (preferences.switchAnimeManga().get()) animeController else mangaController
                    else -> error("Wrong position $position")
                }
                router.setRoot(RouterTransaction.with(controller))
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return tabTitles[position]
        }
    }

    override fun configureFab(fab: ExtendedFloatingActionButton) {
        thisFab = fab
    }

    override fun cleanupFab(fab: ExtendedFloatingActionButton) {
        thisFab = fab
        animeController.cleanupFab(thisFab)
        mangaController.cleanupFab(thisFab)
        thisFab.isVisible = false
    }

    companion object {
        const val DOWNLOAD_CONTROLLER_FIRST = 0
        const val DOWNLOAD_CONTROLLER_SECOND = 1
    }
}
