package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.anime.model.AnimeSource
import eu.kanade.domain.source.anime.model.Pin
import eu.kanade.domain.source.anime.model.Pins
import eu.kanade.domain.source.anime.repository.AnimeSourceRepository
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.anime.LocalAnimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetEnabledAnimeSources(
    private val repository: AnimeSourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<AnimeSource>> {
        return combine(
            preferences.pinnedAnimeSources().changes(),
            preferences.enabledLanguages().changes(),
            preferences.disabledAnimeSources().changes(),
            preferences.lastUsedAnimeSource().changes(),
            repository.getAnimeSources(),
        ) { pinnedSourceIds, enabledLanguages, disabledSources, lastUsedSource, sources ->
            sources
                .filter { it.lang in enabledLanguages || it.id == LocalAnimeSource.ID }
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
