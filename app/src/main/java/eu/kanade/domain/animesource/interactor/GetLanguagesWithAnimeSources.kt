package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetLanguagesWithAnimeSources(
    private val repository: AnimeSourceRepository,
    private val preferences: PreferencesHelper,
) {

    fun subscribe(): Flow<Map<String, List<AnimeSource>>> {
        return combine(
            preferences.enabledLanguages().asFlow(),
            preferences.disabledSources().asFlow(),
            repository.getOnlineSources(),
        ) { enabledLanguage, disabledSource, onlineSources ->
            val sortedSources = onlineSources.sortedWith(
                compareBy<AnimeSource> { it.id.toString() in disabledSource }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )

            sortedSources.groupBy { it.lang }
                .toSortedMap(
                    compareBy(
                        { it !in enabledLanguage },
                        { LocaleHelper.getDisplayName(it) },
                    ),
                )
        }
    }
}
