package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.manga.model.Pin
import eu.kanade.domain.source.manga.model.Pins
import eu.kanade.domain.source.manga.model.Source
import eu.kanade.domain.source.manga.repository.MangaSourceRepository
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.manga.LocalMangaSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetEnabledMangaSources(
    private val repository: MangaSourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Source>> {
        return combine(
            preferences.pinnedMangaSources().changes(),
            preferences.enabledLanguages().changes(),
            preferences.disabledMangaSources().changes(),
            preferences.lastUsedMangaSource().changes(),
            repository.getMangaSources(),
        ) { pinnedSourceIds, enabledLanguages, disabledSources, lastUsedSource, sources ->
            sources
                .filter { it.lang in enabledLanguages || it.id == LocalMangaSource.ID }
                .filterNot { it.id.toString() in disabledSources }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .flatMap {
                    val flag = if ("${it.id}" in pinnedSourceIds) Pins.pinned else Pins.unpinned
                    val source = it.copy(pin = flag)
                    val toFlatten = mutableListOf(source)
                    if (source.id == lastUsedSource) {
                        toFlatten.add(source.copy(isUsedLast = true, pin = source.pin - Pin.Actual))
                    }
                    toFlatten
                }
        }
            .distinctUntilChanged()
    }
}
