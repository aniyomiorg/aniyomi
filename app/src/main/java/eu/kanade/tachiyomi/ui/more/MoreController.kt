package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.tachiyomi.ui.HistoryTabsController
import eu.kanade.tachiyomi.ui.UpdatesTabsController
import eu.kanade.tachiyomi.ui.animecategory.AnimeCategoryController
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.download.anime.AnimeDownloadController
import eu.kanade.tachiyomi.ui.download.manga.DownloadController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.setting.SettingsMainController
import eu.kanade.tachiyomi.util.system.isInstalledFromFDroid
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MoreController :
    FullComposeController<MorePresenter>(),
    RootController {

    private val libraryPreferences: LibraryPreferences = Injekt.get()

    override fun createPresenter() = MorePresenter()

    @Composable
    override fun ComposeContent() {
        MoreScreen(
            presenter = presenter,
            onClickHistory = {
                val targetController = when (libraryPreferences.bottomNavStyle().get()) {
                    1 -> UpdatesTabsController()
                    2 -> LibraryController()
                    else -> HistoryTabsController()
                }
                router.pushController(targetController)
            },
            onClickAnimeDownloadQueue = { router.pushController(AnimeDownloadController()) },
            isFDroid = activity?.isInstalledFromFDroid() ?: false,
            onClickDownloadQueue = { router.pushController(DownloadController()) },
            onClickAnimeCategories = { router.pushController(AnimeCategoryController()) },
            onClickCategories = { router.pushController(CategoryController()) },
            onClickBackupAndRestore = { router.pushController(SettingsMainController.toBackupScreen()) },
            onClickSettings = { router.pushController(SettingsMainController()) },
            onClickAbout = { router.pushController(SettingsMainController.toAboutScreen()) },
        )
    }

    companion object {
        const val URL_HELP = "https://aniyomi.org/help/"
    }
}
