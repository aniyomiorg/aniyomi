package eu.kanade.domain.animeextension.interactor

import eu.kanade.core.util.asFlow
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetAnimeExtensionLanguages(
    private val preferences: PreferencesHelper,
    private val extensionManager: AnimeExtensionManager,
) {
    fun subscribe(): Flow<List<String>> {
        return combine(
            preferences.enabledLanguages().asFlow(),
            extensionManager.getAvailableExtensionsObservable().asFlow(),
        ) { enabledLanguage, availableExtensions ->
            availableExtensions
                .map { it.lang }
                .distinct()
                .sortedWith(
                    compareBy(
                        { it !in enabledLanguage },
                        { LocaleHelper.getDisplayName(it) },
                    ),
                )
        }
    }
}
