package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import android.view.View
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PagerControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.download.anime.DownloadController as AnimeDownloadController
import eu.kanade.tachiyomi.ui.download.manga.DownloadController as MangaDownloadController

class DownloadTabsController() :
    RxController<PagerControllerBinding>(),
    RootController,
    TabbedController {

    private var adapter: DownloadTabsAdapter? = null

    override fun getTitle(): String {
        return resources!!.getString(R.string.label_download_queue)
    }

    override fun createBinding(inflater: LayoutInflater) = PagerControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        (activity as? MainActivity)?.showBottomNav(visible = false, collapse = true)
        super.onViewCreated(view)

        adapter = DownloadTabsAdapter()
        binding.pager.adapter = adapter
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
        (activity as? MainActivity)?.showBottomNav(visible = true, collapse = true)
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
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }
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
                    ANIMEDOWNLOAD_CONTROLLER -> AnimeDownloadController()
                    MANGADOWNLOAD_CONTROLLER -> MangaDownloadController()
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
        const val ANIMEDOWNLOAD_CONTROLLER = 0
        const val MANGADOWNLOAD_CONTROLLER = 1
    }
}
