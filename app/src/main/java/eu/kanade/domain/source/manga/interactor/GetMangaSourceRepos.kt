package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetMangaSourceRepos(private val preferences: SourcePreferences) {

    fun subscribe(): Flow<List<String>> {
        return preferences.mangaExtensionRepos().changes()
            .map { it.sortedWith(String.CASE_INSENSITIVE_ORDER) }
    }
}
