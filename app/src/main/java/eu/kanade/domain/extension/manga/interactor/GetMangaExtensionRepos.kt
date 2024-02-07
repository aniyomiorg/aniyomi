package eu.kanade.domain.extension.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow

class GetMangaExtensionRepos(private val preferences: SourcePreferences) {

    fun subscribe(): Flow<Set<String>> {
        return preferences.mangaExtensionRepos().changes()
    }
}
