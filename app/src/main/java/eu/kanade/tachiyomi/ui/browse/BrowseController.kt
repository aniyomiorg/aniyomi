package eu.kanade.tachiyomi.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.viewpager.RouterPagerAdapter
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PagerControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.browse.animeextension.AnimeExtensionController
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourceController
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionController
import eu.kanade.tachiyomi.ui.browse.migration.animesources.MigrationAnimeSourcesController
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrationSourcesController
import eu.kanade.tachiyomi.ui.browse.source.SourceController
import eu.kanade.tachiyomi.ui.main.MainActivity
import uy.kohesive.injekt.injectLazy

class BrowseController :
    RxController<PagerControllerBinding>,
    RootController,
    TabbedController {

    constructor(toExtensions: Boolean = false) : super(
        bundleOf(TO_EXTENSIONS_EXTRA to toExtensions),
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getBoolean(TO_EXTENSIONS_EXTRA))

    private val preferences: PreferencesHelper by injectLazy()

    private val toExtensions = args.getBoolean(TO_EXTENSIONS_EXTRA, false)

    val extensionListUpdateRelay: PublishRelay<Boolean> = PublishRelay.create()

    private var adapter: BrowseAdapter? = null

    override fun getTitle(): String {
        return resources!!.getString(R.string.browse)
    }

    override fun createBinding(inflater: LayoutInflater) = PagerControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = BrowseAdapter()
        binding.pager.adapter = adapter

        if (toExtensions) {
            binding.pager.currentItem = EXTENSIONS_CONTROLLER_FIRST
        }
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

                // Show badges on tabs for extension updates
                setExtensionUpdateBadge()
                setAnimeExtensionUpdateBadge()
            }
        }
    }

    override fun configureTabs(tabs: TabLayout): Boolean {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_SCROLLABLE
        }
        return true
    }

    override fun cleanupTabs(tabs: TabLayout) {
        // Remove extension update badge
        tabs.getTabAt(EXTENSIONS_CONTROLLER_FIRST)?.removeBadge()
        tabs.getTabAt(EXTENSIONS_CONTROLLER_SECOND)?.removeBadge()
    }

    fun setExtensionUpdateBadge() {
        /* It's possible to switch to the Library controller by the time setExtensionUpdateBadge
        is called, resulting in a badge being put on the category tabs (if enabled).
        This check prevents that from happening */
        if (router.backstack.lastOrNull()?.controller !is BrowseController) return

        (activity as? MainActivity)?.binding?.tabs?.apply {
            val updates = preferences.extensionUpdatesCount().get()
            if (preferences.switchAnimeManga().get()) {
                if (updates > 0) {
                    val badge: BadgeDrawable? = getTabAt(EXTENSIONS_CONTROLLER_FIRST)?.orCreateBadge
                    badge?.isVisible = true
                } else {
                    getTabAt(EXTENSIONS_CONTROLLER_FIRST)?.removeBadge()
                }
            } else {
                if (updates > 0) {
                    val badge: BadgeDrawable? = getTabAt(EXTENSIONS_CONTROLLER_SECOND)?.orCreateBadge
                    badge?.isVisible = true
                } else {
                    getTabAt(EXTENSIONS_CONTROLLER_SECOND)?.removeBadge()
                }
            }
        }
    }

    fun setAnimeExtensionUpdateBadge() {
        /* It's possible to switch to the Library controller by the time setExtensionUpdateBadge
        is called, resulting in a badge being put on the category tabs (if enabled).
        This check prevents that from happening */
        if (router.backstack.lastOrNull()?.controller !is BrowseController) return

        (activity as? MainActivity)?.binding?.tabs?.apply {
            val updates = preferences.animeextensionUpdatesCount().get()
            if (preferences.switchAnimeManga().get()) {
                if (updates > 0) {
                    val badge: BadgeDrawable? = getTabAt(EXTENSIONS_CONTROLLER_SECOND)?.orCreateBadge
                    badge?.isVisible = true
                } else {
                    getTabAt(EXTENSIONS_CONTROLLER_SECOND)?.removeBadge()
                }
            } else {
                if (updates > 0) {
                    val badge: BadgeDrawable? = getTabAt(EXTENSIONS_CONTROLLER_FIRST)?.orCreateBadge
                    badge?.isVisible = true
                } else {
                    getTabAt(EXTENSIONS_CONTROLLER_FIRST)?.removeBadge()
                }
            }
        }
    }

    private inner class BrowseAdapter : RouterPagerAdapter(this@BrowseController) {

        private val tabTitles =
            if (preferences.switchAnimeManga().get()) {
                listOf(
                    R.string.label_mangasources,
                    R.string.label_animesources,
                    R.string.label_mangaextensions,
                    R.string.label_animeextensions,
                    R.string.label_migration_manga,
                    R.string.label_migration_anime,
                )
                    .map { resources!!.getString(it) }
            } else listOf(
                R.string.label_animesources,
                R.string.label_mangasources,
                R.string.label_animeextensions,
                R.string.label_mangaextensions,
                R.string.label_migration_anime,
                R.string.label_migration_manga,
            )
                .map { resources!!.getString(it) }

        override fun getCount(): Int {
            return tabTitles.size
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val controller: Controller = when (position) {
                    SOURCES_CONTROLLER_FIRST -> if (preferences.switchAnimeManga().get()) SourceController() else AnimeSourceController()
                    SOURCES_CONTROLLER_SECOND -> if (preferences.switchAnimeManga().get()) AnimeSourceController() else SourceController()
                    EXTENSIONS_CONTROLLER_FIRST -> if (preferences.switchAnimeManga().get()) ExtensionController() else AnimeExtensionController()
                    EXTENSIONS_CONTROLLER_SECOND -> if (preferences.switchAnimeManga().get()) AnimeExtensionController() else ExtensionController()
                    MIGRATION_CONTROLLER_FIRST -> if (preferences.switchAnimeManga().get()) MigrationSourcesController() else MigrationAnimeSourcesController()
                    MIGRATION_CONTROLLER_SECOND -> if (preferences.switchAnimeManga().get()) MigrationAnimeSourcesController() else MigrationSourcesController()
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
        const val TO_EXTENSIONS_EXTRA = "to_extensions"

        const val SOURCES_CONTROLLER_FIRST = 0
        const val SOURCES_CONTROLLER_SECOND = 1
        const val EXTENSIONS_CONTROLLER_FIRST = 2
        const val EXTENSIONS_CONTROLLER_SECOND = 3
        const val MIGRATION_CONTROLLER_FIRST = 4
        const val MIGRATION_CONTROLLER_SECOND = 5
    }
}
