package eu.kanade.tachiyomi.ui

import android.os.Bundle
import eu.kanade.tachiyomi.ui.animeupdates.AnimeUpdatesPresenter
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.updates.UpdatesPresenter

class UpdatesTabsPresenter : BasePresenter<UpdatesTabsController>() {

    val animeUpdatesPresenter = AnimeUpdatesPresenter(presenterScope, view)
    val updatesPresenter = UpdatesPresenter(presenterScope, view)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        animeUpdatesPresenter.onCreate()
        updatesPresenter.onCreate()
    }
}
