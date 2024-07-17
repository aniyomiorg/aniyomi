package mihon.domain.upcoming.manga.interactor

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.entries.manga.repository.MangaRepository

class GetUpcomingManga(
    private val mangaRepository: MangaRepository,
) {

    private val includedStatuses = setOf(
        SManga.ONGOING.toLong(),
        SManga.PUBLISHING_FINISHED.toLong(),
    )

    suspend fun subscribe(): Flow<List<Manga>> {
        return mangaRepository.getUpcomingManga(includedStatuses)
    }
}
