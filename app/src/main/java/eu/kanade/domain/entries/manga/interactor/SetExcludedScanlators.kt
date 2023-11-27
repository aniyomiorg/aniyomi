package eu.kanade.domain.entries.manga.interactor

import tachiyomi.data.handlers.manga.MangaDatabaseHandler

class SetExcludedScanlators(
    private val handler: MangaDatabaseHandler,
) {

    suspend fun await(mangaId: Long, excludedScanlators: Set<String>) {
        handler.await(inTransaction = true) {
            val currentExcluded = handler.awaitList {
                excluded_scanlatorsQueries.getExcludedScanlatorsByMangaId(mangaId)
            }.toSet()
            val toAdd = excludedScanlators.minus(currentExcluded)
            for (scanlator in toAdd) {
                excluded_scanlatorsQueries.insert(mangaId, scanlator)
            }
            val toRemove = currentExcluded.minus(excludedScanlators)
            excluded_scanlatorsQueries.remove(mangaId, toRemove)
        }
    }
}
