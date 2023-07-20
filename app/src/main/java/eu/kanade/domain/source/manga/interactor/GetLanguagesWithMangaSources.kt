package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.manga.model.Source
import tachiyomi.domain.source.manga.repository.MangaSourceRepository

class GetLanguagesWithMangaSources(
    private val repository: MangaSourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<Map<String, List<Source>>> {
        return combine(
            preferences.enabledLanguages().changes(),
            preferences.disabledMangaSources().changes(),
            repository.getOnlineMangaSources(),
        ) { enabledLanguage, disabledSource, onlineSources ->
            val sortedSources = onlineSources.sortedWith(
                compareBy<Source> { it.id.toString() in disabledSource }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )

            sortedSources.groupBy { it.lang }
                .toSortedMap(
                    compareBy<String> { it !in enabledLanguage }.then(LocaleHelper.comparator),
                )
        }
    }
}
