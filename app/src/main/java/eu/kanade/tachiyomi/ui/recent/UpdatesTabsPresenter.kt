package eu.kanade.tachiyomi.ui.recent

import android.os.Bundle
import androidx.compose.runtime.getValue
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesPresenter
import eu.kanade.tachiyomi.ui.recent.updates.UpdatesPresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdatesTabsPresenter(
    preferences: BasePreferences = Injekt.get(),
) : BasePresenter<UpdatesTabsController>() {

    val isDownloadOnly: Boolean by preferences.downloadedOnly().asState()
    val isIncognitoMode: Boolean by preferences.incognitoMode().asState()

    val animeUpdatesPresenter = AnimeUpdatesPresenter(presenterScope)
    val updatesPresenter = UpdatesPresenter(presenterScope)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        animeUpdatesPresenter.onCreate(view?.activity)
        updatesPresenter.onCreate(view?.activity)
    }
}
