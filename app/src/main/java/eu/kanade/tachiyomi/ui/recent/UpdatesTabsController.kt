package eu.kanade.tachiyomi.ui.recent

import android.Manifest
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import eu.kanade.presentation.components.PagerState
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.download.anime.AnimeDownloadController
import eu.kanade.tachiyomi.ui.download.manga.DownloadController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.recent.animeupdates.animeUpdatesTab
import eu.kanade.tachiyomi.ui.recent.updates.updatesTab

class UpdatesTabsController : FullComposeController<UpdatesTabsPresenter>(), RootController {

    override fun createPresenter() = UpdatesTabsPresenter()

    private val state = PagerState(currentPage = TAB_ANIME)

    @Composable
    override fun ComposeContent() {
        TabbedScreen(
            titleRes = null,
            tabs = listOf(
                animeUpdatesTab(router, presenter.animeUpdatesPresenter, activity),
                updatesTab(router, presenter.updatesPresenter, activity),
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
