package eu.kanade.domain.extension.anime.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.minusAssign

class DeleteAnimeExtensionRepo(private val preferences: SourcePreferences) {

    fun await(repo: String) {
        preferences.animeExtensionRepos() -= repo
    }
}
