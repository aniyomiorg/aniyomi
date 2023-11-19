package tachiyomi.domain.updates.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import tachiyomi.domain.updates.manga.repository.MangaUpdatesRepository
import java.util.Calendar

class GetMangaUpdates(
    private val repository: MangaUpdatesRepository,
) {

    suspend fun await(read: Boolean, after: Long): List<MangaUpdatesWithRelations> {
        return repository.awaitWithRead(read, after, limit = 500)
    }

    fun subscribe(calendar: Calendar): Flow<List<MangaUpdatesWithRelations>> {
        return repository.subscribeAllMangaUpdates(calendar.time.time, limit = 500)
    }

    fun subscribe(read: Boolean, after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return repository.subscribeWithRead(read, after, limit = 500)
    }
}
