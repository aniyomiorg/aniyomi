package eu.kanade.tachiyomi.ui

import android.Manifest
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.presentation.components.PagerState
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animeupdates.animeUpdatesTab
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.download.anime.AnimeDownloadController
import eu.kanade.tachiyomi.ui.download.manga.DownloadController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.updates.updatesTab
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdatesTabsController : FullComposeController<UpdatesTabsPresenter>(), RootController {

    override fun createPresenter() = UpdatesTabsPresenter()

    private val state = PagerState(currentPage = TAB_ANIME)

    @Composable
    override fun ComposeContent() {
        val libraryPreferences: LibraryPreferences = Injekt.get()
        val fromMore = libraryPreferences.bottomNavStyle().get() == 1
        TabbedScreen(
            titleRes = R.string.label_recent_updates,
            tabs = listOf(
                animeUpdatesTab(router, presenter.animeUpdatesPresenter, activity, fromMore),
                updatesTab(router, presenter.updatesPresenter, activity, fromMore),
            ),
            incognitoMode = presenter.isIncognitoMode,
            downloadedOnlyMode = presenter.isDownloadOnly,
            state = state,
        )

        LaunchedEffect(Unit) {
            (activity as? MainActivity)?.ready = true
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        requestPermissionsSafe(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 301)
    }

    fun openDownloadQueue() {
        if (state.currentPage == TAB_MANGA) {
            router.pushController(DownloadController())
        } else {
            router.pushController(AnimeDownloadController())
        }
    }
}

private const val TAB_ANIME = 0
private const val TAB_MANGA = 1
