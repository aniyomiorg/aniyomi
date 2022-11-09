package eu.kanade.tachiyomi.ui

import eu.kanade.tachiyomi.ui.animehistory.AnimeHistoryPresenter
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.history.HistoryPresenter

class HistoryTabsPresenter : BasePresenter<HistoryTabsController>() {

    val animeHistoryPresenter = AnimeHistoryPresenter(presenterScope, view)
    val historyPresenter = HistoryPresenter(presenterScope, view)

    fun resumeLastChapterRead() {
        historyPresenter.resumeLastChapterRead()
    }

    fun resumeLastEpisodeSeen() {
        animeHistoryPresenter.resumeLastEpisodeSeen()
    }
}
