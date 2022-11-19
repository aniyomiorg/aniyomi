package eu.kanade.tachiyomi.ui

import androidx.compose.runtime.getValue
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.ui.animehistory.AnimeHistoryPresenter
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.history.HistoryPresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HistoryTabsPresenter(
    preferences: BasePreferences = Injekt.get(),
) : BasePresenter<HistoryTabsController>() {

    val animeHistoryPresenter = AnimeHistoryPresenter(presenterScope, view)
    val historyPresenter = HistoryPresenter(presenterScope, view)

    val isDownloadOnly: Boolean by preferences.downloadedOnly().asState()
    val isIncognitoMode: Boolean by preferences.incognitoMode().asState()

    fun resumeLastChapterRead() {
        historyPresenter.resumeLastChapterRead()
    }

    fun resumeLastEpisodeSeen() {
        animeHistoryPresenter.resumeLastEpisodeSeen()
    }
}
