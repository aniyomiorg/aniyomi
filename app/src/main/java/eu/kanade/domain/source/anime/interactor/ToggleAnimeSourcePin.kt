package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.preference.getAndSet
import tachiyomi.domain.source.anime.model.AnimeSource

class ToggleAnimeSourcePin(
    private val preferences: SourcePreferences,
) {

    fun await(source: AnimeSource) {
        val isPinned = source.id.toString() in preferences.pinnedAnimeSources().get()
        preferences.pinnedAnimeSources().getAndSet { pinned ->
            if (isPinned) pinned.minus("${source.id}") else pinned.plus("${source.id}")
        }
    }
}
