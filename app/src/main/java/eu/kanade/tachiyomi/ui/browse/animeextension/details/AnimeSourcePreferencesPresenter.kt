package eu.kanade.tachiyomi.ui.browse.animeextension.details

import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeSourcePreferencesPresenter(
    val sourceId: Long,
    sourceManager: AnimeSourceManager = Injekt.get(),
) : BasePresenter<AnimeSourcePreferencesController>() {

    val source = sourceManager.get(sourceId)
}
