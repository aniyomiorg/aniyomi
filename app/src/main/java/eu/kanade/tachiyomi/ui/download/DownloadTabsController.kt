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
import eu.kanade.tachiyomi.databinding.PagerControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.widget.listener.SimpleTabSelectedListener
import eu.kanade.tachiyomi.ui.download.anime.DownloadController as AnimeDownloadController
import eu.kanade.tachiyomi.ui.download.manga.DownloadController as MangaDownloadController

class DownloadTabsController :
    RxController<PagerControllerBinding>(),
    RootController,
    TabbedController,
    FabController {

    private var adapter: DownloadTabsAdapter? = null

    private val animeController = AnimeDownloadController()

    private val mangaController = MangaDownloadController()

    private lateinit var thisFab: ExtendedFloatingActionButton

    private var firstOpen = true

    private val onTabSelectedListener = object : SimpleTabSelectedListener() {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            when (tab?.position) {
                ANIMEDOWNLOAD_CONTROLLER -> {
                    animeController.configureFab(thisFab)
                    animeController.selectTab()
                    if (!firstOpen) animeController.setInformationView()
                }
                MANGADOWNLOAD_CONTROLLER -> {
                    mangaController.configureFab(thisFab)
                    mangaController.selectTab()
                    if (!firstOpen) mangaController.setInformationView()
                }
            }
            firstOpen = false
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            thisFab.isVisible = false
            when (tab?.position) {
                ANIMEDOWNLOAD_CONTROLLER -> {
                    animeController.unselectTab()
                    animeController.cleanupFab(thisFab)
                }
                MANGADOWNLOAD_CONTROLLER -> {
                    mangaController.unselectTab()
                    mangaController.cleanupFab(thisFab)
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

    override fun configureTabs(tabs: TabLayout) {
        tabs.addOnTabSelectedListener(onTabSelectedListener)
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }
    }

    override fun cleanupTabs(tabs: TabLayout) {
        tabs.removeOnTabSelectedListener(onTabSelectedListener)
    }

    private inner class DownloadTabsAdapter : RouterPagerAdapter(this@DownloadTabsController) {

        private val tabTitles = listOf(
            R.string.label_animehistory,
            R.string.label_history
        )
            .map { resources!!.getString(it) }

        override fun getCount(): Int {
            return tabTitles.size
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val controller: Controller = when (position) {
                    ANIMEDOWNLOAD_CONTROLLER -> animeController
                    MANGADOWNLOAD_CONTROLLER -> mangaController
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
        const val ANIMEDOWNLOAD_CONTROLLER = 0
        const val MANGADOWNLOAD_CONTROLLER = 1
    }
}
