package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.service.SetMigrateSorting
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository
import tachiyomi.source.local.entries.anime.LocalAnimeSource
import java.text.Collator
import java.util.Collections
import java.util.Locale

class GetAnimeSourcesWithFavoriteCount(
    private val repository: AnimeSourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Pair<AnimeSource, Long>>> {
        return combine(
            preferences.migrationSortingDirection().changes(),
            preferences.migrationSortingMode().changes(),
            repository.getAnimeSourcesWithFavoriteCount(),
        ) { direction, mode, list ->
            list
                .filterNot { it.first.id == LocalAnimeSource.ID }
                .sortedWith(sortFn(direction, mode))
        }
    }

    private fun sortFn(
        direction: SetMigrateSorting.Direction,
        sorting: SetMigrateSorting.Mode,
    ): java.util.Comparator<Pair<AnimeSource, Long>> {
        val locale = Locale.getDefault()
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
        val sortFn: (Pair<AnimeSource, Long>, Pair<AnimeSource, Long>) -> Int = { a, b ->
            when (sorting) {
                SetMigrateSorting.Mode.ALPHABETICAL -> {
                    when {
                        a.first.isStub && b.first.isStub.not() -> -1
                        b.first.isStub && a.first.isStub.not() -> 1
                        else -> collator.compare(a.first.name.lowercase(locale), b.first.name.lowercase(locale))
                    }
                }
                SetMigrateSorting.Mode.TOTAL -> {
                    when {
                        a.first.isStub && b.first.isStub.not() -> -1
                        b.first.isStub && a.first.isStub.not() -> 1
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
