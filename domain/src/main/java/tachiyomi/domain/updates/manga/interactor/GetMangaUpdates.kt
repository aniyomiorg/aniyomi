package tachiyomi.domain.updates.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import tachiyomi.domain.updates.manga.repository.MangaUpdatesRepository
import java.time.Instant

class GetMangaUpdates(
    private val repository: MangaUpdatesRepository,
) {

    suspend fun await(read: Boolean, after: Long): List<MangaUpdatesWithRelations> {
        return repository.awaitWithRead(read, after, limit = 500)
    }

    fun subscribe(instant: Instant): Flow<List<MangaUpdatesWithRelations>> {
        return repository.subscribeAllMangaUpdates(instant.toEpochMilli(), limit = 500)
    }

    fun subscribe(read: Boolean, after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return repository.subscribeWithRead(read, after, limit = 500)
    }
}
