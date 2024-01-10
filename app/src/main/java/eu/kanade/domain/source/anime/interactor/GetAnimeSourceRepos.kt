package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAnimeSourceRepos(private val preferences: SourcePreferences) {

    fun subscribe(): Flow<List<String>> {
        return preferences.animeExtensionRepos().changes()
            .map { it.sortedWith(String.CASE_INSENSITIVE_ORDER) }
    }
}
