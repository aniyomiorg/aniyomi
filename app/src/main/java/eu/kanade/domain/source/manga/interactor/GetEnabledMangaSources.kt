package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import exh.source.BlacklistedSources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.domain.source.manga.model.Pin
import tachiyomi.domain.source.manga.model.Pins
import tachiyomi.domain.source.manga.model.Source
import tachiyomi.domain.source.manga.repository.MangaSourceRepository
import tachiyomi.source.local.entries.manga.LocalMangaSource

class GetEnabledMangaSources(
    private val repository: MangaSourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Source>> {
        return combine(
            preferences.pinnedMangaSources().changes(),
            combine(
                preferences.enabledLanguages().changes(),
                preferences.disabledMangaSources().changes(),
                preferences.lastUsedMangaSource().changes(),
            ) { a, b, c -> Triple(a, b, c) },
            // SY -->
            combine(
                preferences.dataSaverExcludedSources().changes(),
                preferences.sourcesTabSourcesInCategories().changes(),
                preferences.sourcesTabCategoriesFilter().changes(),
            ) { a, b, c -> Triple(a, b, c) },
            // SY <--
            repository.getMangaSources(),
        ) { pinnedMangaSourceIds, (enabledLanguages, disabledSources, lastUsedSource), (excludedFromDataSaver, sourcesInCategories, sourceCategoriesFilter), sources ->
            val sourcesAndCategories = sourcesInCategories.map {
                it.split('|').let { (source, test) -> source.toLong() to test }
            }
            val sourcesInSourceCategories = sourcesAndCategories.map { it.first }
            sources
                .filter { it.lang in enabledLanguages || it.id == LocalMangaSource.ID }
                .filterNot { it.id.toString() in disabledSources || it.id in BlacklistedSources.HIDDEN_SOURCES }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .flatMap {
                    val flag = if ("${it.id}" in pinnedMangaSourceIds) Pins.pinned else Pins.unpinned
                    // SY -->
                    val categories = sourcesAndCategories.filter { (id) -> id == it.id }
                        .map(Pair<*, String>::second)
                        .toSet()
                    // SY <--
                    val source = it.copy(
                        pin = flag,
                        /*isExcludedFromDataSaver = it.id.toString() in excludedFromDataSaver,*/
                        /*categories = categories,*/
                    )
                    val toFlatten = mutableListOf(source)
                    if (source.id == lastUsedSource) {
                        toFlatten.add(source.copy(isUsedLast = true, pin = source.pin - Pin.Actual))
                    }
                    // SY -->
                    categories.forEach { category ->
                        toFlatten.add(source.copy(pin = source.pin - Pin.Actual))
                    }
                    if (sourceCategoriesFilter && Pin.Actual !in toFlatten[0].pin && source.id in sourcesInSourceCategories) {
                        toFlatten.removeAt(0)
                    }
                    // SY <--
                    toFlatten
                }
        }
            .distinctUntilChanged()
    }
}
