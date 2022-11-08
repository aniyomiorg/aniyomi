package eu.kanade.domain.animeextension.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animeextension.AnimeExtensionManager
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetAnimeExtensionLanguages(
    private val preferences: SourcePreferences,
    private val extensionManager: AnimeExtensionManager,
) {
    fun subscribe(): Flow<List<String>> {
        return combine(
            preferences.enabledLanguages().changes(),
            extensionManager.availableExtensionsFlow,
        ) { enabledLanguage, availableExtensions ->
            availableExtensions
                .flatMap { ext ->
                    if (ext.sources.isEmpty()) {
                        listOf(ext.lang)
                    } else {
                        ext.sources.map { it.lang }
                    }
                }
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
