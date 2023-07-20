package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository

class GetLanguagesWithAnimeSources(
    private val repository: AnimeSourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<Map<String, List<AnimeSource>>> {
        return combine(
            preferences.enabledLanguages().changes(),
            preferences.disabledAnimeSources().changes(),
            repository.getOnlineAnimeSources(),
        ) { enabledLanguage, disabledSource, onlineSources ->
            val sortedSources = onlineSources.sortedWith(
                compareBy<AnimeSource> { it.id.toString() in disabledSource }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )

            sortedSources.groupBy { it.lang }
                .toSortedMap(
                    compareBy<String> { it !in enabledLanguage }.then(LocaleHelper.comparator),
                )
        }
    }
}
