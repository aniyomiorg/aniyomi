package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.ComposeController
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
    FullComposeController<MorePresenter>(),
    RootController {

    private val preferences: PreferencesHelper = Injekt.get()

    override fun createPresenter() = MorePresenter()

    @Composable
    override fun ComposeContent() {
        MoreScreen(
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
            onClickBackupAndRestore = { router.pushController(SettingsMainController.toBackupScreen()) },
            onClickSettings = { router.pushController(SettingsMainController()) },
            onClickAbout = { router.pushController(SettingsMainController.toAboutScreen()) },
        )
    }

    companion object {
        const val URL_HELP = "https://aniyomi.jmir.xyz/help/"
    }
}
