package eu.kanade.tachiyomi.ui

import android.Manifest
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.presentation.components.PagerState
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animehistory.animeHistoryTab
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.history.historyTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HistoryTabsController : FullComposeController<HistoryTabsPresenter>(), RootController {

    override fun createPresenter() = HistoryTabsPresenter()

    private val state = PagerState(currentPage = TAB_ANIME)

    @Composable
    override fun ComposeContent() {
        val libraryPreferences: LibraryPreferences = Injekt.get()
        val fromMore = libraryPreferences.bottomNavStyle().get() == 0
        TabbedScreen(
            titleRes = R.string.label_recent_manga,
            tabs = listOf(
                animeHistoryTab(router, presenter.animeHistoryPresenter, fromMore),
                historyTab(router, presenter.historyPresenter, fromMore),
            ),
            incognitoMode = presenter.isIncognitoMode,
            downloadedOnlyMode = presenter.isDownloadOnly,
            state = state,
            searchQuery = presenter.historyPresenter.searchQuery,
            onChangeSearchQuery = { presenter.historyPresenter.searchQuery = it },
            searchQueryAnime = presenter.animeHistoryPresenter.searchQuery,
            onChangeSearchQueryAnime = { presenter.animeHistoryPresenter.searchQuery = it },
        )

        LaunchedEffect(Unit) {
            (activity as? MainActivity)?.ready = true
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        requestPermissionsSafe(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 301)
    }

    fun resumeLastItem() {
        if (state.currentPage == TAB_MANGA) {
            presenter.resumeLastChapterRead()
        } else {
            presenter.resumeLastEpisodeSeen()
        }
    }
}

private const val TAB_ANIME = 0
private const val TAB_MANGA = 1
