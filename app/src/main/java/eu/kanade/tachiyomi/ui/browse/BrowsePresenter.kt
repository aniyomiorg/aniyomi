package eu.kanade.tachiyomi.ui.browse

import android.os.Bundle
import androidx.compose.runtime.getValue
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.browse.animeextension.AnimeExtensionsPresenter
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourcesPresenter
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsPresenter
import eu.kanade.tachiyomi.ui.browse.migration.animesources.MigrationAnimeSourcesPresenter
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrationSourcesPresenter
import eu.kanade.tachiyomi.ui.browse.source.SourcesPresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowsePresenter(
    preferences: BasePreferences = Injekt.get(),
) : BasePresenter<BrowseController>() {

    val isDownloadOnly: Boolean by preferences.downloadedOnly().asState()
    val isIncognitoMode: Boolean by preferences.incognitoMode().asState()

    val sourcesPresenter = SourcesPresenter(presenterScope)
    val animeSourcesPresenter = AnimeSourcesPresenter(presenterScope)
    val extensionsPresenter = ExtensionsPresenter(presenterScope)
    val animeExtensionsPresenter = AnimeExtensionsPresenter(presenterScope)
    val migrationSourcesPresenter = MigrationSourcesPresenter(presenterScope)
    val migrationAnimeSourcesPresenter = MigrationAnimeSourcesPresenter(presenterScope)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        animeSourcesPresenter.onCreate()
        sourcesPresenter.onCreate()
        animeExtensionsPresenter.onCreate()
        extensionsPresenter.onCreate()
        migrationAnimeSourcesPresenter.onCreate()
        migrationSourcesPresenter.onCreate()
    }
}
