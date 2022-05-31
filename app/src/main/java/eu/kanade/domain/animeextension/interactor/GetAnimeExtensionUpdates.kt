package eu.kanade.domain.animeextension.interactor

import eu.kanade.core.util.asFlow
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.model.AnimeExtension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAnimeExtensionUpdates(
    private val preferences: PreferencesHelper,
    private val extensionManager: AnimeExtensionManager,
) {

    fun subscribe(): Flow<List<AnimeExtension.Installed>> {
        val showNsfwSources = preferences.showNsfwSource().get()

        return extensionManager.getInstalledExtensionsObservable().asFlow()
            .map { installed ->
                installed
                    .filter { it.hasUpdate && (showNsfwSources || it.isNsfw.not()) }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
    }
}
