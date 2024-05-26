package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.core.util.lang.compareToWithCollator
import tachiyomi.domain.source.manga.model.Source
import tachiyomi.domain.source.manga.repository.MangaSourceRepository
import tachiyomi.source.local.entries.manga.LocalMangaSource
import java.util.Collections

class GetMangaSourcesWithFavoriteCount(
    private val repository: MangaSourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Pair<Source, Long>>> {
        return combine(
            preferences.migrationSortingDirection().changes(),
            preferences.migrationSortingMode().changes(),
            repository.getMangaSourcesWithFavoriteCount(),
        ) { direction, mode, list ->
            list
                .filterNot { it.first.id == LocalMangaSource.ID }
                .sortedWith(sortFn(direction, mode))
        }
    }

    private fun sortFn(
        direction: SetMigrateSorting.Direction,
        sorting: SetMigrateSorting.Mode,
    ): java.util.Comparator<Pair<Source, Long>> {
        val sortFn: (Pair<Source, Long>, Pair<Source, Long>) -> Int = { a, b ->
            when (sorting) {
                SetMigrateSorting.Mode.ALPHABETICAL -> {
                    when {
                        a.first.isStub && !b.first.isStub -> -1
                        b.first.isStub && !a.first.isStub -> 1
                        else -> a.first.name.lowercase().compareToWithCollator(b.first.name.lowercase())
                    }
                }
                SetMigrateSorting.Mode.TOTAL -> {
                    when {
                        a.first.isStub && !b.first.isStub -> -1
                        b.first.isStub && !a.first.isStub -> 1
                        else -> a.second.compareTo(b.second)
                    }
                }
            }
        }

        return when (direction) {
            SetMigrateSorting.Direction.ASCENDING -> Comparator(sortFn)
            SetMigrateSorting.Direction.DESCENDING -> Collections.reverseOrder(sortFn)
        }
    }
}
