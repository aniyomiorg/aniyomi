package eu.kanade.domain.extension.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.minusAssign

class DeleteMangaExtensionRepo(private val preferences: SourcePreferences) {

    fun await(repo: String) {
        preferences.mangaExtensionRepos() -= repo
    }
}
