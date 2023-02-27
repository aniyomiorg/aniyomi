package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.manga.model.Source
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.preference.getAndSet

class ToggleMangaSourcePin(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source) {
        val isPinned = source.id.toString() in preferences.pinnedMangaSources().get()
        preferences.pinnedMangaSources().getAndSet { pinned ->
            if (isPinned) pinned.minus("${source.id}") else pinned.plus("${source.id}")
        }
    }
}
