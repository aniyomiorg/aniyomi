package eu.kanade.tachiyomi.ui

import android.os.Bundle
import androidx.compose.runtime.getValue
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.ui.animeupdates.AnimeUpdatesPresenter
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.updates.UpdatesPresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdatesTabsPresenter(
    preferences: BasePreferences = Injekt.get(),
) : BasePresenter<UpdatesTabsController>() {

    val animeUpdatesPresenter = AnimeUpdatesPresenter(presenterScope, view)
    val updatesPresenter = UpdatesPresenter(presenterScope, view)

    val isDownloadOnly: Boolean by preferences.downloadedOnly().asState()
    val isIncognitoMode: Boolean by preferences.incognitoMode().asState()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        animeUpdatesPresenter.onCreate()
        updatesPresenter.onCreate()
    }
}
