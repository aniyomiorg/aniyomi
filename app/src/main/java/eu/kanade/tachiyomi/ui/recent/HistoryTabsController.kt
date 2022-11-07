package eu.kanade.tachiyomi.ui.recent

import android.Manifest
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.recent.animehistory.animeHistoryTab
import eu.kanade.tachiyomi.ui.recent.history.historyTab

class HistoryTabsController : FullComposeController<HistoryTabsPresenter>(), RootController {

    override fun createPresenter() = HistoryTabsPresenter()

    @Composable
    override fun ComposeContent() {
        TabbedScreen(
            titleRes = R.string.browse,
            tabs = listOf(
                animeHistoryTab(router, presenter.animeHistoryPresenter),
                historyTab(router, presenter.historyPresenter),
            ),
            incognitoMode = presenter.isIncognitoMode,
            downloadedOnlyMode = presenter.isDownloadOnly,
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
        if (isCurrentHistoryTabManga) {
            presenter.resumeLastChapterRead()
        } else {
            presenter.resumeLastEpisodeSeen()
        }
    }

    companion object {
        var isCurrentHistoryTabManga = true
    }
}
