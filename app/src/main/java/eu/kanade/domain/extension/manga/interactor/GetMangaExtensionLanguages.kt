package eu.kanade.domain.extension.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMangaExtensionLanguages(
    private val preferences: SourcePreferences,
    private val extensionManager: MangaExtensionManager,
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
                    compareBy<String> { it !in enabledLanguage }.then(LocaleHelper.comparator),
                )
        }
    }
}
