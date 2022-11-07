package eu.kanade.tachiyomi.ui.recent

import android.os.Bundle
import androidx.compose.runtime.getValue
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.recent.animehistory.AnimeHistoryPresenter
import eu.kanade.tachiyomi.ui.recent.history.HistoryPresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HistoryTabsPresenter(
    preferences: BasePreferences = Injekt.get(),
) : BasePresenter<HistoryTabsController>() {

    val isDownloadOnly: Boolean by preferences.downloadedOnly().asState()
    val isIncognitoMode: Boolean by preferences.incognitoMode().asState()

    val animeHistoryPresenter = AnimeHistoryPresenter(presenterScope)
    val historyPresenter = HistoryPresenter(presenterScope)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        animeHistoryPresenter.onCreate(view?.activity, view?.activity)
        historyPresenter.onCreate(view?.activity)
    }

    fun resumeLastChapterRead() {
        historyPresenter.resumeLastChapterRead()
    }

    fun resumeLastEpisodeSeen() {
        animeHistoryPresenter.resumeLastEpisodeSeen()
    }
}
