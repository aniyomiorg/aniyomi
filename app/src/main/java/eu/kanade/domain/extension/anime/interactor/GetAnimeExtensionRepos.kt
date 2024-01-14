package eu.kanade.domain.extension.anime.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow

class GetAnimeExtensionRepos(private val preferences: SourcePreferences) {

    fun subscribe(): Flow<Set<String>> {
        return preferences.animeExtensionRepos().changes()
    }
}
