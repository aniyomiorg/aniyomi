package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.preference.minusAssign
import eu.kanade.tachiyomi.util.preference.plusAssign

class ToggleAnimeSource(
    private val preferences: PreferencesHelper,
) {

    fun await(source: AnimeSource, enable: Boolean = source.id.toString() in preferences.disabledAnimeSources().get()) {
        await(source.id, enable)
    }

    fun await(sourceId: Long, enable: Boolean = sourceId.toString() in preferences.disabledAnimeSources().get()) {
        if (enable) {
            preferences.disabledAnimeSources() -= sourceId.toString()
        } else {
            preferences.disabledAnimeSources() += sourceId.toString()
        }
    }
}
