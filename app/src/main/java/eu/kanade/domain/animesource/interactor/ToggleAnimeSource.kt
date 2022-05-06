package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.preference.minusAssign
import eu.kanade.tachiyomi.util.preference.plusAssign

class ToggleAnimeSource(
    private val preferences: PreferencesHelper
) {

    fun await(source: AnimeSource) {
        val isEnabled = source.id.toString() !in preferences.disabledAnimeSources().get()
        if (isEnabled) {
            preferences.disabledAnimeSources() += source.id.toString()
        } else {
            preferences.disabledAnimeSources() -= source.id.toString()
        }
    }
}
