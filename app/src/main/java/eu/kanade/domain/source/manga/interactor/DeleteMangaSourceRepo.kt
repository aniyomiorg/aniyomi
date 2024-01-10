package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.preference.minusAssign

class DeleteMangaSourceRepo(private val preferences: SourcePreferences) {

    fun await(repo: String) {
        preferences.mangaExtensionRepos() -= repo
    }
}
