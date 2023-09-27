package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.preference.getAndSet
import tachiyomi.domain.source.manga.model.Source

class ToggleExcludeFromMangaDataSaver(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source) {
        preferences.dataSaverExcludedSources().getAndSet {
            if (source.id.toString() in it) {
                it - source.id.toString()
            } else {
                it + source.id.toString()
            }
        }
    }
}
