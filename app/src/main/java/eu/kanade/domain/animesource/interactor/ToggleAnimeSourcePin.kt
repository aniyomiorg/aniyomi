package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.preference.minusAssign
import eu.kanade.tachiyomi.util.preference.plusAssign

class ToggleAnimeSourcePin(
    private val preferences: PreferencesHelper,
) {

    fun await(source: AnimeSource) {
        val isPinned = source.id.toString() in preferences.pinnedAnimeSources().get()
        if (isPinned) {
            preferences.pinnedAnimeSources() -= source.id.toString()
        } else {
            preferences.pinnedAnimeSources() += source.id.toString()
        }
    }
}
