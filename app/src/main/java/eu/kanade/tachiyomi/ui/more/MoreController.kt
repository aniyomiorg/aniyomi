package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.ComposeController
import eu.kanade.tachiyomi.ui.base.controller.NoAppBarElevationController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.download.DownloadTabsController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.recent.HistoryTabsController
import eu.kanade.tachiyomi.ui.recent.UpdatesTabsController
import eu.kanade.tachiyomi.ui.setting.SettingsBackupController
import eu.kanade.tachiyomi.ui.setting.SettingsMainController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.ui.animecategory.CategoryController as AnimeCategoryController

class MoreController :
    ComposeController<MorePresenter>(),
    RootController,
    NoAppBarElevationController {

    private val preferences: PreferencesHelper = Injekt.get()

    override fun getTitle() = resources?.getString(R.string.label_more)

    override fun createPresenter() = MorePresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        MoreScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickHistory = {
                val targetController = when (preferences.bottomNavStyle()) {
                    1 -> UpdatesTabsController()
                    2 -> LibraryController()
                    else -> HistoryTabsController()
                }
                router.pushController(targetController)
            },
            onClickDownloadQueue = { router.pushController(DownloadTabsController()) },
            onClickAnimeCategories = { router.pushController(AnimeCategoryController()) },
            onClickCategories = { router.pushController(CategoryController()) },
            onClickBackupAndRestore = { router.pushController(SettingsBackupController()) },
            onClickSettings = { router.pushController(SettingsMainController()) },
            onClickAbout = { router.pushController(AboutController()) },
        )
    }

    companion object {
        const val URL_HELP = "https://aniyomi.jmir.xyz/help/"
    }
}
