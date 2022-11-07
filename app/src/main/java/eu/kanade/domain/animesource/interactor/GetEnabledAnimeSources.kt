package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.Pin
import eu.kanade.domain.animesource.model.Pins
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
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
            repository.getSources(),
        ) { pinnedSourceIds, enabledLanguages, disabledSources, lastUsedSource, sources ->
            val duplicatePins = preferences.duplicatePinnedSources().get()
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
                    if (duplicatePins && Pin.Pinned in source.pin) {
                        toFlatten[0] = toFlatten[0].copy(pin = source.pin + Pin.Forced)
                        toFlatten.add(source.copy(pin = source.pin - Pin.Actual))
                    }
                    toFlatten
                }
        }
            .distinctUntilChanged()
    }
}
