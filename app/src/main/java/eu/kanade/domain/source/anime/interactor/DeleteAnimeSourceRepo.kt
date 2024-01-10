package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.preference.minusAssign

class DeleteAnimeSourceRepo(private val preferences: SourcePreferences) {

    fun await(repo: String) {
        preferences.animeExtensionRepos() -= repo
    }
}
